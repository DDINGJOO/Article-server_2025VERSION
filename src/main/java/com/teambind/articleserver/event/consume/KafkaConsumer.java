package com.teambind.articleserver.event.consume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.event.events.ImageChangeEvent;
import com.teambind.articleserver.event.events.ImagesChangeEventWrapper;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.ArticleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * 이미지 서버로부터 전달되는 이미지 변경 이벤트를 처리하는 Kafka Consumer
 *
 * <p>처리 방식:
 *
 * <ul>
 *   <li>빈 배열: 게시글의 모든 이미지 삭제
 *   <li>이미지 포함: 기존 이미지 전체 삭제 후 새 이미지로 교체 (전체 교체 방식)
 *   <li>순서: sequence 기준 오름차순 정렬 보장
 * </ul>
 *
 * <p>멱등성 보장: 동일한 이벤트를 여러 번 처리해도 결과가 동일함
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class KafkaConsumer {
  private final ObjectMapper objectMapper;
  private final ArticleRepository articleRepository;

  /**
   * 이미지 변경 이벤트 처리
   *
   * @param message Kafka 메시지 (ImagesChangeEventWrapper JSON)
   */
  @KafkaListener(topics = "article-image-changed", groupId = "article-consumer-group")
  public void articleImageChanger(String message) {
    try {
      // 1. 이벤트 역직렬화
      ImagesChangeEventWrapper wrapper =
          objectMapper.readValue(message, ImagesChangeEventWrapper.class);

      // 2. Null Safety: Wrapper 검증
      if (wrapper == null || wrapper.getReferenceId() == null) {
        log.warn("Received invalid image change event: wrapper or referenceId is null");
        return;
      }

      String articleId = wrapper.getReferenceId();
      List<ImageChangeEvent> images = wrapper.getImages();

      // 3. Null Safety: 이미지 리스트 검증
      if (images == null) {
        log.warn("Received null images list for articleId: {}", articleId);
        return;
      }

      // 4. 게시글 조회
      Article article =
          articleRepository
              .findById(articleId)
              .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));

      // 5. 기존 이미지 전체 삭제 (전체 교체 방식)
      article.removeImages();

      // 6. 빈 배열 처리: 전체 삭제
      if (images.isEmpty()) {
        log.info("Deleting all images for articleId: {}", articleId);
        articleRepository.save(article);
        return;
      }

      // 7. 이미지 추가: sequence 순서대로 정렬 후 추가
      images.sort(
          (o1, o2) -> {
            if (o1.getSequence() == null || o2.getSequence() == null) {
              log.warn("Null sequence found in image change event for articleId: {}", articleId);
              return 0;
            }
            return o1.getSequence() - o2.getSequence();
          });

      // 8. 첫 번째 이미지를 firstImageUrl로 설정
      boolean isFirstImage = true;

      for (ImageChangeEvent imageEvent : images) {
        // Null Safety: 이미지 ID와 URL 검증
        if (imageEvent.getImageId() == null || imageEvent.getImageId().isEmpty()) {
          log.warn("Skipping image with null or empty imageId for articleId: {}", articleId);
          continue;
        }

        if (imageEvent.getImageUrl() == null || imageEvent.getImageUrl().isEmpty()) {
          log.warn("Skipping image with null or empty imageUrl for articleId: {}", articleId);
          continue;
        }

        // 이미지 추가
        article.addImage(imageEvent.getImageId(), imageEvent.getImageUrl());

        // 첫 번째 유효한 이미지를 firstImageUrl로 명시적 설정
        if (isFirstImage) {
          article.setFirstImageUrl(imageEvent.getImageUrl());
          isFirstImage = false;
        }
      }

      // 9. 변경 사항 저장
      articleRepository.save(article);

      log.info("Successfully updated {} images for articleId: {}", images.size(), articleId);

    } catch (JsonProcessingException e) {
      // JSON 파싱 실패 시 로그만 기록하고 계속 진행 (Consumer 중단 방지)
      log.error("Failed to deserialize article-image-changed message: {}", message, e);
      // RuntimeException을 던지지 않음 - 가이드 권장사항

    } catch (CustomException e) {
      // 게시글을 찾을 수 없는 경우
      log.error("Article not found while processing image change event: {}", e.getMessage());
      // 계속 진행 (이미 삭제된 게시글일 수 있음)

    } catch (Exception e) {
      // 기타 예상치 못한 에러
      log.error("Unexpected error processing article-image-changed message: {}", message, e);
      // 계속 진행
    }
  }
}

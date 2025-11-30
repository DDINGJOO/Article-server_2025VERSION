package com.teambind.articleserver.adapter.in.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.common.exception.CustomException;
import com.teambind.articleserver.common.exception.ErrorCode;
import com.teambind.articleserver.event.events.ImageChangeEvent;
import com.teambind.articleserver.event.events.ImagesChangeEventWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
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
   * <p>imageId와 imageUrl을 쌍으로 관리하여 이미지 정보의 무결성을 보장합니다.
   *
   * @param message Kafka 메시지 (ImagesChangeEventWrapper JSON)
   * @param topic 수신한 토픽 이름
   * @param partition 파티션 번호
   * @param offset 오프셋
   */
  @KafkaListener(topics = "post-image-changed", groupId = "article-consumer-group")
  public void articleImageChanger(
      @Payload String message,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {

    // 이벤트 수신 로그 - 토픽과 페이로드 정보 기록
    log.info("========== Kafka Event Received ==========");
    log.info("Topic: {}", topic);
    log.info("Partition: {}, Offset: {}", partition, offset);
    log.info("Payload: {}", message);
    log.info("==========================================");

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
        // imageId와 imageUrl 쌍 검증 - 둘 다 존재해야 함
        if (!validateImagePair(imageEvent, articleId)) {
          continue;
        }

        // 이미지 추가 (imageId, imageUrl, sequence를 사용)
        Integer eventSequence = imageEvent.getSequence();
        if (eventSequence != null && eventSequence > 0) {
          // sequence가 유효한 경우 해당 sequence로 추가
          article.addImageWithSequence(
              imageEvent.getImageId(),
              imageEvent.getImageUrl(),
              eventSequence.longValue());
        } else {
          // sequence가 없는 경우 자동 sequence로 추가
          article.addImage(imageEvent.getImageId(), imageEvent.getImageUrl());
        }

        // 첫 번째 유효한 이미지를 firstImageUrl로 명시적 설정
        if (isFirstImage) {
          article.setFirstImageUrl(imageEvent.getImageUrl());
          isFirstImage = false;
        }
      }

      // 9. 변경 사항 저장
      articleRepository.save(article);

      // 이벤트 처리 완료 로그
      log.info("========== Kafka Event Processing Complete ==========");
      log.info("Topic: {}, ArticleId: {}", topic, articleId);
      log.info("Images processed: {}", images.size());
      log.info("First image URL: {}", article.getFirstImageUrl());
      log.info("=====================================================");

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

  /**
   * 이미지 이벤트의 imageId와 imageUrl 쌍 유효성 검증
   *
   * <p>imageId와 imageUrl은 항상 쌍으로 존재해야 하며, 둘 다 유효한 값이어야 합니다.
   *
   * @param imageEvent 검증할 이미지 이벤트
   * @param articleId 로깅을 위한 게시글 ID
   * @return 유효한 경우 true, 그렇지 않은 경우 false
   */
  private boolean validateImagePair(ImageChangeEvent imageEvent, String articleId) {
    if (imageEvent == null) {
      log.warn("Null image event for articleId: {}", articleId);
      return false;
    }

    String imageId = imageEvent.getImageId();
    String imageUrl = imageEvent.getImageUrl();

    // imageId와 imageUrl 둘 다 존재해야 함
    if (imageId == null || imageId.trim().isEmpty()) {
      log.warn("Missing imageId in image pair for articleId: {}, imageUrl: {}",
               articleId, imageUrl);
      return false;
    }

    if (imageUrl == null || imageUrl.trim().isEmpty()) {
      log.warn("Missing imageUrl in image pair for articleId: {}, imageId: {}",
               articleId, imageId);
      return false;
    }

    // 추가 검증: URL 형식 체크
    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("/")) {
      log.warn("Invalid image URL format for articleId: {}, imageId: {}, imageUrl: {}",
               articleId, imageId, imageUrl);
      return false;
    }

    log.debug("Valid image pair for articleId: {}, imageId: {}, imageUrl: {}",
              articleId, imageId, imageUrl);
    return true;
  }
}

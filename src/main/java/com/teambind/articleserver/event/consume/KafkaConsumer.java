package com.teambind.articleserver.event.consume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.event.events.ArticleImageUpdateRequest;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.ArticleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class KafkaConsumer {
  private final ObjectMapper objectMapper;
  private final ArticleRepository articleRepository;

  @KafkaListener(topics = "post-image-changed", groupId = "article-consumer-group")
  public void articleImageChanger(String message) {
    try {
      List<ArticleImageUpdateRequest> request =
          objectMapper.readValue(message, new TypeReference<List<ArticleImageUpdateRequest>>() {});
      String articleId = request.get(0).getReferenceId();
      Article article =
          articleRepository
              .findById(articleId)
              .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));
      article.removeImages();
      request.forEach(im -> article.addImage(im.getImageUrl(), articleId));
      articleRepository.save(article);
    } catch (JsonProcessingException e) {
      log.error("Failed to deserialize or process article-image-changed message: {}", message, e);
      throw new RuntimeException(e);
    }
  }
  //	@KafkaListener(topics = "post-image-changed" , groupId = "article-consumer-group")
  //	public void profileImageChanger(String message) {
  //		try {
  //			List<> request = objectMapper.readValue(message, ProfileImageChanged.class);
  //			service.updateProfileImage(request.getReferenceId(), request.getImageUrl());
  //		} catch (Exception e) {
  //			// 역직렬화 실패 또는 처리 중 오류 발생 시 로깅/대응
  //			log.error("Failed to deserialize or process profile-create-request message: {}", message,
  // e);
  //			// 필요하면 DLQ 전송이나 재시도 로직 추가
  //		}
  //	}
  //
  //	@KafkaListener(topics = "profile-create-request" , groupId = "profile-consumer-group")
  //	public void createUserProfile(String message) {
  //		try {
  //			ProfileCreateRequest request = objectMapper.readValue(message, ProfileCreateRequest.class);
  //			createUserProfile.createUserProfile(request.getUserId(), request.getProvider());
  //		} catch (Exception e) {
  //			// 역직렬화 실패 또는 처리 중 오류 발생 시 로깅/대응
  //			log.error("Failed to deserialize or process profile-create-request message: {}", message,
  // e);
  //			// 필요하면 DLQ 전송이나 재시도 로직 추가
  //		}
  //	}
}

package com.teambind.articleserver.event.consume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.event.events.ArticleImageUpdateRequest;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.ArticleRepository;
import java.util.Objects;
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

  @KafkaListener(topics = "article-image-changed", groupId = "article-consumer-group")
  public void articleImageChanger(String message) {
    try {
      ArticleImageUpdateRequest request =
          objectMapper.readValue(message, new TypeReference<ArticleImageUpdateRequest>() {});
      String articleId = request.getImageChangeEvents().get(0).getReferenceId();
      Article article =
          articleRepository
              .findById(articleId)
              .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));
      article.removeImages();

      // 비즈 니스 로직 빈 문자열로 올경우, 이미지 전체 삭제
      if (request.getImageChangeEvents().get(0).getImageId() == null
          || Objects.equals(request.getImageChangeEvents().get(0).getImageId(), "")) {
        return;
      }

      request.getImageChangeEvents().forEach(event -> article.addImage(event.getImageUrl()));
      articleRepository.save(article);
    } catch (JsonProcessingException e) {
      log.error("Failed to deserialize or process article-image-changed message: {}", message, e);
      throw new RuntimeException(e);
    }
  }
}

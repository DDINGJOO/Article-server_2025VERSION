package com.teambind.articleserver.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.articleserver.application.port.out.PublishEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 이벤트 어댑터
 *
 * <p>Hexagonal Architecture의 Outbound Adapter입니다. 도메인 이벤트를 Kafka로 발행합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventAdapter implements PublishEventPort {

  private static final String ARTICLE_CREATED_TOPIC = "article.created";
  private static final String ARTICLE_DELETED_TOPIC = "article.deleted";
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void publishArticleCreatedEvent(ArticleCreatedEvent event) {
    try {
      String message = objectMapper.writeValueAsString(event);
      kafkaTemplate.send(ARTICLE_CREATED_TOPIC, event.articleId(), message);
      log.info("Published article created event: articleId={}", event.articleId());
    } catch (Exception e) {
      log.error("Failed to publish article created event", e);
      // 이벤트 발행 실패 시 처리 (재시도, DLQ 등)
    }
  }

  @Override
  public void publishArticleDeletedEvent(ArticleDeletedEvent event) {
    try {
      String message = objectMapper.writeValueAsString(event);
      kafkaTemplate.send(ARTICLE_DELETED_TOPIC, event.articleId(), message);
      log.info("Published article deleted event: articleId={}", event.articleId());
    } catch (Exception e) {
      log.error("Failed to publish article deleted event", e);
      // 이벤트 발행 실패 시 처리 (재시도, DLQ 등)
    }
  }
}

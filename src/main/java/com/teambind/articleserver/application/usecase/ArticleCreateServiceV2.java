package com.teambind.articleserver.service.crud.impl;

import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.out.messaging.KafkaPublisher;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.articleType.EventArticle;
import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import com.teambind.articleserver.adapter.out.persistence.repository.*;
import com.teambind.articleserver.aop.LogTrace;
import com.teambind.articleserver.common.exception.CustomException;
import com.teambind.articleserver.common.exception.ErrorCode;
import com.teambind.articleserver.event.events.ArticleCreatedEvent;
import com.teambind.articleserver.factory.ArticleFactory;
import com.teambind.articleserver.factory.ArticleFactoryRegistry;
import com.teambind.articleserver.factory.ArticleType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 생성 서비스 V2
 *
 * <p>Factory Pattern을 적용하여 OCP(Open-Closed Principle)를 준수합니다. 새로운 게시글 타입 추가 시 기존 코드 수정 없이 확장 가능합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArticleCreateServiceV2 {

  private final ArticleRepository articleRepository;
  private final BoardRepository boardRepository;
  private final KeywordRepository keywordRepository;
  private final ArticleFactoryRegistry factoryRegistry;
  private final KafkaPublisher kafkaPublisher;

  /**
   * 게시글 생성 (통합 메서드)
   *
   * <p>요청 정보를 기반으로 적절한 타입의 게시글을 생성합니다.
   *
   * @param request 게시글 생성 요청
   * @return 생성된 게시글
   */
  @LogTrace(value = "게시글 생성 V2", logParameters = true)
  public Article createArticle(ArticleCreateRequest request) {
    // 게시글 타입 결정
    ArticleType type = determineArticleType(request);
    log.debug("Determined article type: {}", type);

    // 해당 타입의 팩토리 획득
    ArticleFactory factory = factoryRegistry.getFactory(type);

    // 게시글 생성
    Article article = factory.create(request);

    // 저장
    Article savedArticle = articleRepository.save(article);

    // 이벤트 발행
    publishArticleCreatedEvent(savedArticle);

    log.info(
        "Article created successfully - id: {}, type: {}, title: {}",
        savedArticle.getId(),
        type,
        savedArticle.getTitle());

    return savedArticle;
  }

  /**
   * 게시글 수정
   *
   * @param articleId 게시글 ID
   * @param request 수정 요청
   * @return 수정된 게시글
   */
  @LogTrace(value = "게시글 수정 V2", logParameters = true)
  public Article updateArticle(String articleId, ArticleCreateRequest request) {
    Article article =
        articleRepository
            .findById(articleId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));

    // 내용 업데이트
    article.updateContent(request.getTitle(), request.getContent());

    // Board 업데이트 (타입에 따라 제한 가능)
    if (request.getBoardIds() != null) {
      Board board =
          boardRepository
              .findById(request.getBoardIds())
              .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
      article.setBoard(board);
    }

    // 키워드 업데이트
    if (request.getKeywordIds() != null && !request.getKeywordIds().isEmpty()) {
      List<Keyword> keywords = keywordRepository.findAllById(request.getKeywordIds());
      article.replaceKeywords(keywords);
    }

    // 이벤트 게시글인 경우 기간 업데이트
    if (article instanceof EventArticle eventArticle) {
      if (request.getEventStartDate() != null && request.getEventEndDate() != null) {
        eventArticle.updateEventPeriod(request.getEventStartDate(), request.getEventEndDate());
      }
    }

    // JPA Dirty Checking으로 자동 저장
    publishArticleCreatedEvent(article);

    log.info("Article updated - id: {}, title: {}", article.getId(), request.getTitle());
    return article;
  }

  /**
   * 게시글 삭제 (Soft Delete)
   *
   * @param articleId 게시글 ID
   */
  @LogTrace(value = "게시글 삭제 V2", logParameters = true)
  public void deleteArticle(String articleId) {
    Article article =
        articleRepository
            .findById(articleId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));

    // Soft Delete
    article.delete();

    // 이벤트 발행
    publishArticleDeletedEvent(article);

    log.info("Article deleted - id: {}", article.getId());
  }

  /**
   * 요청 정보를 기반으로 게시글 타입을 결정합니다.
   *
   * @param request 게시글 생성 요청
   * @return 결정된 게시글 타입
   */
  private ArticleType determineArticleType(ArticleCreateRequest request) {
    // 이벤트 기간이 있으면 EVENT
    boolean hasEventPeriod =
        request.getEventStartDate() != null && request.getEventEndDate() != null;

    // Board 정보 조회 (타입 결정을 위해)
    String boardName = null;
    if (request.getBoardIds() != null) {
      boardRepository
          .findById(request.getBoardIds())
          .ifPresent(board -> request.setBoardName(board.getName()));
      boardName = request.getBoardName();
    }

    return ArticleType.determineType(request.getBoardIds(), boardName, hasEventPeriod);
  }

  private void publishArticleCreatedEvent(Article article) {
    kafkaPublisher.articleUpdatedEvent(ArticleCreatedEvent.from(article));
  }

  private void publishArticleDeletedEvent(Article article) {
    kafkaPublisher.articleDeletedEvent(ArticleCreatedEvent.from(article));
  }
}

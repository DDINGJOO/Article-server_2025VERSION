package com.teambind.articleserver.application.usecase;

import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.out.messaging.KafkaPublisher;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.articleType.EventArticle;
import com.teambind.articleserver.adapter.out.persistence.entity.articleType.NoticeArticle;
import com.teambind.articleserver.adapter.out.persistence.entity.articleType.RegularArticle;
import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import com.teambind.articleserver.adapter.out.persistence.repository.*;
import com.teambind.articleserver.aop.LogTrace;
import com.teambind.articleserver.application.port.in.CreateArticleUseCase;
import com.teambind.articleserver.application.port.in.event.CreateEventUseCase;
import com.teambind.articleserver.application.port.in.notice.CreateNoticeUseCase;
import com.teambind.articleserver.common.exception.CustomException;
import com.teambind.articleserver.common.exception.ErrorCode;
import com.teambind.articleserver.common.util.generator.primay_key.PrimaryKetGenerator;
import com.teambind.articleserver.event.events.ArticleCreatedEvent;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArticleCreateService
    implements CreateArticleUseCase, CreateNoticeUseCase, CreateEventUseCase {

  private final ArticleRepository articleRepository;
  private final RegularArticleRepository regularArticleRepository;
  private final EventArticleRepository eventArticleRepository;
  private final NoticeArticleRepository noticeArticleRepository;
  private final BoardRepository boardRepository;
  private final KeywordRepository keywordRepository;
  private final PrimaryKetGenerator primaryKetGenerator;
  private final KafkaPublisher kafkaPublisher;

  // 일반 게시글 생성
  @LogTrace(value = "일반 게시글 생성", logParameters = true)
  public RegularArticle createRegularArticle(ArticleCreateRequest request) {
    // Validation 어노테이션으로 검증되므로 추가 검증 불필요
    Board board =
        boardRepository
            .findById(request.getBoardIds())
            .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

    List<Keyword> keywords =
        request.getKeywordIds() != null && !request.getKeywordIds().isEmpty()
            ? keywordRepository.findAllById(request.getKeywordIds())
            : null;

    RegularArticle article =
        RegularArticle.builder()
            .id(primaryKetGenerator.generateKey())
            .title(request.getTitle())
            .content(request.getContent())
            .writerId(request.getWriterId())
            .board(board)
            .keywordMappings(new ArrayList<>())
            .status(Status.ACTIVE)
            .build();

    if (keywords != null && !keywords.isEmpty()) {
      article.addKeywords(keywords);
    }

    RegularArticle savedArticle = regularArticleRepository.save(article);
    publishArticleCreatedEvent(savedArticle);

    log.info(
        "Created regular article: id={}, title={}", savedArticle.getId(), savedArticle.getTitle());
    return savedArticle;
  }

  // 이벤트 게시글 생성 (Board 자동 설정)
  @LogTrace(value = "이벤트 게시글 생성", logParameters = true)
  public EventArticle createEventArticle(ArticleCreateRequest request) {
    // Validation 어노테이션으로 검증되므로 추가 검증 불필요
    Board eventBoard =
        boardRepository
            .findByName("이벤트")
            .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

    List<Keyword> keywords =
        request.getKeywordIds() != null && !request.getKeywordIds().isEmpty()
            ? keywordRepository.findAllById(request.getKeywordIds())
            : null;

    EventArticle article =
        EventArticle.builder()
            .id(primaryKetGenerator.generateKey())
            .title(request.getTitle())
            .content(request.getContent())
            .writerId(request.getWriterId())
            .board(eventBoard)
            .keywordMappings(new ArrayList<>())
            .status(Status.ACTIVE)
            .eventStartDate(request.getEventStartDate())
            .eventEndDate(request.getEventEndDate())
            .build();

    if (keywords != null && !keywords.isEmpty()) {
      article.addKeywords(keywords);
    }

    EventArticle savedArticle = eventArticleRepository.save(article);
    publishArticleCreatedEvent(savedArticle);

    log.info(
        "Created event article: id={}, period=[{} ~ {}]",
        savedArticle.getId(),
        request.getEventStartDate(),
        request.getEventEndDate());
    return savedArticle;
  }

  // 공지사항 생성 (Board 자동 설정)
  @LogTrace(value = "공지사항 생성", logParameters = true)
  public NoticeArticle createNoticeArticle(ArticleCreateRequest request) {
    // Validation 어노테이션으로 검증되므로 추가 검증 불필요
    Board noticeBoard =
        boardRepository
            .findByName("공지사항")
            .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

    List<Keyword> keywords =
        request.getKeywordIds() != null && !request.getKeywordIds().isEmpty()
            ? keywordRepository.findAllById(request.getKeywordIds())
            : null;

    NoticeArticle article =
        NoticeArticle.builder()
            .id(primaryKetGenerator.generateKey())
            .title(request.getTitle())
            .content(request.getContent())
            .writerId(request.getWriterId())
            .board(noticeBoard)
            .keywordMappings(new ArrayList<>())
            .status(Status.ACTIVE)
            .build();

    if (keywords != null && !keywords.isEmpty()) {
      article.addKeywords(keywords);
    }

    NoticeArticle savedArticle = noticeArticleRepository.save(article);
    publishArticleCreatedEvent(savedArticle);

    log.info(
        "Created notice article: id={}, title={}", savedArticle.getId(), savedArticle.getTitle());
    return savedArticle;
  }

  // 하위 호환성을 위한 기존 메서드 (일반 게시글로 처리)
  public Article createArticle(ArticleCreateRequest request) {
    return createRegularArticle(request);
  }

  @LogTrace(value = "게시글 수정", logParameters = true)
  public Article updateArticle(String articleId, ArticleCreateRequest request) {
    Article article =
        articleRepository
            .findById(articleId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));

    // 내용 업데이트
    article.updateContent(request.getTitle(), request.getContent());

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

    // JPA Dirty Checking으로 자동 저장
    publishArticleCreatedEvent(article);

    log.info("Updated article: id={}, title={}", article.getId(), request.getTitle());
    return article;
  }

  @LogTrace(value = "이벤트 게시글 수정", logParameters = true)
  public EventArticle updateEventArticle(String articleId, ArticleCreateRequest request) {
    Article article =
        articleRepository
            .findById(articleId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));

    if (!(article instanceof EventArticle eventArticle)) {
      throw new CustomException(ErrorCode.ARTICLE_TYPE_MISMATCH);
    }

    // 내용 및 이벤트 기간 업데이트
    eventArticle.updateContent(request.getTitle(), request.getContent());
    eventArticle.updateEventPeriod(request.getEventStartDate(), request.getEventEndDate());

    // 키워드 업데이트
    if (request.getKeywordIds() != null && !request.getKeywordIds().isEmpty()) {
      List<Keyword> keywords = keywordRepository.findAllById(request.getKeywordIds());
      eventArticle.replaceKeywords(keywords);
    }

    // JPA Dirty Checking으로 자동 저장
    publishArticleCreatedEvent(eventArticle);

    log.info(
        "Updated event article: id={}, period=[{} ~ {}]",
        eventArticle.getId(),
        request.getEventStartDate(),
        request.getEventEndDate());
    return eventArticle;
  }

  @LogTrace(value = "게시글 삭제", logParameters = true)
  public void deleteArticle(String articleId) {
    Article article =
        articleRepository
            .findById(articleId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));
    // TODO : 이벤트 발행 시점 수정 필요 // 다른 브렌치에서 수정 예정
    publishArticleDeletedEvent(article);
    article.delete();
    // JPA Dirty Checking으로 자동 저장
    log.info("Deleted article: id={}", article.getId());
  }

  private void publishArticleCreatedEvent(Article article) {
    kafkaPublisher.articleUpdatedEvent(ArticleCreatedEvent.from(article));
  }

  // 명시적 구분을 위해 메소드 이름만 바꿈
  private void publishArticleDeletedEvent(Article article) {
    kafkaPublisher.articleDeletedEvent(ArticleCreatedEvent.from(article));
  }

  
}

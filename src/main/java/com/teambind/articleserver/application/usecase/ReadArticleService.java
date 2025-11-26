package com.teambind.articleserver.application.service;

import com.teambind.articleserver.adapter.in.web.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.adapter.in.web.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.adapter.in.web.dto.response.common.ImageInfo;
import com.teambind.articleserver.adapter.in.web.dto.response.common.KeywordInfo;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.BoardRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.KeywordRepository;
import com.teambind.articleserver.application.port.in.ReadArticleUseCase;
import com.teambind.articleserver.application.port.out.LoadArticlePort;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 조회 애플리케이션 서비스
 *
 * <p>Hexagonal Architecture의 Application Service입니다. ReadArticleUseCase를 구현합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReadArticleService implements ReadArticleUseCase {

  private final LoadArticlePort loadArticlePort;
  private final ArticleRepository articleRepository;
  private final ArticleReadService legacyReadService;
  private final BoardRepository boardRepository;
  private final KeywordRepository keywordRepository;

  @Override
  public Optional<ArticleDetailInfo> getArticle(String articleId) {
    log.debug("Getting article with id: {}", articleId);

    return loadArticlePort.loadActiveArticle(articleId).map(this::toArticleDetailInfo);
  }

  @Override
  public ArticlePageInfo searchArticles(SearchArticleQuery query) {
    log.debug("Searching articles with query: {}", query);

    // Convert to legacy criteria (임시 - 추후 완전 마이그레이션 필요)
    ArticleSearchCriteria criteria = buildSearchCriteria(query);
    ArticleCursorPageRequest pageRequest =
        ArticleCursorPageRequest.builder()
            .size(query.size())
            .cursorId(null) // 페이지 기반으로 임시 변환
            .build();

    // Use legacy service for now
    ArticleCursorPageResponse response = legacyReadService.searchArticles(criteria, pageRequest);

    // Convert response
    List<ArticleDetailInfo> articles =
        response.getItems() != null
            ? response.getItems().stream()
                .map(
                    articleResponse ->
                        new ArticleDetailInfo(
                            articleResponse.getArticleId(),
                            articleResponse.getTitle(),
                            articleResponse.getContent(),
                            articleResponse.getWriterId(),
                            articleResponse.getBoard() != null
                                ? articleResponse.getBoard().getBoardName()
                                : null,
                            articleResponse.getKeywords() != null
                                ? articleResponse.getKeywords().stream()
                                    .map(KeywordInfo::getKeywordName)
                                    .collect(Collectors.toList())
                                : List.of(),
                            articleResponse.getImages() != null
                                ? articleResponse.getImages().stream()
                                    .map(ImageInfo::getImageUrl)
                                    .collect(Collectors.toList())
                                : List.of(),
                            articleResponse.getStatus(),
                            articleResponse.getViewCount(),
                            articleResponse.getCreatedAt(),
                            articleResponse.getUpdatedAt()))
                .toList()
            : List.of();

    // Calculate approximate total (cursor-based doesn't have exact total)
    long approximateTotalElements = articles.size();

    return new ArticlePageInfo(
        articles,
        query.page(),
        response.isHasNext() ? query.page() + 1 : query.page(),
        approximateTotalElements,
        response.isHasNext());
  }

  private ArticleSearchCriteria buildSearchCriteria(SearchArticleQuery query) {
    ArticleSearchCriteria.ArticleSearchCriteriaBuilder builder = ArticleSearchCriteria.builder();

    if (query.boardId() != null) {
      boardRepository.findById(query.boardId()).ifPresent(builder::board);
    }

    if (query.keywordIds() != null && !query.keywordIds().isEmpty()) {
      List<Keyword> keywords = keywordRepository.findAllById(query.keywordIds());
      builder.keywords(keywords);
    }

    if (query.title() != null && !query.title().isBlank()) {
      builder.title(query.title());
    }

    if (query.content() != null && !query.content().isBlank()) {
      builder.content(query.content());
    }

    if (query.writerId() != null && !query.writerId().isBlank()) {
      builder.writerId(query.writerId());
    }

    // Parse status
    try {
      Status status = Status.valueOf(query.status());
      builder.status(status);
    } catch (IllegalArgumentException e) {
      builder.status(Status.ACTIVE);
    }

    return builder.build();
  }

  private ArticleDetailInfo toArticleDetailInfo(Article article) {
    return new ArticleDetailInfo(
        article.getId(),
        article.getTitle(),
        article.getContent(),
        article.getWriterId(),
        article.getBoard() != null ? article.getBoard().getName() : null,
        article.getKeywordMappings() != null
            ? article.getKeywordMappings().stream()
                .map(mapping -> mapping.getKeyword().getName())
                .collect(Collectors.toList())
            : List.of(),
        article.getImages() != null
            ? article.getImages().stream()
                .map(img -> img.getImageUrl())
                .collect(Collectors.toList())
            : List.of(),
        article.getStatus().name(),
        article.getViewCount(),
        article.getCreatedAt(),
        article.getUpdatedAt());
  }

  private int calculateTotalPages(long totalElements, int pageSize) {
    return (int) Math.ceil((double) totalElements / pageSize);
  }
}

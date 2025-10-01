package com.teambind.articleserver.service.crud.impl;

import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.dto.response.ArticleResponse;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.ArticleRepository;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleReadService {
  private final ArticleRepository articleRepository;
  private final Convertor convertor;

  public Article fetchArticleById(String articleId) {
    Article article =
        articleRepository
            .findById(articleId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));
    if (article.getStatus() == Status.BLOCKED) {
      throw new CustomException(ErrorCode.ARTICLE_IS_BLOCKED);
    }
    if (article.getStatus() == Status.DELETED) {
      throw new CustomException(ErrorCode.ARTICLE_NOT_FOUND);
    }
    return article;
  }

  public ArticleCursorPageResponse searchArticles(
      ArticleSearchCriteria criteria, ArticleCursorPageRequest pageRequest) {

    Article cursor =
        articleRepository
            .findById((pageRequest.getCursorId()))
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));

    pageRequest.setCursorUpdatedAt(cursor.getUpdatedAt());

    // Normalize board/keywords using Convertor to support id or name input
    if (criteria != null) {
      if (criteria.getBoard() != null) {
        // convert to board id for stable filtering
        var board = convertor.convertBoard(criteria.getBoard());
        criteria.setBoard(board.getId());
      }
      if (criteria.getKeywords() != null) {
        List<Keyword> keywords = convertor.convertKeywords(criteria.getKeywords());
        List<Long> keywordIds = new ArrayList<>();
        for (Keyword k : keywords) keywordIds.add(k.getId());
        criteria.setKeywords(keywordIds);
      }
    }

    int size =
        (pageRequest.getSize() == null || pageRequest.getSize() <= 0) ? 20 : pageRequest.getSize();

    // Derive cursorUpdatedAt from cursorId when not provided
    java.time.LocalDateTime effectiveCursorUpdatedAt = pageRequest.getCursorUpdatedAt();
    String cursorId = pageRequest.getCursorId();
    if (effectiveCursorUpdatedAt == null && cursorId != null && !cursorId.isBlank()) {
      // Use existing validation path: fetchArticleById enforces visibility rules
      Article cursorArticle = fetchArticleById(cursorId);
      effectiveCursorUpdatedAt = cursorArticle.getUpdatedAt();
    }

    List<Article> articles =
        articleRepository.searchByCursor(criteria, effectiveCursorUpdatedAt, cursorId, size + 1);

    boolean hasNext = articles.size() > size;
    if (hasNext) {
      articles = articles.subList(0, size);
    }

    List<ArticleResponse> items = articles.stream().map(ArticleResponse::fromEntity).toList();

    String nextCursorId = null;
    java.time.LocalDateTime nextCursorUpdatedAt = null;
    if (!articles.isEmpty()) {
      Article last = articles.get(articles.size() - 1);
      nextCursorId = last.getId();
      nextCursorUpdatedAt = last.getUpdatedAt();
    }

    return ArticleCursorPageResponse.builder()
        .items(items)
        .nextCursorUpdatedAt(nextCursorUpdatedAt)
        .nextCursorId(nextCursorId)
        .hasNext(hasNext)
        .size(size)
        .build();
  }
}

package com.teambind.articleserver.repository;

import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.entity.article.Article;
import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRepositoryCustom {
  List<Article> searchByCursor(
      ArticleSearchCriteria criteria, LocalDateTime cursorUpdatedAt, String cursorId, int size);
}

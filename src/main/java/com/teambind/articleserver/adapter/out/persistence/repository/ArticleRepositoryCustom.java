package com.teambind.articleserver.adapter.out.persistence.repository;

import com.teambind.articleserver.adapter.in.web.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRepositoryCustom {
  List<Article> searchByCursor(
      ArticleSearchCriteria criteria, LocalDateTime cursorUpdatedAt, String cursorId, int size);
}

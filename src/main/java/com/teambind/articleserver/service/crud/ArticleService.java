package com.teambind.articleserver.service.crud;

import com.teambind.articleserver.adapter.in.web.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import org.springframework.data.domain.Slice;

public interface ArticleService {

  Article createArticle(Article article);

  Article updateArticle(Article article);

  void deleteArticle(String articleId);

  Article fetchArticle(String articleId);

  Slice<Article> fetchArticlesByCriteria(ArticleSearchCriteria c, String cursor, int size);
}

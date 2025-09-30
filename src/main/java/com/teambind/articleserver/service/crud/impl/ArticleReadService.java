package com.teambind.articleserver.service.crud.impl;

import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleReadService {
  private final ArticleRepository articleRepository;

  Article fetchArticleById(String articleId) {
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
}

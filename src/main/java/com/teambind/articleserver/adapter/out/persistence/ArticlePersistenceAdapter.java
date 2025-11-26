package com.teambind.articleserver.adapter.out.persistence;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.application.port.out.LoadArticlePort;
import com.teambind.articleserver.application.port.out.SaveArticlePort;
import com.teambind.articleserver.common.exception.CustomException;
import com.teambind.articleserver.common.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 게시글 영속성 어댑터
 *
 * <p>Hexagonal Architecture의 Outbound Adapter입니다. Port 인터페이스를 구현하여 실제 데이터베이스 작업을 수행합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticlePersistenceAdapter implements LoadArticlePort, SaveArticlePort {

  private final ArticleRepository articleRepository;

  @Override
  public Optional<Article> loadArticle(String articleId) {
    log.debug("Loading article with id: {}", articleId);
    return articleRepository.findById(articleId);
  }

  @Override
  public Optional<Article> loadActiveArticle(String articleId) {
    log.debug("Loading active article with id: {}", articleId);
    return articleRepository
        .findById(articleId)
        .filter(article -> article.getStatus() == Status.ACTIVE);
  }

  @Override
  public boolean existsArticle(String articleId) {
    return articleRepository.existsById(articleId);
  }

  @Override
  public Article saveArticle(Article article) {
    log.info("Saving article with id: {}", article.getId());
    return articleRepository.save(article);
  }

  @Override
  public void deleteArticle(String articleId) {
    log.info("Soft deleting article with id: {}", articleId);
    Article article =
        articleRepository
            .findById(articleId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));

    article.delete();
    articleRepository.save(article);
  }
}

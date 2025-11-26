package com.teambind.articleserver.application.port.out;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;

/** 게시글 저장 Port (Outbound Port) */
public interface SaveArticlePort {

  /**
   * 게시글 저장
   *
   * @param article 저장할 게시글
   * @return 저장된 게시글
   */
  Article saveArticle(Article article);

  /**
   * 게시글 삭제 (Soft Delete)
   *
   * @param articleId 삭제할 게시글 ID
   */
  void deleteArticle(String articleId);
}

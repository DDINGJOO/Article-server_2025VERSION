package com.teambind.articleserver.application.port.out;

import com.teambind.articleserver.entity.article.Article;
import java.util.Optional;

/**
 * 게시글 조회 Port (Outbound Port)
 *
 * Hexagonal Architecture의 Outbound Port입니다.
 * 애플리케이션에서 외부 시스템(DB 등)으로 나가는 요청을 정의합니다.
 */
public interface LoadArticlePort {

    /**
     * ID로 게시글 조회
     *
     * @param articleId 게시글 ID
     * @return 게시글 엔티티
     */
    Optional<Article> loadArticle(String articleId);

    /**
     * 활성화된 게시글 조회
     *
     * @param articleId 게시글 ID
     * @return 활성 게시글 엔티티
     */
    Optional<Article> loadActiveArticle(String articleId);

    /**
     * 게시글 존재 여부 확인
     *
     * @param articleId 게시글 ID
     * @return 존재 여부
     */
    boolean existsArticle(String articleId);
}
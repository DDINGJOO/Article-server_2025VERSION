package com.teambind.articleserver.factory;

import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.entity.article.Article;

/**
 * 게시글 생성 팩토리 인터페이스
 *
 * Strategy Pattern과 Factory Pattern을 결합하여
 * 게시글 타입별 생성 로직을 분리합니다.
 */
public interface ArticleFactory {

    /**
     * 게시글을 생성합니다.
     *
     * @param request 게시글 생성 요청
     * @return 생성된 게시글
     */
    Article create(ArticleCreateRequest request);

    /**
     * 해당 팩토리가 지원하는 게시글 타입을 반환합니다.
     *
     * @return 지원하는 게시글 타입
     */
    ArticleType getSupportedType();
}
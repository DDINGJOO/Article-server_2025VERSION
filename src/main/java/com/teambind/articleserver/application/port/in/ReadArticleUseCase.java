package com.teambind.articleserver.application.port.in;

import java.util.List;
import java.util.Optional;

/**
 * 게시글 조회 Use Case (Inbound Port)
 */
public interface ReadArticleUseCase {

    /**
     * ID로 게시글 조회
     *
     * @param articleId 게시글 ID
     * @return 게시글 정보
     */
    Optional<ArticleDetailInfo> getArticle(String articleId);

    /**
     * 게시글 목록 조회
     *
     * @param query 조회 조건
     * @return 게시글 목록
     */
    ArticlePageInfo searchArticles(SearchArticleQuery query);

    /**
     * 게시글 조회 조건
     */
    record SearchArticleQuery(
        Long boardId,
        String title,
        String content,
        String writerId,
        List<Long> keywordIds,
        String status,
        int page,
        int size
    ) {}

    /**
     * 게시글 상세 정보
     */
    record ArticleDetailInfo(
        String id,
        String title,
        String content,
        String writerId,
        String boardName,
        List<String> keywords,
        List<String> imageUrls,
        String status,
        Long viewCount,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
    ) {}

    /**
     * 페이징된 게시글 목록
     */
    record ArticlePageInfo(
        List<ArticleDetailInfo> articles,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean hasNext
    ) {}
}
package com.teambind.articleserver.domain.repository;

import com.teambind.articleserver.domain.aggregate.ArticleAggregate;
import com.teambind.articleserver.domain.vo.ArticleId;
import com.teambind.articleserver.domain.vo.WriterId;
import java.util.List;
import java.util.Optional;

/**
 * Article 도메인 Repository 인터페이스
 *
 * DDD에서 Repository는 도메인 레이어에 위치합니다.
 * 구현체는 인프라스트럭처 레이어에 위치합니다.
 */
public interface ArticleDomainRepository {

    /**
     * 게시글 저장
     *
     * @param article 저장할 게시글
     * @return 저장된 게시글
     */
    ArticleAggregate save(ArticleAggregate article);

    /**
     * ID로 게시글 조회
     *
     * @param id 게시글 ID
     * @return 게시글 (Optional)
     */
    Optional<ArticleAggregate> findById(ArticleId id);

    /**
     * 작성자로 게시글 조회
     *
     * @param writerId 작성자 ID
     * @return 게시글 목록
     */
    List<ArticleAggregate> findByWriter(WriterId writerId);

    /**
     * 게시글 삭제
     *
     * @param id 삭제할 게시글 ID
     */
    void deleteById(ArticleId id);

    /**
     * 게시글 존재 여부 확인
     *
     * @param id 게시글 ID
     * @return 존재 여부
     */
    boolean existsById(ArticleId id);

    /**
     * 모든 게시글 조회 (페이징 처리 필요)
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 게시글 목록
     */
    List<ArticleAggregate> findAll(int page, int size);

    /**
     * 활성화된 게시글만 조회
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 활성 게시글 목록
     */
    List<ArticleAggregate> findActiveArticles(int page, int size);

    /**
     * 작성자의 활성 게시글 수 조회
     *
     * @param writerId 작성자 ID
     * @return 게시글 수
     */
    long countActiveArticlesByWriter(WriterId writerId);
}
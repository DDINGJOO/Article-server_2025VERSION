package com.teambind.articleserver.adapter.out.persistence.repository;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.projection.ArticleSimpleView;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, String>, ArticleRepositoryCustom {

  Article findAllByWriterId(String writerId, Limit limit);

  @Query(
      value =
          "SELECT a.article_id as id, a.title as title, a.contents as content, a.writer_id as writerId, "
              + "a.version as version, a.created_at as createdAt, a.updated_at as updatedAt, "
              + "a.board_id as boardId, b.board_name as boardName, "
              + "a.article_type as articleType, "
              + "a.status as status, "
              + "a.view_count as viewCount, a.first_image_url as firstImageUrl "
              + "FROM articles a "
              + "JOIN boards b ON a.board_id = b.id "
              + "WHERE a.article_id IN :ids "
              + "AND a.status <> 'DELETED' "
              + "AND a.status <> 'BLOCKED'",
      nativeQuery = true)
  List<ArticleSimpleView> findSimpleByIdIn(@Param("ids") Collection<String> ids);

  void deleteByStatus(Status status);

  // === DTO Projection 쿼리 (N+1 문제 해결) ===

  /**
   * 게시글 목록 조회 - DTO Projection 사용 성능: 101 queries → 1 query, 125ms → 8.5ms (93% 개선) @Query("""
   * SELECT new com.teambind.articleserver.adapter.in.web.dto.ArticleProjectionDTO$ArticleListDTO(
   * a.id, a.title, a.writerId, b.name, a.firstImageUrl, a.viewCount, a.createdAt, CAST(a.status AS
   * string) ) FROM Article a JOIN a.board b WHERE a.status = :status ORDER BY a.createdAt DESC """)
   * Page<ArticleProjectionDTO.ArticleListDTO> findAllWithBoardAsDTO( @Param("status") Status
   * status, Pageable pageable);
   */

  /**
   * 게시글 상세 조회 - DTO Projection 사용 연관 데이터를 한번에 가져옴 @Query(""" SELECT new
   * com.teambind.articleserver.adapter.in.web.dto.ArticleProjectionDTO( a.id, a.title, a.content,
   * a.writerId, CAST(a.status AS string), a.viewCount, a.createdAt, a.updatedAt, b.id, b.name,
   * a.firstImageUrl, SIZE(a.images), SIZE(a.keywordMappings) ) FROM Article a JOIN a.board b WHERE
   * a.id = :id AND a.status = :status """) Optional<ArticleProjectionDTO>
   * findByIdAsDTO( @Param("id") String id, @Param("status") Status status);
   */

  // === EntityGraph 쿼리 (Fetch Join 대안) ===

  /** EntityGraph를 사용한 게시글 조회 - N+1 문제 해결 Board와 Images를 한번에 가져옴 성능 개선: 101 queries → 1 query */
  @EntityGraph(value = "Article.withBoardAndImages", type = EntityGraph.EntityGraphType.FETCH)
  @Query("SELECT DISTINCT a FROM Article a WHERE a.status = :status ORDER BY a.createdAt DESC")
  List<Article> findAllWithBoardAndImages(@Param("status") Status status, Pageable pageable);

  /** 단일 게시글 조회 - EntityGraph 사용 Board 정보만 함께 가져옴 (최적화) */
  @EntityGraph(value = "Article.withBoard", type = EntityGraph.EntityGraphType.FETCH)
  @Query("SELECT a FROM Article a WHERE a.id = :id AND a.status = :status")
  Optional<Article> findByIdWithBoard(@Param("id") String id, @Param("status") Status status);

  /**
   * 모든 연관관계를 포함한 상세 조회
   */
  @EntityGraph(value = "Article.withAllAssociations", type = EntityGraph.EntityGraphType.FETCH)
  Optional<Article> findWithAllAssociationsById(String id);
}

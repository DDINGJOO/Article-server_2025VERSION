package com.teambind.articleserver.entity.keyword;

import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.embeddable_id.KeywordMappingTableId;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글-키워드 매핑 테이블
 *
 * <p>게시글과 키워드의 다대다 관계를 표현하는 중간 테이블
 */
@Entity
@Table(
    name = "keyword_mapping_table",
    indexes = {
      @Index(name = "idx_mapping_article", columnList = "article_id"),
      @Index(name = "idx_mapping_keyword", columnList = "keyword_id"),
      @Index(name = "idx_mapping_created", columnList = "created_at")
    })
@NoArgsConstructor
@Getter
@Setter
public class KeywordMappingTable {

  @EmbeddedId private KeywordMappingTableId id;

  /** 매핑이 생성된 시간 */
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** 키워드가 매핑된 게시글 */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("articleId")
  @JoinColumn(name = "article_id", insertable = false, updatable = false)
  private Article article;

  /** 게시글에 매핑된 키워드 */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("keywordId")
  @JoinColumn(name = "keyword_id", insertable = false, updatable = false)
  private Keyword keyword;

  // === 생성자 ===

  /**
   * KeywordMappingTable 생성자
   *
   * <p>Article과 Keyword를 매핑합니다.
   *
   * <p>주의: Keyword는 반드시 persist된 상태여야 합니다 (ID가 있어야 함).
   *
   * @param article 게시글
   * @param keyword 키워드 (persist된 엔티티)
   * @throws IllegalArgumentException keyword의 ID가 null인 경우
   */
  public KeywordMappingTable(Article article, Keyword keyword) {
    if (article == null || keyword == null) {
      throw new IllegalArgumentException("Article and Keyword cannot be null");
    }
    if (article.getId() == null) {
      throw new IllegalArgumentException("Article must have an ID");
    }
    if (keyword.getId() == null) {
      throw new IllegalArgumentException(
          "Keyword must be persisted before creating mapping (ID is null)");
    }

    this.id = new KeywordMappingTableId(keyword.getId(), article.getId());
    this.article = article;
    this.keyword = keyword;
    this.createdAt = LocalDateTime.now();
  }

  // === 편의 메서드 ===

  /**
   * 게시글 ID 조회
   *
   * @return 게시글 ID
   */
  public String getArticleId() {
    return this.id != null ? this.id.getArticleId() : null;
  }

  /**
   * 키워드 ID 조회
   *
   * @return 키워드 ID
   */
  public Long getKeywordId() {
    return this.id != null ? this.id.getKeywordId() : null;
  }

  /**
   * 연관관계 제거
   *
   * <p>Article과 Keyword 양쪽에서 이 매핑을 제거합니다.
   */
  public void detach() {
    if (this.article != null) {
      this.article.getKeywordMappings().remove(this);
    }
    if (this.keyword != null) {
      this.keyword.getMappings().remove(this);
    }
    this.article = null;
    this.keyword = null;
  }

  // === JPA 라이프사이클 ===

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  // === equals & hashCode ===

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof KeywordMappingTable)) return false;
    KeywordMappingTable that = (KeywordMappingTable) o;
    return id != null && id.equals(that.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "KeywordMappingTable{"
        + "articleId='"
        + getArticleId()
        + '\''
        + ", keywordId="
        + getKeywordId()
        + ", createdAt="
        + createdAt
        + '}';
  }
}

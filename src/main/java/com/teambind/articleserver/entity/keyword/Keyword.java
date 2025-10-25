package com.teambind.articleserver.entity.keyword;

import com.teambind.articleserver.entity.board.Board;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * 키워드 엔티티
 *
 * <p>키워드는 두 가지 타입으로 존재:
 *
 * <ul>
 *   <li>공통 키워드: board가 null인 경우, 모든 게시판에서 사용 가능
 *   <li>보드 전용 키워드: board가 지정된 경우, 해당 게시판에서만 사용 가능
 * </ul>
 */
@Entity
@Table(
    name = "keywords",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_keyword_board",
          columnNames = {"keyword_name", "board_id"})
    },
    indexes = {
      @Index(name = "idx_keyword_board", columnList = "board_id"),
      @Index(name = "idx_keyword_name", columnList = "keyword_name")
    })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Keyword {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "keyword_id", nullable = false)
  private Long id;

  @Column(name = "keyword_name", nullable = false, length = 50)
  private String name;

  /** 키워드가 속한 게시판 null인 경우 공통 키워드 (모든 게시판에서 사용 가능) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id")
  private Board board;

  /** 키워드 사용 빈도 (캐싱 최적화용) */
  @Column(name = "usage_count", nullable = false)
  @Builder.Default
  private Integer usageCount = 0;

  /** 키워드 활성화 여부 */
  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  /** 게시글과의 매핑 관계 */
  @OneToMany(
      mappedBy = "keyword",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @ToString.Exclude
  @Builder.Default
  private List<KeywordMappingTable> mappings = new ArrayList<>();

  // === 생성자 ===

  public Keyword(String name, Board board) {
    this.name = name;
    this.board = board;
    this.usageCount = 0;
    this.isActive = true;
    this.mappings = new ArrayList<>();
  }

  // === 연관관계 편의 메서드 ===

  /**
   * 키워드 매핑 추가
   *
   * <p>Article에서 호출하므로 순환 참조를 방지하기 위해 단순 추가만 수행합니다.
   *
   * <p><strong>주의:</strong> 이 메서드는 Article.addKeyword()에서만 호출해야 합니다. 직접 호출 시 양방향 관계가 깨질 수 있습니다.
   *
   * @param mapping 매핑 테이블
   */
  public void addMappingInternal(KeywordMappingTable mapping) {
    if (mappings == null) {
      mappings = new ArrayList<>();
    }
    if (!mappings.contains(mapping)) {
      mappings.add(mapping);
    }
  }

  /**
   * 키워드 매핑 제거
   *
   * <p>Article에서 호출하므로 순환 참조를 방지하기 위해 단순 제거만 수행합니다.
   *
   * <p><strong>주의:</strong> 이 메서드는 Article.removeKeyword()에서만 호출해야 합니다. 직접 호출 시 양방향 관계가 깨질 수 있습니다.
   *
   * @param mapping 매핑 테이블
   */
  public void removeMappingInternal(KeywordMappingTable mapping) {
    if (mappings != null) {
      mappings.remove(mapping);
    }
  }

  /** 모든 매핑 제거 */
  public void clearMappings() {
    if (mappings != null) {
      mappings.clear();
      this.usageCount = 0;
    }
  }

  /**
   * 보드 설정 (연관관계 편의 메서드)
   *
   * <p>양방향 관계를 안전하게 설정합니다.
   *
   * @param board 설정할 게시판
   */
  public void assignToBoard(Board board) {
    // 기존 보드에서 제거
    if (this.board != null && this.board != board) {
      List<Keyword> keywords = this.board.getKeywords();
      if (keywords != null) {
        keywords.remove(this);
      }
    }

    // 새 보드에 추가
    this.board = board;
    if (board != null) {
      List<Keyword> keywords = board.getKeywords();
      if (keywords != null && !keywords.contains(this)) {
        keywords.add(this);
      }
    }
  }

  /** 보드에서 분리 */
  public void detachFromBoard() {
    if (this.board != null) {
      List<Keyword> keywords = this.board.getKeywords();
      if (keywords != null) {
        keywords.remove(this);
      }
      this.board = null;
    }
  }

  // === 비즈니스 로직 ===

  /**
   * 공통 키워드 여부 확인
   *
   * @return 공통 키워드 여부
   */
  public boolean isCommonKeyword() {
    return this.board == null;
  }

  /**
   * 특정 보드의 키워드 여부 확인
   *
   * @param board 확인할 게시판
   * @return 해당 게시판의 키워드 여부
   */
  public boolean belongsToBoard(Board board) {
    return this.board != null && this.board.equals(board);
  }

  /** 사용 빈도 증가 */
  public void incrementUsageCount() {
    if (this.usageCount == null) {
      this.usageCount = 0;
    }
    this.usageCount++;
  }

  /** 사용 빈도 감소 */
  public void decrementUsageCount() {
    if (this.usageCount == null) {
      this.usageCount = 0;
      return;
    }
    if (this.usageCount > 0) {
      this.usageCount--;
    }
  }

  /** 키워드 활성화 */
  public void activate() {
    this.isActive = true;
  }

  /** 키워드 비활성화 */
  public void deactivate() {
    this.isActive = false;
  }

  // === JPA 라이프사이클 ===

  @PrePersist
  protected void onCreate() {
    if (usageCount == null) {
      usageCount = 0;
    }
    if (isActive == null) {
      isActive = true;
    }
    if (mappings == null) {
      mappings = new ArrayList<>();
    }
  }

  // === equals & hashCode ===

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Keyword)) return false;
    Keyword keyword = (Keyword) o;
    return id != null && id.equals(keyword.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Keyword{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", boardId="
        + (board != null ? board.getId() : null)
        + ", usageCount="
        + usageCount
        + ", isActive="
        + isActive
        + '}';
  }
}

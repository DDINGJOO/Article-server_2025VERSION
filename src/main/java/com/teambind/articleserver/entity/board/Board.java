package com.teambind.articleserver.entity.board;

import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.keyword.Keyword;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * 게시판 엔티티
 *
 * <p>게시판은 게시글을 분류하는 카테고리이며, 각 게시판은 전용 키워드를 가질 수 있다.
 */
@Entity
@Table(
    name = "boards",
    uniqueConstraints = {@UniqueConstraint(name = "uk_board_name", columnNames = "board_name")},
    indexes = {
      @Index(name = "idx_board_name", columnList = "board_name"),
      @Index(name = "idx_board_active", columnList = "is_active")
    })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Board {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "board_id", nullable = false)
  private Long id;

  @Column(name = "board_name", nullable = false, unique = true, length = 50)
  private String name;

  @Column(name = "description", length = 200)
  private String description;

  /** 게시판 활성화 여부 */
  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  /** 게시판 순서 (정렬용) */
  @Column(name = "display_order")
  private Integer displayOrder;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /** 게시판에 속한 게시글들 */
  @OneToMany(mappedBy = "board", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
  @ToString.Exclude
  @Builder.Default
  private List<Article> articles = new ArrayList<>();

  /** 게시판 전용 키워드들 */
  @OneToMany(
      mappedBy = "board",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @ToString.Exclude
  @Builder.Default
  private List<Keyword> keywords = new ArrayList<>();

  // === 생성자 ===

  public Board(String name) {
    this.name = name;
    this.isActive = true;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.articles = new ArrayList<>();
    this.keywords = new ArrayList<>();
  }

  public Board(String name, String description) {
    this(name);
    this.description = description;
  }

  // === 연관관계 편의 메서드 - Article ===

  /**
   * 게시글 추가
   *
   * <p>양방향 관계를 안전하게 설정합니다.
   *
   * @param article 추가할 게시글
   */
  public void addArticle(Article article) {
    if (article == null) {
      return;
    }

    if (articles == null) {
      articles = new ArrayList<>();
    }

    if (!articles.contains(article)) {
      articles.add(article);
    }

    // 게시글의 board 참조가 현재 board가 아니면 설정
    if (article.getBoard() != this) {
      article.setBoard(this);
    }
  }

  /**
   * 게시글 제거
   *
   * @param article 제거할 게시글
   */
  public void removeArticle(Article article) {
    if (articles == null || article == null) {
      return;
    }

    if (articles.remove(article)) {
      article.setBoard(null);
    }
  }

  /** 모든 게시글 제거 */
  public void clearArticles() {
    if (articles != null) {
      // ConcurrentModificationException 방지를 위해 복사본 생성
      new ArrayList<>(articles).forEach(article -> article.setBoard(null));
      articles.clear();
    }
  }

  // === 연관관계 편의 메서드 - Keyword ===

  /**
   * 키워드 추가
   *
   * <p>양방향 관계를 안전하게 설정합니다.
   *
   * @param keyword 추가할 키워드
   */
  public void addKeyword(Keyword keyword) {
    if (keyword == null) {
      return;
    }

    if (keywords == null) {
      keywords = new ArrayList<>();
    }

    if (!keywords.contains(keyword)) {
      keywords.add(keyword);
    }

    // Keyword의 board 참조가 현재 board가 아니면 설정
    if (keyword.getBoard() != this) {
      keyword.assignToBoard(this);
    }
  }

  /**
   * 키워드 제거
   *
   * @param keyword 제거할 키워드
   */
  public void removeKeyword(Keyword keyword) {
    if (keywords == null || keyword == null) {
      return;
    }

    if (keywords.remove(keyword)) {
      keyword.detachFromBoard();
    }
  }

  /** 모든 키워드 제거 */
  public void clearKeywords() {
    if (keywords != null) {
      // ConcurrentModificationException 방지를 위해 복사본 생성
      new ArrayList<>(keywords).forEach(Keyword::detachFromBoard);
      keywords.clear();
    }
  }

  /**
   * 키워드 이름으로 조회
   *
   * @param keywordName 키워드 이름
   * @return 키워드 엔티티 (없으면 null)
   */
  public Keyword findKeywordByName(String keywordName) {
    if (keywords == null || keywordName == null) {
      return null;
    }
    return keywords.stream().filter(k -> keywordName.equals(k.getName())).findFirst().orElse(null);
  }

  /**
   * 활성화된 키워드만 조회
   *
   * @return 활성화된 키워드 리스트
   */
  public List<Keyword> getActiveKeywords() {
    if (keywords == null) {
      return new ArrayList<>();
    }
    return keywords.stream()
        .filter(keyword -> keyword.getIsActive() != null && keyword.getIsActive())
        .toList();
  }

  // === 비즈니스 로직 ===

  /**
   * 게시판 정보 업데이트
   *
   * @param name 게시판 이름
   * @param description 게시판 설명
   */
  public void updateInfo(String name, String description) {
    if (name != null && !name.isBlank()) {
      this.name = name;
    }
    this.description = description;
    this.updatedAt = LocalDateTime.now();
  }

  /** 게시판 활성화 */
  public void activate() {
    this.isActive = true;
    this.updatedAt = LocalDateTime.now();
  }

  /** 게시판 비활성화 */
  public void deactivate() {
    this.isActive = false;
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * 표시 순서 변경
   *
   * @param order 표시 순서
   */
  public void updateDisplayOrder(Integer order) {
    this.displayOrder = order;
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * 게시글 수 조회
   *
   * @return 게시글 수
   */
  public int getArticleCount() {
    return articles != null ? articles.size() : 0;
  }

  /**
   * 키워드 수 조회
   *
   * @return 키워드 수
   */
  public int getKeywordCount() {
    return keywords != null ? keywords.size() : 0;
  }

  // === JPA 라이프사이클 ===

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (updatedAt == null) {
      updatedAt = LocalDateTime.now();
    }
    if (isActive == null) {
      isActive = true;
    }
    if (articles == null) {
      articles = new ArrayList<>();
    }
    if (keywords == null) {
      keywords = new ArrayList<>();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // === equals & hashCode ===

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Board)) return false;
    Board board = (Board) o;
    return id != null && id.equals(board.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Board{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", isActive="
        + isActive
        + ", displayOrder="
        + displayOrder
        + ", articleCount="
        + getArticleCount()
        + ", keywordCount="
        + getKeywordCount()
        + '}';
  }
}

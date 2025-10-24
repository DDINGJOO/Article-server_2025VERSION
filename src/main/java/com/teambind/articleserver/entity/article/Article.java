package com.teambind.articleserver.entity.article;

import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.entity.image.ArticleImage;
import com.teambind.articleserver.entity.keyword.Keyword;
import com.teambind.articleserver.entity.keyword.KeywordMappingTable;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * 게시글 엔티티 (추상 클래스)
 *
 * <p>Single Table Inheritance 전략 사용
 *
 * <ul>
 *   <li>RegularArticle: 일반 게시글
 *   <li>EventArticle: 이벤트 게시글 (기간 정보 포함)
 *   <li>NoticeArticle: 공지사항
 * </ul>
 */
@Entity
@Table(
    name = "articles",
    indexes = {
      // 단일 인덱스 (기존 유지 - 다른 쿼리에서 사용될 수 있음)
      @Index(name = "idx_article_board", columnList = "board_id"),
      @Index(name = "idx_article_writer", columnList = "writer_id"),

      // 복합 인덱스 (성능 최적화)
      // 1. 상태별 최신순 조회 (가장 자주 사용)
      @Index(name = "idx_status_created_id", columnList = "status, created_at, article_id"),

      // 2. 커서 페이징용 (updated_at 기준)
      @Index(name = "idx_status_updated_id", columnList = "status, updated_at, article_id"),

      // 3. 게시판별 상태 및 최신순 조회
      @Index(name = "idx_board_status_created", columnList = "board_id, status, created_at"),

      // 4. 작성자별 상태 및 최신순 조회
      @Index(name = "idx_writer_status_created", columnList = "writer_id, status, created_at"),

      // 5. 타입별 조회 (Single Table Inheritance)
      @Index(name = "idx_type_status_created", columnList = "article_type, status, created_at"),

      // 6. EventArticle 전용 인덱스
      // 진행중 이벤트 조회: WHERE status = ? AND now BETWEEN start AND end
      @Index(
          name = "idx_event_status_dates",
          columnList = "article_type, status, event_start_date, event_end_date"),

      // 종료된 이벤트 조회: WHERE status = ? AND end < now
      @Index(name = "idx_event_status_end", columnList = "article_type, status, event_end_date"),

      // 예정 이벤트 조회: WHERE status = ? AND start > now
      @Index(name = "idx_event_status_start", columnList = "article_type, status, event_start_date")
    })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "article_type", discriminatorType = DiscriminatorType.STRING)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@SuperBuilder
@Slf4j
public abstract class Article {

  @Id
  @Column(name = "article_id", nullable = false, length = 50)
  private String id;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "contents", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "writer_id", nullable = false, length = 50)
  private String writerId;

  @Version
  @Column(name = "version")
  private Long version;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private Status status = Status.ACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /** 게시글이 속한 게시판 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  /** 대표 이미지 URL (첫 번째 이미지) */
  @Column(name = "first_image_url", length = 500)
  private String firstImageUrl;

  /** 조회 수 */
  @Column(name = "view_count", nullable = false)
  @Builder.Default
  private Long viewCount = 0L;

  /** 게시글의 이미지들 */
  @OneToMany(
      mappedBy = "article",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @org.hibernate.annotations.BatchSize(size = 100)
  @ToString.Exclude
  @Builder.Default
  private List<ArticleImage> images = new ArrayList<>();

  /** 게시글의 키워드 매핑 */
  @OneToMany(
      mappedBy = "article",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @org.hibernate.annotations.BatchSize(size = 100)
  @ToString.Exclude
  @Builder.Default
  private List<KeywordMappingTable> keywordMappings = new ArrayList<>();

  // === 연관관계 편의 메서드 - Board ===

  /**
   * 게시판 설정
   *
   * <p>양방향 관계를 안전하게 설정합니다.
   */
  public void setBoard(Board board) {
    // 기존 게시판에서 제거
    if (this.board != null) {
      List<Article> articles = this.board.getArticles();
      if (articles != null) {
        articles.remove(this);
      }
    }

    // 새 게시판에 추가
    this.board = board;
    if (board != null) {
      List<Article> articles = board.getArticles();
      if (articles != null && !articles.contains(this)) {
        articles.add(this);
      }
    }

    this.updatedAt = LocalDateTime.now();
  }

  // === 연관관계 편의 메서드 - Image ===

  /**
   * 이미지 추가
   *
   * <p>시퀀스는 자동으로 계산되며, 첫 번째 이미지는 대표 이미지로 설정됩니다.
   *
   * @param imageId 이미지 ID
   * @param imageUrl 이미지 URL
   */
  public void addImage(String imageId, String imageUrl) {
    if (imageId == null || imageUrl == null) {
      log.warn("Cannot add image with null imageId or imageUrl");
      return;
    }

    if (images == null) {
      images = new ArrayList<>();
    }

    long sequence = images.size() + 1;
    ArticleImage articleImage = new ArticleImage(this, sequence, imageUrl, imageId);
    images.add(articleImage);

    // 첫 번째 이미지를 대표 이미지로 설정
    if (sequence == 1) {
      this.firstImageUrl = imageUrl;
    }

    this.updatedAt = LocalDateTime.now();
  }

  /**
   * 이미지 제거
   *
   * <p>대표 이미지가 제거될 경우 자동으로 재설정됩니다.
   *
   * @param image 제거할 이미지
   */
  public void removeImage(ArticleImage image) {
    if (images == null || image == null) {
      return;
    }

    boolean removed = images.remove(image);

    if (removed) {
      // orphanRemoval이 자동으로 처리하므로 article 설정 불필요

      // 대표 이미지 재설정
      if (images.isEmpty()) {
        this.firstImageUrl = null;
      } else if (firstImageUrl != null && firstImageUrl.equals(image.getImageUrl())) {
        this.firstImageUrl = images.get(0).getImageUrl();
      }

      this.updatedAt = LocalDateTime.now();
    }
  }

  /** 모든 이미지 제거 */
  public void removeImages() {
    if (images != null) {
      images.clear();
      this.firstImageUrl = null;
      this.updatedAt = LocalDateTime.now();
    }
  }

  /**
   * 대표 이미지 URL 설정
   *
   * @param imageUrl 대표 이미지 URL
   */
  public void setFirstImageUrl(String imageUrl) {
    this.firstImageUrl = imageUrl;
    this.updatedAt = LocalDateTime.now();
  }

  // === 연관관계 편의 메서드 - Keyword ===

  /**
   * 키워드 추가
   *
   * <p>중복 체크를 수행하고 양방향 관계를 안전하게 설정합니다.
   *
   * @param keyword 추가할 키워드 (persist된 엔티티여야 함)
   */
  public void addKeyword(Keyword keyword) {
    if (keyword == null) {
      log.warn("Cannot add null keyword");
      return;
    }

    if (keywordMappings == null) {
      keywordMappings = new ArrayList<>();
    }

    // 중복 확인
    boolean exists =
        keywordMappings.stream()
            .anyMatch(
                mapping -> {
                  Keyword existingKeyword = mapping.getKeyword();
                  return existingKeyword != null && existingKeyword.equals(keyword);
                });

    if (!exists) {
      KeywordMappingTable mapping = new KeywordMappingTable(this, keyword);
      keywordMappings.add(mapping);

      // Keyword 쪽 양방향 관계 설정 (package-private 메서드 사용)
      keyword.addMappingInternal(mapping);

      // 사용 빈도 증가
      keyword.incrementUsageCount();

      this.updatedAt = LocalDateTime.now();
    }
  }

  /**
   * 키워드 리스트 일괄 추가
   *
   * @param keywords 추가할 키워드 리스트
   */
  public void addKeywords(List<Keyword> keywords) {
    if (keywords != null && !keywords.isEmpty()) {
      keywords.forEach(this::addKeyword);
    }
  }

  /**
   * 키워드 제거
   *
   * <p>양방향 관계를 안전하게 정리합니다.
   *
   * @param keyword 제거할 키워드
   */
  public void removeKeyword(Keyword keyword) {
    if (keywordMappings == null || keyword == null) {
      return;
    }

    keywordMappings.removeIf(
        mapping -> {
          Keyword mappedKeyword = mapping.getKeyword();
          if (mappedKeyword != null && mappedKeyword.equals(keyword)) {
            // Keyword 쪽 양방향 관계 정리 (package-private 메서드 사용)
            keyword.removeMappingInternal(mapping);

            // 사용 빈도 감소
            keyword.decrementUsageCount();

            return true;
          }
          return false;
        });

    this.updatedAt = LocalDateTime.now();
  }

  /** 모든 키워드 제거 */
  public void removeKeywords() {
    if (keywordMappings != null) {
      // 각 키워드의 사용 빈도 감소 및 양방향 관계 정리
      keywordMappings.forEach(
          mapping -> {
            Keyword keyword = mapping.getKeyword();
            if (keyword != null) {
              // Keyword 쪽 양방향 관계 정리 (package-private 메서드 사용)
              keyword.removeMappingInternal(mapping);
              keyword.decrementUsageCount();
            }
          });

      keywordMappings.clear();
      this.updatedAt = LocalDateTime.now();
    }
  }

  /**
   * 키워드 교체 (기존 제거 + 새로 추가)
   *
   * @param newKeywords 새로운 키워드 리스트
   */
  public void replaceKeywords(List<Keyword> newKeywords) {
    removeKeywords();
    addKeywords(newKeywords);
  }

  /**
   * 게시판의 키워드만 필터링하여 반환
   *
   * @return 게시판 전용 키워드 리스트
   */
  public List<Keyword> getBoardKeywords() {
    if (keywordMappings == null || board == null) {
      return new ArrayList<>();
    }
    return keywordMappings.stream()
        .map(KeywordMappingTable::getKeyword)
        .filter(keyword -> keyword != null && keyword.belongsToBoard(board))
        .toList();
  }

  /**
   * 공통 키워드만 필터링하여 반환
   *
   * @return 공통 키워드 리스트
   */
  public List<Keyword> getCommonKeywords() {
    if (keywordMappings == null) {
      return new ArrayList<>();
    }
    return keywordMappings.stream()
        .map(KeywordMappingTable::getKeyword)
        .filter(keyword -> keyword != null && keyword.isCommonKeyword())
        .toList();
  }

  // === 비즈니스 로직 ===

  /**
   * 게시글 내용 수정
   *
   * @param title 제목
   * @param content 내용
   */
  public void updateContent(String title, String content) {
    if (title != null && !title.isBlank()) {
      this.title = title;
    }
    if (content != null && !content.isBlank()) {
      this.content = content;
    }
    this.updatedAt = LocalDateTime.now();
  }

  /** 게시글 삭제 (Soft Delete) */
  public void delete() {
    this.status = Status.DELETED;
    this.updatedAt = LocalDateTime.now();
  }

  /** 게시글 차단 */
  public void block() {
    this.status = Status.BLOCKED;
    this.updatedAt = LocalDateTime.now();
  }

  /** 게시글 활성화 */
  public void activate() {
    this.status = Status.ACTIVE;
    this.updatedAt = LocalDateTime.now();
  }

  /** 조회 수 증가 */
  public void incrementViewCount() {
    if (this.viewCount == null) {
      this.viewCount = 0L;
    }
    this.viewCount++;
  }

  /** 게시글 활성 상태 확인 */
  public boolean isActive() {
    return this.status == Status.ACTIVE;
  }

  /** 게시글 삭제 상태 확인 */
  public boolean isDeleted() {
    return this.status == Status.DELETED;
  }

  /** 게시글 차단 상태 확인 */
  public boolean isBlocked() {
    return this.status == Status.BLOCKED;
  }

  /**
   * 작성자 확인
   *
   * @param userId 사용자 ID
   * @return 작성자 여부
   */
  public boolean isWrittenBy(String userId) {
    return this.writerId != null && this.writerId.equals(userId);
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
    if (status == null) {
      status = Status.ACTIVE;
    }
    if (viewCount == null) {
      viewCount = 0L;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // === ID 설정 (테스트용) ===

  /**
   * ID 설정
   *
   * <p>프로덕션에서는 Snowflake ID 생성기가 자동으로 ID를 생성하지만, 테스트 환경에서는 수동으로 ID를 설정해야 합니다.
   *
   * <p><strong>주의:</strong> 이 메서드는 테스트 환경에서만 사용해야 합니다. 프로덕션 코드에서 호출하지 마세요.
   *
   * @param id 설정할 ID
   */
  public void setId(String id) {
    this.id = id;
  }

  // === equals & hashCode ===

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Article)) return false;
    Article article = (Article) o;
    return id != null && id.equals(article.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Article{"
        + "id='"
        + id
        + '\''
        + ", title='"
        + title
        + '\''
        + ", writerId='"
        + writerId
        + '\''
        + ", status="
        + status
        + ", boardId="
        + (board != null ? board.getId() : null)
        + ", imageCount="
        + (images != null ? images.size() : 0)
        + ", keywordCount="
        + (keywordMappings != null ? keywordMappings.size() : 0)
        + ", viewCount="
        + viewCount
        + '}';
  }
}

package com.teambind.articleserver.domain.model.article;

import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.entity.image.ArticleImage;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.KeywordMappingTable;
import com.teambind.articleserver.domain.event.ArticleCreatedDomainEvent;
import com.teambind.articleserver.domain.event.ArticleDeletedDomainEvent;
import com.teambind.articleserver.domain.model.AggregateRoot;
import com.teambind.articleserver.domain.valueobject.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * Article Aggregate Root
 *
 * <p>DDD의 Aggregate Root 패턴을 적용한 게시글 엔티티입니다. - Value Object 사용 - 도메인 이벤트 발행 - 불변 조건 보장
 */
@Entity
@Table(name = "articles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "article_type", discriminatorType = DiscriminatorType.STRING)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class ArticleAggregate extends AggregateRoot {

  @EmbeddedId private ArticleId id;

  @Embedded private Title title;

  @Embedded private Content content;

  @Embedded private WriterId writerId;

  @Version
  @Column(name = "version")
  private Long version;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private Status status = Status.ACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  @Column(name = "first_image_url", length = 500)
  private String firstImageUrl;

  @Column(name = "view_count", nullable = false)
  private Long viewCount = 0L;

  @Transient private List<ArticleImage> images = new ArrayList<>();

  @Transient private List<KeywordMappingTable> keywordMappings = new ArrayList<>();

  /** 생성자 */
  protected ArticleAggregate(
      ArticleId id, Title title, Content content, WriterId writerId, Board board) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.writerId = writerId;
    this.board = board;
    this.status = Status.ACTIVE;
    this.viewCount = 0L;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.images = new ArrayList<>();
    this.keywordMappings = new ArrayList<>();

    // 도메인 이벤트 발행
    raiseArticleCreatedEvent();
  }

  /** 게시글 생성 이벤트 발행 */
  protected void raiseArticleCreatedEvent() {
    registerEvent(
        new ArticleCreatedDomainEvent(
            id, title, content, writerId, board != null ? board.getId() : null));
  }

  /**
   * 게시글 내용 수정
   *
   * @param newTitle 새로운 제목
   * @param newContent 새로운 내용
   */
  public void updateContent(Title newTitle, Content newContent) {
    if (newTitle != null) {
      this.title = newTitle;
    }
    if (newContent != null) {
      this.content = newContent;
    }
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * 게시글 삭제 (Soft Delete)
   *
   * @param reason 삭제 사유
   */
  public void delete(String reason) {
    if (this.status == Status.DELETED) {
      return; // 이미 삭제된 경우
    }

    this.status = Status.DELETED;
    this.updatedAt = LocalDateTime.now();

    // 도메인 이벤트 발행
    registerEvent(new ArticleDeletedDomainEvent(id, title, writerId, reason));
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

  /**
   * 키워드 추가
   *
   * @param keyword 추가할 키워드
   */
  public void addKeyword(Keyword keyword) {
    if (keyword == null) {
      return;
    }

    boolean exists =
        keywordMappings.stream()
            .anyMatch(
                mapping -> {
                  Keyword k = mapping.getKeyword();
                  return k != null && k.equals(keyword);
                });

    if (!exists) {
      KeywordMappingTable mapping = new KeywordMappingTable(null, keyword);
      keywordMappings.add(mapping);
      keyword.incrementUsageCount();
      this.updatedAt = LocalDateTime.now();
    }
  }

  /**
   * 키워드 제거
   *
   * @param keyword 제거할 키워드
   */
  public void removeKeyword(Keyword keyword) {
    keywordMappings.removeIf(
        mapping -> {
          Keyword k = mapping.getKeyword();
          if (k != null && k.equals(keyword)) {
            keyword.decrementUsageCount();
            return true;
          }
          return false;
        });
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * 키워드 교체
   *
   * @param newKeywords 새로운 키워드 리스트
   */
  public void replaceKeywords(List<Keyword> newKeywords) {
    // 기존 키워드 제거
    keywordMappings.forEach(
        mapping -> {
          Keyword keyword = mapping.getKeyword();
          if (keyword != null) {
            keyword.decrementUsageCount();
          }
        });
    keywordMappings.clear();

    // 새로운 키워드 추가
    if (newKeywords != null) {
      newKeywords.forEach(this::addKeyword);
    }
  }

  /** 게시글 상태 확인 메서드들 */
  public boolean isActive() {
    return this.status == Status.ACTIVE;
  }

  public boolean isDeleted() {
    return this.status == Status.DELETED;
  }

  public boolean isBlocked() {
    return this.status == Status.BLOCKED;
  }

  /**
   * 작성자 확인
   *
   * @param userId 확인할 사용자 ID
   * @return 작성자 여부
   */
  public boolean isWrittenBy(String userId) {
    if (userId == null) return false;
    return writerId.getValue().equals(userId);
  }

  /**
   * 작성자 확인 (Value Object 사용)
   *
   * @param writer 확인할 작성자 ID
   * @return 작성자 여부
   */
  public boolean isWrittenBy(WriterId writer) {
    return writerId.isSameWriter(writer);
  }

  /** Aggregate 유효성 검증 */
  @Override
  public boolean isValid() {
    // 필수 필드 검증
    if (id == null || title == null || content == null || writerId == null) {
      return false;
    }

    // 상태 검증
    if (status == null) {
      return false;
    }

    // 날짜 검증
    if (createdAt == null || updatedAt == null) {
      return false;
    }

    // 생성일이 수정일보다 늦을 수 없음
    if (createdAt.isAfter(updatedAt)) {
      return false;
    }

    return true;
  }

  /** JPA 라이프사이클 콜백 */
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

  /** equals & hashCode */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArticleAggregate)) return false;
    ArticleAggregate that = (ArticleAggregate) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return String.format(
        "%s{id=%s, title=%s, writer=%s, status=%s, viewCount=%d}",
        getClass().getSimpleName(),
        id,
        title != null ? title.getSummary(50) : null,
        writerId,
        status,
        viewCount);
  }
}

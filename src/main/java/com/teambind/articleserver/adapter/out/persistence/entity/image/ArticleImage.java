package com.teambind.articleserver.adapter.out.persistence.entity.image;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.embeddable_id.ArticleImagesId;
import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글 이미지 엔티티
 *
 * <p>게시글에 첨부된 이미지 정보를 저장
 */
@Entity
@Table(
    name = "article_images",
    indexes = {
      @Index(name = "idx_article_image_article", columnList = "article_id"),
      @Index(name = "idx_article_image_id", columnList = "image_id")
    })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ArticleImage {

  @EmbeddedId private ArticleImagesId id;

  @Column(name = "article_image_url", nullable = false, length = 500)
  private String imageUrl;

  @Column(name = "image_id", nullable = false, length = 100)
  private String imageId;

  /** 이미지가 속한 게시글 */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("articleId")
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  // === 생성자 ===

  /**
   * ArticleImage 생성자
   *
   * <p>imageId와 imageUrl을 쌍으로 관리하여 이미지 정보의 무결성을 보장합니다.
   *
   * @param article 게시글
   * @param sequence 이미지 순서
   * @param imageUrl 이미지 URL
   * @param imageId 이미지 ID
   * @throws IllegalArgumentException 필수 파라미터가 누락되거나 유효하지 않은 경우
   */
  public ArticleImage(Article article, Long sequence, String imageUrl, String imageId) {
    if (article == null || article.getId() == null) {
      throw new IllegalArgumentException("Article must have an ID");
    }
    if (sequence == null || sequence < 1) {
      throw new IllegalArgumentException("Sequence must be greater than 0");
    }
    if (imageUrl == null || imageUrl.isBlank()) {
      throw new IllegalArgumentException("Image URL cannot be null or empty");
    }
    if (imageId == null || imageId.isBlank()) {
      throw new IllegalArgumentException("Image ID cannot be null or empty");
    }

    // imageId와 imageUrl이 쌍으로 제공되었는지 확인
    validateImagePair(imageId, imageUrl);

    this.id = new ArticleImagesId(article.getId(), sequence);
    this.imageUrl = imageUrl;
    this.imageId = imageId;
    this.article = article;
  }

  // === 편의 메서드 ===

  /**
   * 이미지 URL 변경
   *
   * @param newImageUrl 새로운 이미지 URL
   * @deprecated imageId와 imageUrl은 쌍으로 관리되어야 하므로, updateImagePair 사용 권장
   */
  @Deprecated
  public void updateImageUrl(String newImageUrl) {
    if (newImageUrl == null || newImageUrl.isBlank()) {
      throw new IllegalArgumentException("Image URL cannot be null or empty");
    }
    this.imageUrl = newImageUrl;
  }

  /**
   * 이미지 ID 변경
   *
   * @param newImageId 새로운 이미지 ID
   * @deprecated imageId와 imageUrl은 쌍으로 관리되어야 하므로, updateImagePair 사용 권장
   */
  @Deprecated
  public void updateImageId(String newImageId) {
    if (newImageId == null || newImageId.isBlank()) {
      throw new IllegalArgumentException("Image ID cannot be null or empty");
    }
    this.imageId = newImageId;
  }

  /**
   * 이미지 ID와 URL을 쌍으로 업데이트
   *
   * <p>imageId와 imageUrl은 항상 쌍으로 관리되어야 합니다.
   *
   * @param newImageId 새로운 이미지 ID
   * @param newImageUrl 새로운 이미지 URL
   */
  public void updateImagePair(String newImageId, String newImageUrl) {
    if (newImageId == null || newImageId.isBlank()) {
      throw new IllegalArgumentException("Image ID cannot be null or empty");
    }
    if (newImageUrl == null || newImageUrl.isBlank()) {
      throw new IllegalArgumentException("Image URL cannot be null or empty");
    }

    validateImagePair(newImageId, newImageUrl);

    this.imageId = newImageId;
    this.imageUrl = newImageUrl;
  }

  /**
   * 순서 변경
   *
   * @param newSequence 새로운 순서
   */
  public void updateSequence(Long newSequence) {
    if (newSequence == null || newSequence < 1) {
      throw new IllegalArgumentException("Sequence must be greater than 0");
    }
    if (this.id != null) {
      this.id = new ArticleImagesId(this.id.getArticleId(), newSequence);
    }
  }

  /**
   * 게시글 ID 조회
   *
   * @return 게시글 ID
   */
  public String getArticleId() {
    return this.id != null ? this.id.getArticleId() : null;
  }

  /**
   * 순서 조회
   *
   * @return 이미지 순서
   */
  public Long getSequence() {
    return this.id != null ? this.id.getSequenceNum() : null;
  }

  // === Validation Methods ===

  /**
   * imageId와 imageUrl 쌍의 유효성을 검증합니다.
   *
   * <p>이미지 정보의 무결성을 보장하기 위해 ID와 URL이 항상 쌍으로 존재해야 합니다.
   *
   * @param imageId 이미지 ID
   * @param imageUrl 이미지 URL
   * @throws IllegalArgumentException imageId와 imageUrl이 일치하지 않거나 유효하지 않은 경우
   */
  private void validateImagePair(String imageId, String imageUrl) {
    // 기본 null/blank 체크는 이미 수행됨
    // 추가 검증 로직 (예: URL 형식 검증, ID 형식 검증 등)을 여기에 구현할 수 있습니다.

    // URL 기본 형식 검증
    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("/")) {
      throw new IllegalArgumentException("Invalid image URL format: " + imageUrl);
    }

    // imageId 형식 검증 (예: UUID 형식 등)
    if (imageId.length() < 3) {
      throw new IllegalArgumentException("Image ID is too short: " + imageId);
    }
  }

  // === equals & hashCode ===

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArticleImage)) return false;
    ArticleImage that = (ArticleImage) o;
    return id != null && id.equals(that.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "ArticleImage{"
        + "articleId='"
        + getArticleId()
        + '\''
        + ", sequence="
        + getSequence()
        + ", imageId='"
        + imageId
        + '\''
        + ", imageUrl='"
        + imageUrl
        + '\''
        + '}';
  }
}

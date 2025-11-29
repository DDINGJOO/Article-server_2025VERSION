package com.teambind.articleserver.adapter.in.web.dto.response.common;

import com.teambind.articleserver.adapter.out.persistence.entity.image.ArticleImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이미지 정보 DTO
 *
 * <p>게시글 응답에 포함되는 이미지 정보를 나타냅니다.
 * <p>imageId와 imageUrl은 항상 쌍으로 관리되며, 이미지의 고유 식별자와 접근 URL을 제공합니다.
 *
 * <p>응답 예시:
 * <pre>
 * {
 *   "imageId": "img_12345",
 *   "imageUrl": "https://example.com/images/img_12345.jpg",
 *   "sequence": 1
 * }
 * </pre>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageInfo {

  /** 이미지 고유 식별자 */
  private String imageId;

  /** 이미지 접근 URL */
  private String imageUrl;

  /** 이미지 순서 (1부터 시작) */
  private Long sequence;

  /**
   * ArticleImage 엔티티로부터 ImageInfo 생성
   *
   * <p>imageId와 imageUrl이 모두 존재하는 유효한 이미지 정보만 변환합니다.
   *
   * @param articleImage 이미지 엔티티
   * @return ImageInfo (유효하지 않은 경우 null)
   */
  public static ImageInfo fromEntity(ArticleImage articleImage) {
    if (articleImage == null) {
      return null;
    }

    // imageId와 imageUrl이 쌍으로 존재하는지 확인
    String imageId = articleImage.getImageId();
    String imageUrl = articleImage.getImageUrl();

    if (imageId == null || imageId.trim().isEmpty() ||
        imageUrl == null || imageUrl.trim().isEmpty()) {
      // 로그를 남기거나 예외를 던질 수 있지만, 여기서는 null 반환
      return null;
    }

    return ImageInfo.builder()
        .imageId(imageId)
        .imageUrl(imageUrl)
        .sequence(articleImage.getSequence())
        .build();
  }
}

package com.teambind.articleserver.dto.response.common;

import com.teambind.articleserver.entity.image.ArticleImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이미지 정보 DTO
 *
 * <p>게시글 응답에 포함되는 이미지 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageInfo {

  private String imageId;
  private String imageUrl;
  private Long sequence;

  /**
   * ArticleImage 엔티티로부터 ImageInfo 생성
   *
   * @param articleImage 이미지 엔티티
   * @return ImageInfo
   */
  public static ImageInfo fromEntity(ArticleImage articleImage) {
    if (articleImage == null) {
      return null;
    }

    return ImageInfo.builder()
        .imageId(articleImage.getImageId())
        .imageUrl(articleImage.getImageUrl())
        .sequence(articleImage.getSequence())
        .build();
  }
}

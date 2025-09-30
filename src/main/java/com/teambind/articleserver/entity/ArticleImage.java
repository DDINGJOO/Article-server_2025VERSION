package com.teambind.articleserver.entity;

import com.teambind.articleserver.entity.embeddable_id.ArticleImagesId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "article_images")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ArticleImage {
  @Id @EmbeddedId private ArticleImagesId id;

  @Column(name = "article_image_url", nullable = false)
  private String imageUrl;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("articleId")
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  ArticleImage(Article article, Long index, String imageUrl) {
    this.id = new ArticleImagesId(article.getId(), index);
    this.imageUrl = imageUrl;
    this.article = article;
  }
}

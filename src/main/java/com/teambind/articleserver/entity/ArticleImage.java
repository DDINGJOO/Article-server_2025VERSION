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
	@Id
	@EmbeddedId
	private ArticleImagesId id;
	
	@Column(name = "article_image_url", nullable = false)
	private String imageUrl;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "article_id", nullable = false)
	private Article article;
	
}

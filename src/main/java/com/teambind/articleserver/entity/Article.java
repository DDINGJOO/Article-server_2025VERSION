package com.teambind.articleserver.entity;
import com.teambind.articleserver.entity.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "articles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Article {
	@Id
	@Column(name = "article_id", nullable = false)
	private String id;
	
	@Column(name = "title", nullable = false)
	private String title;
	
	@Column(name = "contents", nullable = false)
	private String content;
	
	@Column(name = "writer_id", nullable = false)
	private String writerId;
	@Version
	private Long version;
	
	@Column(name = "status", nullable = false)
	private Status status;
	
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "board_id", nullable = false)
	private Board board;
	
	@OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ArticleImage> images = new ArrayList<>();
	
	@OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<KeywordMappingTable> keywords = new ArrayList<>();
	
	public void addKeyword(String keyword) {
		long index = 0L;
		if (this.keywords == null) {
			this.keywords = new ArrayList<KeywordMappingTable>();
		} else {
			index = (long) keywords.size() + 1;
		}
		keywords.add(
				new KeywordMappingTable(this, Keyword.builder().id(index).keyword(keyword).build())
		);
	}
	
	public void removeKeywords() {
		keywords.forEach(km -> km.setArticle(null));
		keywords.clear();
	}
	
	public void addImage(String keyword) {
		long index = 0L;
		if (this.images == null) {
			this.images = new ArrayList<ArticleImage>();
		} else {
			index = (long) images.size() + 1;
		}
		images.add(
				new ArticleImage(this, index, keyword)
		);
	}
	
	public void removeImages() {
		images.forEach(im -> im.setArticle(null));
		images.clear();
	}
}

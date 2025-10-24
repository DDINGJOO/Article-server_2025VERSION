package com.teambind.articleserver.entity;
import static com.teambind.articleserver.utils.DataInitializer.keywordMap;

import com.teambind.articleserver.entity.enums.Status;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "articles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "article_type", discriminatorType = DiscriminatorType.STRING)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Slf4j
public abstract class Article {
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

  @Column(name = "first_image_url", nullable = false)
  private String firstImageUrl;

	@OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ArticleImage> images = new ArrayList<>();

	@OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<KeywordMappingTable> keywords = new ArrayList<>();


	// 관계 편의성 메서드
	public void addKeyword(String keyword) {
		long index = keywordMap.size() + 1;
		if (this.keywords == null) {
			this.keywords = new ArrayList<KeywordMappingTable>();
		}
		keywords.add(
				new KeywordMappingTable(this, Keyword.builder().id(index).keyword(keyword).build())
		);
	}


	//TODO : // TEST PLZ!!
	public void addKeywords(List<Keyword> keywords) {
		if (this.keywords == null) {
			this.keywords = new ArrayList<KeywordMappingTable>();
		}
    keywords.forEach(
        kw -> {
          this.keywords.add(new KeywordMappingTable(this, kw));
        });
    this.getKeywords().forEach(km -> km.setArticle(this));
    log.info("addKeywords keyword size : {}", this.getKeywords().size());
    this.getKeywords()
        .forEach(
            keyword -> {
              log.info("addKeywords keyword : {}", keyword.getKeyword().getKeyword());
            });
	}

	public void removeKeywords() {
		keywords.forEach(km -> km.setArticle(null));
		keywords.clear();
	}

	public void addImage(String imageUrl) {
		long index = 0L;
		if (this.images == null) {
			this.images = new ArrayList<ArticleImage>();
		} else {
			index = (long) images.size() + 1;
		}
		images.add(
				new ArticleImage(this, index, imageUrl)
		);
	}

  public void addImage(String imageId, String imageUrl) {
    long index = 0L;
    if (this.images == null) {
      this.images = new ArrayList<ArticleImage>();
    } else {
      index = (long) images.size() + 1;
    }
    images.add(new ArticleImage(this, index, imageUrl, imageId));
    // firstImageUrl은 KafkaConsumer에서 명시적으로 설정하므로 여기서는 설정하지 않음
  }

	public void removeImages() {
		images.forEach(im -> im.setArticle(null));
		images.clear();
    firstImageUrl = null;
	}
}

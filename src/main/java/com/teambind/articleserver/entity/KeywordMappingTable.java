package com.teambind.articleserver.entity;

import com.teambind.articleserver.entity.embeddable_id.KeywordMappingTableId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "keyword_mapping_table")
@NoArgsConstructor
@Getter
@Setter
public class KeywordMappingTable {
	@EmbeddedId
	private KeywordMappingTableId id;
	
	KeywordMappingTable(Article article, Keyword keyword) {
		this.id = new KeywordMappingTableId(keyword.getId(), article.getId());
		this.article = article;
		this.keyword = keyword;
	}
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("articleId")
	@JoinColumn(name = "article_id", insertable = false, updatable = false)
	private Article article;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("keywordId")
	@JoinColumn(name = "keyword_id", insertable = false, updatable = false)
	private Keyword keyword;
}

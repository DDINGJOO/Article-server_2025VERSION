package com.teambind.articleserver.entity;

import com.teambind.articleserver.entity.embeddable_id.KeywordMappingTableId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "keyword_mapping_table")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class KeywordMappingTable {
	@EmbeddedId
	private KeywordMappingTableId id;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "article_id", insertable = false, updatable = false)
	private Article article;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "keyword_id", insertable = false, updatable = false)
	private keywords keyword;
}

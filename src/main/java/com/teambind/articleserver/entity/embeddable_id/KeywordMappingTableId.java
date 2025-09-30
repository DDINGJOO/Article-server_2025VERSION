package com.teambind.articleserver.entity.embeddable_id;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class KeywordMappingTableId implements Serializable {
	@Column(name = "keyword_id")
	private int keywordId;
	@Column(name = "article_id")
	private String articleId;
	
	public KeywordMappingTableId(int keywordId, String articleId) {
		this.keywordId = keywordId;
		this.articleId = articleId;
	}
}

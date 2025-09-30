package com.teambind.articleserver.entity.embeddable_id;


import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class KeywordMappingTableId implements Serializable {
	private int keywordId;
	private String articleId;
	
	public KeywordMappingTableId(int keywordId, String articleId) {
		this.keywordId = keywordId;
		this.articleId = articleId;
	}
}

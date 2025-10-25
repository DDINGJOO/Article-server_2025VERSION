package com.teambind.articleserver.entity.embeddable_id;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@Setter
@Getter
public class KeywordMappingTableId implements Serializable {
	@Column(name = "keyword_id")
	private Long keywordId;
	@Column(name = "article_id")
	private String articleId;
	
	public KeywordMappingTableId(Long keywordId, String articleId) {
		this.keywordId = keywordId;
		this.articleId = articleId;
	}
}

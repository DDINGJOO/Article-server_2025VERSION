package com.teambind.articleserver.entity.embeddable_id;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class ArticleImagesId implements Serializable {
	
	@Column(name = "article_id", nullable = false)
	private String articleId;
	@Column(name = "sequence_no", nullable = false)
	private int sequenceNum;
	
	public ArticleImagesId(String articleId, int sequenceNum) {
		this.articleId = articleId;
		this.sequenceNum = sequenceNum;
	}
}

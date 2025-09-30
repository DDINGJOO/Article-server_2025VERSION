package com.teambind.articleserver.entity.embeddable_id;


import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class ArticleImagesId implements Serializable {
	
	private String articleId;
	private int sequenceNum;
	
	public ArticleImagesId(String articleId, int sequenceNum) {
		this.articleId = articleId;
		this.sequenceNum = sequenceNum;
	}
}

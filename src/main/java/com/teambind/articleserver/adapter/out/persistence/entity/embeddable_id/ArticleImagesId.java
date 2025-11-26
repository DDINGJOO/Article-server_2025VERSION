package com.teambind.articleserver.adapter.out.persistence.entity.embeddable_id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class ArticleImagesId implements Serializable {

  @Column(name = "article_id", nullable = false)
  private String articleId;

  @Column(name = "sequence_no", nullable = false)
  private Long sequenceNum;

  public ArticleImagesId(String articleId, Long sequenceNum) {
    this.articleId = articleId;
    this.sequenceNum = sequenceNum;
  }
}

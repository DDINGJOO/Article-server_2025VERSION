package com.teambind.articleserver.event.events;

import com.teambind.articleserver.entity.Article;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleCreatedEvent {
  private String articleId;
  private String writerId;
  private Long version;
  private String title;

  public static ArticleCreatedEvent from(Article article) {
    return ArticleCreatedEvent.builder()
        .articleId(article.getId())
        .writerId(article.getWriterId())
        .version(article.getVersion())
        .title(article.getTitle())
        .build();
  }
}

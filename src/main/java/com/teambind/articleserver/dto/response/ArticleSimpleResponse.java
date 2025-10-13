package com.teambind.articleserver.dto.response;

import com.teambind.articleserver.entity.Article;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleSimpleResponse {
  private String articleId;
  private String title;
  private String writerId;
  private Long version;
  private LocalDateTime createdAt;

  public static ArticleSimpleResponse from(Article article) {
    return ArticleSimpleResponse.builder()
        .articleId(article.getId())
        .title(article.getTitle())
        .writerId(article.getWriterId())
        .version(article.getVersion())
        .createdAt(article.getCreatedAt())
        .build();
  }
}

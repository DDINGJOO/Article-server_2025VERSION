package com.teambind.articleserver.dto.response;

import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.ArticleImage;
import com.teambind.articleserver.entity.Board;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {
	private String articleId;
	private String title;
	private String content;
	private String writerId;
	
	private Board board;
	
	private LocalDateTime LastestUpdateId;
	private List<String> imageUrls;
  private Map<Long, String> keywords;

  public static ArticleResponse fromEntity(Article article) {

    return ArticleResponse.builder()
        .articleId(article.getId())
        .title(article.getTitle())
        .content(article.getContent())
        .writerId(article.getWriterId())
        .board(article.getBoard())
        .LastestUpdateId(article.getUpdatedAt())
        .imageUrls(article.getImages().stream().map(ArticleImage::getImageUrl).toList())
        .keywords(
            article.getKeywords().stream()
                .collect(
                    Collectors.toMap(
                        keyword -> keyword.getId().getKeywordId(),
                        keyword -> keyword.getKeyword().getKeyword())))
        .build();
  }
}

package com.teambind.articleserver.dto.response;

import com.teambind.articleserver.entity.ArticleImage;
import com.teambind.articleserver.entity.EventArticle;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventArticleResponse {
  private String articleId;
  private String title;
  private String content;
  private String writerId;

  private Map<Long, String> board;

  private LocalDateTime LastestUpdateId;
  private Map<String, String> imageUrls;
  private Map<Long, String> keywords;

  private LocalDateTime eventStartDate;
  private LocalDateTime eventEndDate;

  public static EventArticleResponse fromEntity(EventArticle article) {
    if (article == null) {
      throw new CustomException(ErrorCode.ARTICLE_IS_NULL);
    }
    Map<String, String> imageUrls = null;
    if (article.getImages() != null) {
      imageUrls =
          article.getImages().stream()
              .collect(Collectors.toMap(ArticleImage::getImageId, ArticleImage::getImageUrl));
    }
    Map<Long, String> keywords = null;
    if (article.getKeywords() != null) {
      keywords =
          article.getKeywords().stream()
              .collect(
                  Collectors.toMap(
                      keyword -> keyword.getId().getKeywordId(),
                      keyword -> keyword.getKeyword().getKeyword()));
    }
    Map<Long, String> board = new HashMap<>();
    if (article.getBoard() != null) {
      board.put(article.getBoard().getId(), article.getBoard().getBoardName());
    }
    return EventArticleResponse.builder()
        .articleId(article.getId())
        .title(article.getTitle())
        .content(article.getContent())
        .writerId(article.getWriterId())
        .board(board)
        .LastestUpdateId(article.getUpdatedAt())
        .imageUrls(imageUrls)
        .keywords(keywords)
        .eventStartDate(article.getEventStartDate())
        .eventEndDate(article.getEventEndDate())
        .build();
  }
}

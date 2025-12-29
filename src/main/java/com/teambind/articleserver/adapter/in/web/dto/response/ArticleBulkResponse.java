package com.teambind.articleserver.adapter.in.web.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bulk 조회 전용 응답 DTO
 *
 * <p>다중 게시글 일괄 조회 시 사용되는 응답 DTO입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleBulkResponse {

  private String articleId;

  private String title;

  private String content;

  private String writerId;

  private Long boardId;

  private String boardName;

  private String articleType;

  private String status;

  private Long viewCount;

  private String firstImageUrl;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}

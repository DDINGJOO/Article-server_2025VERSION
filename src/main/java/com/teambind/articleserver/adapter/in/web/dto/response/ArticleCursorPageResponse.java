package com.teambind.articleserver.adapter.in.web.dto.response;

import com.teambind.articleserver.adapter.in.web.dto.response.article.ArticleBaseResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleCursorPageResponse {
  private List<ArticleBaseResponse> items;
  // 다음 페이지 요청 시 사용할 수정일시 (updated_at)
  private LocalDateTime nextCursorUpdatedAt;
  private String nextCursorId;
  private boolean hasNext;
  private int size;
}

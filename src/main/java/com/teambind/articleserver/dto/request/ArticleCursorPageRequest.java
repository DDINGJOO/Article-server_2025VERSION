package com.teambind.articleserver.dto.request;

import java.time.LocalDateTime;
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
public class ArticleCursorPageRequest {
  // 페이지 크기 (기본값은 컨트롤러에서 설정)
  private Integer size;
  // 마지막으로 본 항목의 수정일시 (updated_at)
  private LocalDateTime cursorUpdatedAt;
  // tie-breaker
  private String cursorId;
}

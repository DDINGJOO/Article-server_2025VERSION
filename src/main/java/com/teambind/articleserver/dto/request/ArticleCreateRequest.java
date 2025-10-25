package com.teambind.articleserver.dto.request;

import com.teambind.articleserver.validation.ValidBoardId;
import com.teambind.articleserver.validation.ValidEventPeriod;
import com.teambind.articleserver.validation.ValidKeywordIds;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * 게시글 생성/수정 요청 DTO
 *
 * <p>Validation: - title, content, writerId: 필수 (생성 시) - boardIds: 존재하는 Board ID여야 함 - keywordIds:
 * 존재하는 Keyword ID 리스트여야 함 - eventStartDate, eventEndDate: 시작일이 종료일보다 이전이어야 함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidEventPeriod // 클래스 레벨 검증
public class ArticleCreateRequest {

  @NotBlank(message = "제목은 필수입니다")
  private String title;

  @NotBlank(message = "내용은 필수입니다")
  private String content;

  @NotBlank(message = "작성자 ID는 필수입니다")
  private String writerId;

  // 이벤트 기간 (이벤트 게시글만 해당)
  private LocalDateTime eventStartDate;
  private LocalDateTime eventEndDate;

  // 키워드 ID 리스트 (선택)
  @ValidKeywordIds private List<Long> keywordIds;

  // Board ID (필수)
  @NotNull(message = "Board ID는 필수입니다")
  @ValidBoardId(nullable = false)
  private Long boardIds;
}

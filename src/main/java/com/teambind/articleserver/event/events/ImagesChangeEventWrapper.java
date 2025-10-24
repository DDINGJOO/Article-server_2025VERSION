package com.teambind.articleserver.event.events;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 다중 이미지 변경 이벤트 Wrapper
 *
 * <p>이미지 서버의 ImagesChangeEventWrapper와 동일한 구조
 *
 * <p>사용 예시: - images가 비어있으면: 전체 삭제 - images가 있으면: 전체 교체 (기존 삭제 후 새로 추가)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImagesChangeEventWrapper {
  /** 참조 ID (게시글 ID) 빈 배열일 때 삭제 대상 식별용 */
  private String referenceId;

  /** 이미지 변경 이벤트 리스트 - 빈 배열: 전체 삭제 - 값 있음: 전체 교체 (sequence 순서대로 정렬됨) */
  private List<ImageChangeEvent> images;
}

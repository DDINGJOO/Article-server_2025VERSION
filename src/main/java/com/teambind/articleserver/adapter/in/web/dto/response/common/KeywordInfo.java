package com.teambind.articleserver.adapter.in.web.dto.response.common;

import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

/**
 * 키워드 정보 DTO
 *
 * <p>게시글 응답에 포함되는 키워드 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordInfo {

  private Long keywordId;
  private String keywordName;
  private Boolean isCommon; // true: 공통 키워드, false: 보드 전용 키워드
  private Long boardId; // 보드 전용 키워드인 경우 보드 ID (공통 키워드면 null)
  private String boardName; // 보드 전용 키워드인 경우 보드 이름 (공통 키워드면 null)

  /**
   * Keyword 엔티티로부터 KeywordInfo 생성
   *
   * @param keyword 키워드 엔티티
   * @return KeywordInfo
   */
  public static KeywordInfo fromEntity(Keyword keyword) {
    if (keyword == null) {
      return null;
    }

    // Lazy 로딩된 board 프록시 안전하게 처리
    Long boardId = null;
    String boardName = null;

    Board board = keyword.getBoard();
    if (board != null && Hibernate.isInitialized(board)) {
      boardId = board.getId();
      boardName = board.getName();
    }

    return KeywordInfo.builder()
        .keywordId(keyword.getId())
        .keywordName(keyword.getName())
        .isCommon(keyword.isCommonKeyword())
        .boardId(boardId)
        .boardName(boardName)
        .build();
  }
}

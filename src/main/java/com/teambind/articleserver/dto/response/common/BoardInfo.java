package com.teambind.articleserver.dto.response.common;

import com.teambind.articleserver.entity.board.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시판 정보 DTO
 *
 * <p>게시글 응답에 포함되는 게시판 기본 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardInfo {

  private Long boardId;
  private String boardName;
  private String description;

  /**
   * Board 엔티티로부터 BoardInfo 생성
   *
   * @param board 게시판 엔티티
   * @return BoardInfo
   */
  public static BoardInfo fromEntity(Board board) {
    if (board == null) {
      return null;
    }

    return BoardInfo.builder()
        .boardId(board.getId())
        .boardName(board.getName())
        .description(board.getDescription())
        .build();
  }
}

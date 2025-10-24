package com.teambind.articleserver.fixture;

import com.teambind.articleserver.entity.board.Board;
import java.time.LocalDateTime;

/** Board 엔티티 테스트 픽스처 팩토리 */
public class BoardFixture {

  /** 기본 Board 생성 */
  public static Board createBoard() {
    return Board.builder()
        .id(1L)
        .name("자유게시판")
        .description("자유롭게 소통하는 공간입니다")
        .isActive(true)
        .displayOrder(1)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** ID가 있는 Board 생성 */
  public static Board createBoardWithId(Long id) {
    return Board.builder()
        .id(id)
        .name("게시판 " + id)
        .description("게시판 " + id + " 설명")
        .isActive(true)
        .displayOrder(id.intValue())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** 커스텀 이름을 가진 Board 생성 */
  public static Board createBoardWithName(String name) {
    return Board.builder()
        .id(1L)
        .name(name)
        .description(name + " 설명")
        .isActive(true)
        .displayOrder(1)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** 비활성화된 Board 생성 */
  public static Board createInactiveBoard() {
    return Board.builder()
        .id(1L)
        .name("비활성화 게시판")
        .description("비활성화된 게시판입니다")
        .isActive(false)
        .displayOrder(1)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** ID 없는 Board 생성 (persist 전) */
  public static Board createBoardWithoutId() {
    return Board.builder()
        .name("새 게시판")
        .description("새로운 게시판입니다")
        .isActive(true)
        .displayOrder(1)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }
}

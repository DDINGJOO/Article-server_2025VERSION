package com.teambind.articleserver.fixture;

import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.keyword.Keyword;

/** Keyword 엔티티 테스트 픽스처 팩토리 */
public class KeywordFixture {

  /** 기본 공통 키워드 생성 (board = null) */
  public static Keyword createCommonKeyword() {
    return Keyword.builder().id(1L).name("공통키워드").board(null).usageCount(0).isActive(true).build();
  }

  /** ID가 있는 공통 키워드 생성 */
  public static Keyword createCommonKeywordWithId(Long id) {
    return Keyword.builder()
        .id(id)
        .name("키워드 " + id)
        .board(null)
        .usageCount(0)
        .isActive(true)
        .build();
  }

  /** 커스텀 이름을 가진 공통 키워드 생성 */
  public static Keyword createCommonKeywordWithName(String name) {
    return Keyword.builder().id(1L).name(name).board(null).usageCount(0).isActive(true).build();
  }

  /** Board 전용 키워드 생성 */
  public static Keyword createBoardKeyword(Board board) {
    return Keyword.builder()
        .id(2L)
        .name("보드전용키워드")
        .board(board)
        .usageCount(0)
        .isActive(true)
        .build();
  }

  /** Board 전용 키워드 생성 (ID와 이름 지정) */
  public static Keyword createBoardKeywordWithIdAndName(Long id, String name, Board board) {
    return Keyword.builder().id(id).name(name).board(board).usageCount(0).isActive(true).build();
  }

  /** 사용 빈도가 높은 키워드 생성 */
  public static Keyword createHighUsageKeyword(int usageCount) {
    return Keyword.builder()
        .id(1L)
        .name("인기키워드")
        .board(null)
        .usageCount(usageCount)
        .isActive(true)
        .build();
  }

  /** 비활성화된 키워드 생성 */
  public static Keyword createInactiveKeyword() {
    return Keyword.builder()
        .id(1L)
        .name("비활성키워드")
        .board(null)
        .usageCount(0)
        .isActive(false)
        .build();
  }

  /** ID 없는 키워드 생성 (persist 전) */
  public static Keyword createKeywordWithoutId() {
    return Keyword.builder().name("새키워드").board(null).usageCount(0).isActive(true).build();
  }
}

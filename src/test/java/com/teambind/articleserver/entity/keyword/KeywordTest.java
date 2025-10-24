package com.teambind.articleserver.entity.keyword;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.fixture.BoardFixture;
import com.teambind.articleserver.fixture.KeywordFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Keyword 엔티티 테스트")
class KeywordTest {

  @Nested
  @DisplayName("키워드 생성 테스트")
  class CreateKeywordTest {

    @Test
    @DisplayName("정상: 공통 키워드를 생성할 수 있다")
    void createCommonKeyword_Success() {
      // given & when
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // then
      assertThat(keyword).isNotNull();
      assertThat(keyword.getId()).isEqualTo(1L);
      assertThat(keyword.getName()).isEqualTo("공통키워드");
      assertThat(keyword.getBoard()).isNull();
      assertThat(keyword.getUsageCount()).isZero();
      assertThat(keyword.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("정상: 게시판 전용 키워드를 생성할 수 있다")
    void createBoardKeyword_Success() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      Keyword keyword = KeywordFixture.createBoardKeyword(board);

      // then
      assertThat(keyword.getBoard()).isNotNull();
      assertThat(keyword.getBoard()).isEqualTo(board);
      assertThat(keyword.isCommonKeyword()).isFalse();
    }

    @Test
    @DisplayName("정상: 이름과 Board로 키워드를 생성할 수 있다")
    void createKeyword_WithConstructor_Success() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      Keyword keyword = new Keyword("테스트키워드", board);

      // then
      assertThat(keyword.getName()).isEqualTo("테스트키워드");
      assertThat(keyword.getBoard()).isEqualTo(board);
      assertThat(keyword.getUsageCount()).isZero();
      assertThat(keyword.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("정상: 기본값이 올바르게 설정된다")
    void createKeyword_WithDefaultValues_Success() {
      // given & when
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // then
      assertThat(keyword.getUsageCount()).isZero();
      assertThat(keyword.getIsActive()).isTrue();
      assertThat(keyword.getMappings()).isEmpty();
    }
  }

  @Nested
  @DisplayName("공통 키워드 vs 게시판 전용 키워드 테스트")
  class KeywordTypeTest {

    @Test
    @DisplayName("정상: board가 null이면 공통 키워드이다")
    void isCommonKeyword_WhenBoardIsNull_ReturnsTrue() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when & then
      assertThat(keyword.isCommonKeyword()).isTrue();
    }

    @Test
    @DisplayName("정상: board가 있으면 공통 키워드가 아니다")
    void isCommonKeyword_WhenBoardExists_ReturnsFalse() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword = KeywordFixture.createBoardKeyword(board);

      // when & then
      assertThat(keyword.isCommonKeyword()).isFalse();
    }

    @Test
    @DisplayName("정상: 특정 게시판의 키워드인지 확인할 수 있다")
    void belongsToBoard_WithCorrectBoard_ReturnsTrue() {
      // given
      Board board = BoardFixture.createBoardWithId(1L);
      Keyword keyword = KeywordFixture.createBoardKeyword(board);

      // when & then
      assertThat(keyword.belongsToBoard(board)).isTrue();
    }

    @Test
    @DisplayName("정상: 다른 게시판의 키워드가 아니다")
    void belongsToBoard_WithDifferentBoard_ReturnsFalse() {
      // given
      Board board1 = BoardFixture.createBoardWithId(1L);
      Board board2 = BoardFixture.createBoardWithId(2L);
      Keyword keyword = KeywordFixture.createBoardKeyword(board1);

      // when & then
      assertThat(keyword.belongsToBoard(board2)).isFalse();
    }

    @Test
    @DisplayName("정상: 공통 키워드는 어떤 게시판에도 속하지 않는다")
    void belongsToBoard_ForCommonKeyword_ReturnsFalse() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();
      Board board = BoardFixture.createBoard();

      // when & then
      assertThat(keyword.belongsToBoard(board)).isFalse();
    }
  }

  @Nested
  @DisplayName("사용 빈도 관리 테스트")
  class UsageCountTest {

    @Test
    @DisplayName("정상: 사용 빈도를 증가시킬 수 있다")
    void incrementUsageCount_Success() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      keyword.incrementUsageCount();
      keyword.incrementUsageCount();

      // then
      assertThat(keyword.getUsageCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("엣지: 사용 빈도가 null인 경우 0으로 초기화 후 증가")
    void incrementUsageCount_WhenNull_InitializesToZero() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();
      // Reflection으로 usageCount를 null로 설정
      try {
        java.lang.reflect.Field field = Keyword.class.getDeclaredField("usageCount");
        field.setAccessible(true);
        field.set(keyword, null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      // when
      keyword.incrementUsageCount();

      // then
      assertThat(keyword.getUsageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("정상: 사용 빈도를 감소시킬 수 있다")
    void decrementUsageCount_Success() {
      // given
      Keyword keyword = KeywordFixture.createHighUsageKeyword(5);

      // when
      keyword.decrementUsageCount();
      keyword.decrementUsageCount();

      // then
      assertThat(keyword.getUsageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("엣지: 사용 빈도가 0일 때 감소 시도 시 0 유지")
    void decrementUsageCount_WhenZero_StaysZero() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      keyword.decrementUsageCount();

      // then
      assertThat(keyword.getUsageCount()).isZero();
    }

    @Test
    @DisplayName("엣지: 사용 빈도가 null인 경우 감소 시도 시 0으로 초기화")
    void decrementUsageCount_WhenNull_InitializesToZero() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();
      try {
        java.lang.reflect.Field field = Keyword.class.getDeclaredField("usageCount");
        field.setAccessible(true);
        field.set(keyword, null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      // when
      keyword.decrementUsageCount();

      // then
      assertThat(keyword.getUsageCount()).isZero();
    }

    @Test
    @DisplayName("정상: 높은 사용 빈도에서도 정상 동작한다")
    void incrementUsageCount_WithHighCount_Success() {
      // given
      Keyword keyword = KeywordFixture.createHighUsageKeyword(99999);

      // when
      keyword.incrementUsageCount();

      // then
      assertThat(keyword.getUsageCount()).isEqualTo(100000);
    }
  }

  @Nested
  @DisplayName("활성화/비활성화 테스트")
  class ActivationTest {

    @Test
    @DisplayName("정상: 키워드를 비활성화할 수 있다")
    void deactivateKeyword_Success() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      keyword.deactivate();

      // then
      assertThat(keyword.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("정상: 비활성화된 키워드를 활성화할 수 있다")
    void activateKeyword_Success() {
      // given
      Keyword keyword = KeywordFixture.createInactiveKeyword();

      // when
      keyword.activate();

      // then
      assertThat(keyword.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("엣지: 이미 활성화된 키워드를 다시 활성화해도 문제없다")
    void activateActiveKeyword_Success() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      keyword.activate();

      // then
      assertThat(keyword.getIsActive()).isTrue();
    }
  }

  @Nested
  @DisplayName("Board 연관관계 테스트")
  class BoardRelationTest {

    @Test
    @DisplayName("정상: Board에 키워드를 할당할 수 있다")
    void assignToBoard_Success() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();
      Board board = BoardFixture.createBoard();

      // when
      keyword.assignToBoard(board);

      // then
      assertThat(keyword.getBoard()).isEqualTo(board);
      assertThat(board.getKeywords()).contains(keyword);
    }

    @Test
    @DisplayName("정상: Board에서 키워드를 분리할 수 있다")
    void detachFromBoard_Success() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword = KeywordFixture.createBoardKeyword(board);
      board.addKeyword(keyword);

      // when
      keyword.detachFromBoard();

      // then
      assertThat(keyword.getBoard()).isNull();
      assertThat(board.getKeywords()).doesNotContain(keyword);
    }

    @Test
    @DisplayName("정상: Board 변경 시 이전 Board에서 제거되고 새 Board에 추가된다")
    void assignToBoard_RemovesFromOldBoard() {
      // given
      Board oldBoard = BoardFixture.createBoardWithId(1L);
      Board newBoard = BoardFixture.createBoardWithId(2L);
      Keyword keyword = KeywordFixture.createBoardKeyword(oldBoard);
      oldBoard.addKeyword(keyword);

      // when
      keyword.assignToBoard(newBoard);

      // then
      assertThat(keyword.getBoard()).isEqualTo(newBoard);
      assertThat(oldBoard.getKeywords()).doesNotContain(keyword);
      assertThat(newBoard.getKeywords()).contains(keyword);
    }

    @Test
    @DisplayName("엣지: Board가 null인 상태에서 detach 호출 시 아무 일도 일어나지 않는다")
    void detachFromBoard_WhenBoardIsNull_DoesNothing() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when & then
      assertThatCode(() -> keyword.detachFromBoard()).doesNotThrowAnyException();
      assertThat(keyword.getBoard()).isNull();
    }
  }

  @Nested
  @DisplayName("매핑 관리 테스트")
  class MappingManagementTest {

    @Test
    @DisplayName("정상: 모든 매핑을 제거할 수 있다")
    void clearMappings_Success() {
      // given
      Keyword keyword = KeywordFixture.createHighUsageKeyword(5);

      // when
      keyword.clearMappings();

      // then
      assertThat(keyword.getMappings()).isEmpty();
      assertThat(keyword.getUsageCount()).isZero();
    }

    @Test
    @DisplayName("엣지: 매핑이 없는 상태에서 clear 호출 시 문제없다")
    void clearMappings_WhenEmpty_DoesNothing() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when & then
      assertThatCode(() -> keyword.clearMappings()).doesNotThrowAnyException();
      assertThat(keyword.getMappings()).isEmpty();
    }
  }

  @Nested
  @DisplayName("equals & hashCode 테스트")
  class EqualsAndHashCodeTest {

    @Test
    @DisplayName("정상: 같은 ID를 가진 Keyword는 동등하다")
    void equals_SameId_ReturnsTrue() {
      // given
      Keyword keyword1 = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword keyword2 = KeywordFixture.createCommonKeywordWithId(1L);

      // when & then
      assertThat(keyword1).isEqualTo(keyword2);
      assertThat(keyword1.hashCode()).isEqualTo(keyword2.hashCode());
    }

    @Test
    @DisplayName("정상: 다른 ID를 가진 Keyword는 동등하지 않다")
    void equals_DifferentId_ReturnsFalse() {
      // given
      Keyword keyword1 = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword keyword2 = KeywordFixture.createCommonKeywordWithId(2L);

      // when & then
      assertThat(keyword1).isNotEqualTo(keyword2);
    }

    @Test
    @DisplayName("정상: 자기 자신과는 동등하다")
    void equals_Self_ReturnsTrue() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when & then
      assertThat(keyword).isEqualTo(keyword);
    }

    @Test
    @DisplayName("정상: null과는 동등하지 않다")
    void equals_Null_ReturnsFalse() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when & then
      assertThat(keyword).isNotEqualTo(null);
    }
  }

  @Nested
  @DisplayName("toString 테스트")
  class ToStringTest {

    @Test
    @DisplayName("정상: 공통 키워드의 toString이 주요 정보를 포함한다")
    void toString_ForCommonKeyword_ContainsMainInfo() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      String result = keyword.toString();

      // then
      assertThat(result).contains("id=1");
      assertThat(result).contains("name='공통키워드'");
      assertThat(result).contains("boardId=null");
      assertThat(result).contains("usageCount=0");
      assertThat(result).contains("isActive=true");
    }

    @Test
    @DisplayName("정상: 게시판 전용 키워드의 toString이 boardId를 포함한다")
    void toString_ForBoardKeyword_ContainsBoardId() {
      // given
      Board board = BoardFixture.createBoardWithId(1L);
      Keyword keyword = KeywordFixture.createBoardKeyword(board);

      // when
      String result = keyword.toString();

      // then
      assertThat(result).contains("boardId=1");
    }
  }
}

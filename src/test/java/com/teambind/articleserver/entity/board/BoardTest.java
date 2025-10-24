package com.teambind.articleserver.entity.board;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.entity.articleType.RegularArticle;
import com.teambind.articleserver.entity.keyword.Keyword;
import com.teambind.articleserver.fixture.ArticleFixture;
import com.teambind.articleserver.fixture.BoardFixture;
import com.teambind.articleserver.fixture.KeywordFixture;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Board 엔티티 테스트")
class BoardTest {

  @Nested
  @DisplayName("게시판 생성 테스트")
  class CreateBoardTest {

    @Test
    @DisplayName("정상: Board를 빌더로 생성할 수 있다")
    void createBoard_Success() {
      // given & when
      Board board = BoardFixture.createBoard();

      // then
      assertThat(board).isNotNull();
      assertThat(board.getId()).isEqualTo(1L);
      assertThat(board.getName()).isEqualTo("자유게시판");
      assertThat(board.getDescription()).isEqualTo("자유롭게 소통하는 공간입니다");
      assertThat(board.getIsActive()).isTrue();
      assertThat(board.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("정상: 이름만으로 Board를 생성할 수 있다")
    void createBoard_WithNameOnly_Success() {
      // given & when
      Board board = new Board("공지사항");

      // then
      assertThat(board.getName()).isEqualTo("공지사항");
      assertThat(board.getIsActive()).isTrue();
      assertThat(board.getArticles()).isEmpty();
      assertThat(board.getKeywords()).isEmpty();
    }

    @Test
    @DisplayName("정상: 이름과 설명으로 Board를 생성할 수 있다")
    void createBoard_WithNameAndDescription_Success() {
      // given & when
      Board board = new Board("공지사항", "중요한 공지사항을 확인하세요");

      // then
      assertThat(board.getName()).isEqualTo("공지사항");
      assertThat(board.getDescription()).isEqualTo("중요한 공지사항을 확인하세요");
      assertThat(board.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("정상: 기본값이 올바르게 설정된다")
    void createBoard_WithDefaultValues_Success() {
      // given & when
      Board board = BoardFixture.createBoard();

      // then
      assertThat(board.getIsActive()).isTrue();
      assertThat(board.getArticles()).isEmpty();
      assertThat(board.getKeywords()).isEmpty();
    }
  }

  @Nested
  @DisplayName("게시판 정보 수정 테스트")
  class UpdateInfoTest {

    @Test
    @DisplayName("정상: 게시판 이름과 설명을 수정할 수 있다")
    void updateInfo_Success() {
      // given
      Board board = BoardFixture.createBoard();
      LocalDateTime beforeUpdate = board.getUpdatedAt();

      // when
      board.updateInfo("새 게시판명", "새 설명");

      // then
      assertThat(board.getName()).isEqualTo("새 게시판명");
      assertThat(board.getDescription()).isEqualTo("새 설명");
      assertThat(board.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("정상: 이름만 수정할 수 있다")
    void updateName_Only_Success() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      board.updateInfo("새 이름", null);

      // then
      assertThat(board.getName()).isEqualTo("새 이름");
      assertThat(board.getDescription()).isNull();
    }

    @Test
    @DisplayName("엣지: 빈 문자열 이름은 무시된다")
    void updateInfo_WithBlankName_IgnoresUpdate() {
      // given
      Board board = BoardFixture.createBoard();
      String originalName = board.getName();

      // when
      board.updateInfo("", "새 설명");

      // then
      assertThat(board.getName()).isEqualTo(originalName);
      assertThat(board.getDescription()).isEqualTo("새 설명");
    }
  }

  @Nested
  @DisplayName("게시판 활성화/비활성화 테스트")
  class ActivationTest {

    @Test
    @DisplayName("정상: 게시판을 비활성화할 수 있다")
    void deactivateBoard_Success() {
      // given
      Board board = BoardFixture.createBoard();
      LocalDateTime beforeUpdate = board.getUpdatedAt();

      // when
      board.deactivate();

      // then
      assertThat(board.getIsActive()).isFalse();
      assertThat(board.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("정상: 비활성화된 게시판을 활성화할 수 있다")
    void activateBoard_Success() {
      // given
      Board board = BoardFixture.createInactiveBoard();

      // when
      board.activate();

      // then
      assertThat(board.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("엣지: 이미 활성화된 게시판을 다시 활성화해도 문제없다")
    void activateActiveBoard_Success() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      board.activate();

      // then
      assertThat(board.getIsActive()).isTrue();
    }
  }

  @Nested
  @DisplayName("표시 순서 변경 테스트")
  class DisplayOrderTest {

    @Test
    @DisplayName("정상: 표시 순서를 변경할 수 있다")
    void updateDisplayOrder_Success() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      board.updateDisplayOrder(5);

      // then
      assertThat(board.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    @DisplayName("엣지: 표시 순서를 0으로 설정할 수 있다")
    void updateDisplayOrder_ToZero_Success() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      board.updateDisplayOrder(0);

      // then
      assertThat(board.getDisplayOrder()).isZero();
    }

    @Test
    @DisplayName("엣지: 표시 순서를 null로 설정할 수 있다")
    void updateDisplayOrder_ToNull_Success() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      board.updateDisplayOrder(null);

      // then
      assertThat(board.getDisplayOrder()).isNull();
    }
  }

  @Nested
  @DisplayName("게시글 연관관계 테스트")
  class ArticleRelationTest {

    @Test
    @DisplayName("정상: 게시글을 추가할 수 있다")
    void addArticle_Success() {
      // given
      Board board = BoardFixture.createBoard();
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      board.addArticle(article);

      // then
      assertThat(board.getArticles()).hasSize(1);
      assertThat(board.getArticles()).contains(article);
      assertThat(article.getBoard()).isEqualTo(board);
    }

    @Test
    @DisplayName("정상: 여러 게시글을 추가할 수 있다")
    void addMultipleArticles_Success() {
      // given
      Board board = BoardFixture.createBoard();
      RegularArticle article1 = ArticleFixture.createRegularArticleWithId("ART_001");
      RegularArticle article2 = ArticleFixture.createRegularArticleWithId("ART_002");

      // when
      board.addArticle(article1);
      board.addArticle(article2);

      // then
      assertThat(board.getArticles()).hasSize(2);
      assertThat(board.getArticleCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("정상: 중복 게시글은 추가되지 않는다")
    void addDuplicateArticle_DoesNotAdd() {
      // given
      Board board = BoardFixture.createBoard();
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      board.addArticle(article);
      board.addArticle(article); // 중복 추가 시도

      // then
      assertThat(board.getArticles()).hasSize(1);
    }

    @Test
    @DisplayName("예외: null 게시글은 추가되지 않는다")
    void addNullArticle_DoesNotAdd() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      board.addArticle(null);

      // then
      assertThat(board.getArticles()).isEmpty();
    }

    @Test
    @DisplayName("정상: 게시글을 제거할 수 있다")
    void removeArticle_Success() {
      // given
      Board board = BoardFixture.createBoard();
      RegularArticle article = ArticleFixture.createRegularArticle();
      board.addArticle(article);

      // when
      board.removeArticle(article);

      // then
      assertThat(board.getArticles()).isEmpty();
      assertThat(article.getBoard()).isNull();
    }

    @Test
    @DisplayName("정상: 모든 게시글을 제거할 수 있다")
    void clearArticles_Success() {
      // given
      Board board = BoardFixture.createBoard();
      RegularArticle article1 = ArticleFixture.createRegularArticleWithId("ART_001");
      RegularArticle article2 = ArticleFixture.createRegularArticleWithId("ART_002");
      board.addArticle(article1);
      board.addArticle(article2);

      // when
      board.clearArticles();

      // then
      assertThat(board.getArticles()).isEmpty();
      assertThat(article1.getBoard()).isNull();
      assertThat(article2.getBoard()).isNull();
    }

    @Test
    @DisplayName("엣지: null 게시글 제거 시 아무 일도 일어나지 않는다")
    void removeNullArticle_DoesNothing() {
      // given
      Board board = BoardFixture.createBoard();
      RegularArticle article = ArticleFixture.createRegularArticle();
      board.addArticle(article);

      // when
      board.removeArticle(null);

      // then
      assertThat(board.getArticles()).hasSize(1);
    }

    @Test
    @DisplayName("정상: 게시글 수를 조회할 수 있다")
    void getArticleCount_Success() {
      // given
      Board board = BoardFixture.createBoard();
      board.addArticle(ArticleFixture.createRegularArticleWithId("ART_001"));
      board.addArticle(ArticleFixture.createRegularArticleWithId("ART_002"));
      board.addArticle(ArticleFixture.createRegularArticleWithId("ART_003"));

      // when
      int count = board.getArticleCount();

      // then
      assertThat(count).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("키워드 연관관계 테스트")
  class KeywordRelationTest {

    @Test
    @DisplayName("정상: 키워드를 추가할 수 있다")
    void addKeyword_Success() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      board.addKeyword(keyword);

      // then
      assertThat(board.getKeywords()).hasSize(1);
      assertThat(board.getKeywords()).contains(keyword);
      assertThat(keyword.getBoard()).isEqualTo(board);
    }

    @Test
    @DisplayName("정상: 여러 키워드를 추가할 수 있다")
    void addMultipleKeywords_Success() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword1 = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword keyword2 = KeywordFixture.createCommonKeywordWithId(2L);

      // when
      board.addKeyword(keyword1);
      board.addKeyword(keyword2);

      // then
      assertThat(board.getKeywords()).hasSize(2);
      assertThat(board.getKeywordCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("정상: 중복 키워드는 추가되지 않는다")
    void addDuplicateKeyword_DoesNotAdd() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      board.addKeyword(keyword);
      board.addKeyword(keyword); // 중복 추가 시도

      // then
      assertThat(board.getKeywords()).hasSize(1);
    }

    @Test
    @DisplayName("예외: null 키워드는 추가되지 않는다")
    void addNullKeyword_DoesNotAdd() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      board.addKeyword(null);

      // then
      assertThat(board.getKeywords()).isEmpty();
    }

    @Test
    @DisplayName("정상: 키워드를 제거할 수 있다")
    void removeKeyword_Success() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      board.addKeyword(keyword);

      // when
      board.removeKeyword(keyword);

      // then
      assertThat(board.getKeywords()).isEmpty();
      assertThat(keyword.getBoard()).isNull();
    }

    @Test
    @DisplayName("정상: 모든 키워드를 제거할 수 있다")
    void clearKeywords_Success() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword1 = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword keyword2 = KeywordFixture.createCommonKeywordWithId(2L);
      board.addKeyword(keyword1);
      board.addKeyword(keyword2);

      // when
      board.clearKeywords();

      // then
      assertThat(board.getKeywords()).isEmpty();
      assertThat(keyword1.getBoard()).isNull();
      assertThat(keyword2.getBoard()).isNull();
    }

    @Test
    @DisplayName("정상: 이름으로 키워드를 찾을 수 있다")
    void findKeywordByName_Success() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword = KeywordFixture.createCommonKeywordWithName("공지");
      board.addKeyword(keyword);

      // when
      Keyword found = board.findKeywordByName("공지");

      // then
      assertThat(found).isNotNull();
      assertThat(found.getName()).isEqualTo("공지");
    }

    @Test
    @DisplayName("엣지: 존재하지 않는 키워드 이름으로 검색 시 null 반환")
    void findKeywordByName_NotFound_ReturnsNull() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword = KeywordFixture.createCommonKeywordWithName("공지");
      board.addKeyword(keyword);

      // when
      Keyword found = board.findKeywordByName("없는키워드");

      // then
      assertThat(found).isNull();
    }

    @Test
    @DisplayName("정상: 활성화된 키워드만 조회할 수 있다")
    void getActiveKeywords_Success() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword activeKeyword = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword inactiveKeyword = KeywordFixture.createInactiveKeyword();
      board.addKeyword(activeKeyword);
      board.addKeyword(inactiveKeyword);

      // when
      var activeKeywords = board.getActiveKeywords();

      // then
      assertThat(activeKeywords).hasSize(1);
      assertThat(activeKeywords.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("정상: 키워드 수를 조회할 수 있다")
    void getKeywordCount_Success() {
      // given
      Board board = BoardFixture.createBoard();
      board.addKeyword(KeywordFixture.createCommonKeywordWithId(1L));
      board.addKeyword(KeywordFixture.createCommonKeywordWithId(2L));

      // when
      int count = board.getKeywordCount();

      // then
      assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("엣지: null 키워드 제거 시 아무 일도 일어나지 않는다")
    void removeNullKeyword_DoesNothing() {
      // given
      Board board = BoardFixture.createBoard();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      board.addKeyword(keyword);

      // when
      board.removeKeyword(null);

      // then
      assertThat(board.getKeywords()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("equals & hashCode 테스트")
  class EqualsAndHashCodeTest {

    @Test
    @DisplayName("정상: 같은 ID를 가진 Board는 동등하다")
    void equals_SameId_ReturnsTrue() {
      // given
      Board board1 = BoardFixture.createBoardWithId(1L);
      Board board2 = BoardFixture.createBoardWithId(1L);

      // when & then
      assertThat(board1).isEqualTo(board2);
      assertThat(board1.hashCode()).isEqualTo(board2.hashCode());
    }

    @Test
    @DisplayName("정상: 다른 ID를 가진 Board는 동등하지 않다")
    void equals_DifferentId_ReturnsFalse() {
      // given
      Board board1 = BoardFixture.createBoardWithId(1L);
      Board board2 = BoardFixture.createBoardWithId(2L);

      // when & then
      assertThat(board1).isNotEqualTo(board2);
    }

    @Test
    @DisplayName("정상: 자기 자신과는 동등하다")
    void equals_Self_ReturnsTrue() {
      // given
      Board board = BoardFixture.createBoard();

      // when & then
      assertThat(board).isEqualTo(board);
    }

    @Test
    @DisplayName("정상: null과는 동등하지 않다")
    void equals_Null_ReturnsFalse() {
      // given
      Board board = BoardFixture.createBoard();

      // when & then
      assertThat(board).isNotEqualTo(null);
    }
  }

  @Nested
  @DisplayName("toString 테스트")
  class ToStringTest {

    @Test
    @DisplayName("정상: toString이 주요 정보를 포함한다")
    void toString_ContainsMainInfo() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      String result = board.toString();

      // then
      assertThat(result).contains("id=1");
      assertThat(result).contains("name='자유게시판'");
      assertThat(result).contains("isActive=true");
    }
  }
}

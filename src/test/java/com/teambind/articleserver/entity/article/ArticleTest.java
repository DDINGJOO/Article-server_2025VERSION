package com.teambind.articleserver.entity.article;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.entity.articleType.RegularArticle;
import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.entity.image.ArticleImage;
import com.teambind.articleserver.entity.keyword.Keyword;
import com.teambind.articleserver.fixture.ArticleFixture;
import com.teambind.articleserver.fixture.BoardFixture;
import com.teambind.articleserver.fixture.KeywordFixture;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Article 엔티티 테스트")
class ArticleTest {

  @Nested
  @DisplayName("게시글 생성 테스트")
  class CreateArticleTest {

    @Test
    @DisplayName("정상: RegularArticle을 빌더로 생성할 수 있다")
    void createRegularArticle_Success() {
      // given & when
      RegularArticle article = ArticleFixture.createRegularArticle();

      // then
      assertThat(article).isNotNull();
      assertThat(article.getId()).isEqualTo("ART_20251025_001");
      assertThat(article.getTitle()).isEqualTo("일반 게시글 제목");
      assertThat(article.getContent()).isEqualTo("일반 게시글 내용입니다.");
      assertThat(article.getWriterId()).isEqualTo("user123");
      assertThat(article.getStatus()).isEqualTo(Status.ACTIVE);
      assertThat(article.getViewCount()).isZero();
    }

    @Test
    @DisplayName("정상: Board와 연관관계가 설정된다")
    void createArticleWithBoard_Success() {
      // given
      Board board = BoardFixture.createBoard();

      // when
      RegularArticle article = ArticleFixture.createRegularArticleWithBoard(board);

      // then
      assertThat(article.getBoard()).isEqualTo(board);
      assertThat(article.getBoard().getName()).isEqualTo("자유게시판");
    }

    @Test
    @DisplayName("정상: 기본값이 올바르게 설정된다")
    void createArticle_WithDefaultValues_Success() {
      // given & when
      RegularArticle article = ArticleFixture.createRegularArticle();

      // then
      assertThat(article.getStatus()).isEqualTo(Status.ACTIVE);
      assertThat(article.getViewCount()).isZero();
      assertThat(article.getVersion()).isZero();
      assertThat(article.getImages()).isEmpty();
      assertThat(article.getKeywordMappings()).isEmpty();
    }
  }

  @Nested
  @DisplayName("게시글 내용 수정 테스트")
  class UpdateContentTest {

    @Test
    @DisplayName("정상: 제목과 내용을 수정할 수 있다")
    void updateContent_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      LocalDateTime beforeUpdate = article.getUpdatedAt();

      // when
      article.updateContent("새로운 제목", "새로운 내용");

      // then
      assertThat(article.getTitle()).isEqualTo("새로운 제목");
      assertThat(article.getContent()).isEqualTo("새로운 내용");
      assertThat(article.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("정상: 제목만 수정할 수 있다")
    void updateTitle_Only_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      String originalContent = article.getContent();

      // when
      article.updateContent("새 제목만", null);

      // then
      assertThat(article.getTitle()).isEqualTo("새 제목만");
      assertThat(article.getContent()).isEqualTo(originalContent);
    }

    @Test
    @DisplayName("정상: 내용만 수정할 수 있다")
    void updateContent_Only_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      String originalTitle = article.getTitle();

      // when
      article.updateContent(null, "새 내용만");

      // then
      assertThat(article.getTitle()).isEqualTo(originalTitle);
      assertThat(article.getContent()).isEqualTo("새 내용만");
    }

    @Test
    @DisplayName("엣지: 빈 문자열로 수정 시 원래 값 유지")
    void updateContent_WithBlankString_KeepsOriginalValue() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      String originalTitle = article.getTitle();
      String originalContent = article.getContent();

      // when
      article.updateContent("", "");

      // then
      assertThat(article.getTitle()).isEqualTo(originalTitle);
      assertThat(article.getContent()).isEqualTo(originalContent);
    }
  }

  @Nested
  @DisplayName("상태 변경 테스트")
  class StatusChangeTest {

    @Test
    @DisplayName("정상: 게시글을 삭제(Soft Delete)할 수 있다")
    void deleteArticle_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      LocalDateTime beforeUpdate = article.getUpdatedAt();

      // when
      article.delete();

      // then
      assertThat(article.getStatus()).isEqualTo(Status.DELETED);
      assertThat(article.isDeleted()).isTrue();
      assertThat(article.isActive()).isFalse();
      assertThat(article.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("정상: 게시글을 차단할 수 있다")
    void blockArticle_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      article.block();

      // then
      assertThat(article.getStatus()).isEqualTo(Status.BLOCKED);
      assertThat(article.isBlocked()).isTrue();
      assertThat(article.isActive()).isFalse();
    }

    @Test
    @DisplayName("정상: 차단된 게시글을 활성화할 수 있다")
    void activateArticle_Success() {
      // given
      RegularArticle article = ArticleFixture.createBlockedArticle();

      // when
      article.activate();

      // then
      assertThat(article.getStatus()).isEqualTo(Status.ACTIVE);
      assertThat(article.isActive()).isTrue();
      assertThat(article.isBlocked()).isFalse();
    }

    @Test
    @DisplayName("정상: 삭제된 게시글을 활성화할 수 있다")
    void activateDeletedArticle_Success() {
      // given
      RegularArticle article = ArticleFixture.createDeletedArticle();

      // when
      article.activate();

      // then
      assertThat(article.getStatus()).isEqualTo(Status.ACTIVE);
      assertThat(article.isActive()).isTrue();
      assertThat(article.isDeleted()).isFalse();
    }
  }

  @Nested
  @DisplayName("조회수 증가 테스트")
  class ViewCountTest {

    @Test
    @DisplayName("정상: 조회수를 1씩 증가시킬 수 있다")
    void incrementViewCount_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      article.incrementViewCount();
      article.incrementViewCount();

      // then
      assertThat(article.getViewCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("엣지: 조회수가 null인 경우 0으로 초기화 후 증가")
    void incrementViewCount_WhenNull_InitializesToZero() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      // Reflection으로 viewCount를 null로 설정
      java.lang.reflect.Field field;
      try {
        field = Article.class.getDeclaredField("viewCount");
        field.setAccessible(true);
        field.set(article, null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      // when
      article.incrementViewCount();

      // then
      assertThat(article.getViewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("정상: 높은 조회수에서도 정상 동작한다")
    void incrementViewCount_WithHighCount_Success() {
      // given
      RegularArticle article = ArticleFixture.createHighViewCountArticle(999999L);

      // when
      article.incrementViewCount();

      // then
      assertThat(article.getViewCount()).isEqualTo(1000000L);
    }
  }

  @Nested
  @DisplayName("작성자 확인 테스트")
  class WriterCheckTest {

    @Test
    @DisplayName("정상: 작성자가 맞으면 true를 반환한다")
    void isWrittenBy_CorrectWriter_ReturnsTrue() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      boolean result = article.isWrittenBy("user123");

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 작성자가 아니면 false를 반환한다")
    void isWrittenBy_WrongWriter_ReturnsFalse() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      boolean result = article.isWrittenBy("user456");

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("엣지: null 작성자 ID는 false를 반환한다")
    void isWrittenBy_NullUserId_ReturnsFalse() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      boolean result = article.isWrittenBy(null);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("이미지 관리 테스트")
  class ImageManagementTest {

    @Test
    @DisplayName("정상: 이미지를 추가할 수 있다")
    void addImage_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      article.addImage("IMG_001", "https://example.com/image1.jpg");

      // then
      assertThat(article.getImages()).hasSize(1);
      ArticleImage image = article.getImages().get(0);
      assertThat(image.getImageId()).isEqualTo("IMG_001");
      assertThat(image.getImageUrl()).isEqualTo("https://example.com/image1.jpg");
      assertThat(image.getSequence()).isEqualTo(1L);
    }

    @Test
    @DisplayName("정상: 첫 번째 이미지가 대표 이미지로 설정된다")
    void addImage_FirstImage_SetAsFirstImageUrl() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      article.addImage("IMG_001", "https://example.com/image1.jpg");

      // then
      assertThat(article.getFirstImageUrl()).isEqualTo("https://example.com/image1.jpg");
    }

    @Test
    @DisplayName("정상: 여러 이미지를 순서대로 추가할 수 있다")
    void addMultipleImages_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      article.addImage("IMG_001", "https://example.com/image1.jpg");
      article.addImage("IMG_002", "https://example.com/image2.jpg");
      article.addImage("IMG_003", "https://example.com/image3.jpg");

      // then
      assertThat(article.getImages()).hasSize(3);
      assertThat(article.getImages().get(0).getSequence()).isEqualTo(1L);
      assertThat(article.getImages().get(1).getSequence()).isEqualTo(2L);
      assertThat(article.getImages().get(2).getSequence()).isEqualTo(3L);
      assertThat(article.getFirstImageUrl()).isEqualTo("https://example.com/image1.jpg");
    }

    @Test
    @DisplayName("예외: null 이미지 ID는 추가되지 않는다")
    void addImage_WithNullImageId_DoesNotAdd() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      article.addImage(null, "https://example.com/image1.jpg");

      // then
      assertThat(article.getImages()).isEmpty();
    }

    @Test
    @DisplayName("예외: null 이미지 URL은 추가되지 않는다")
    void addImage_WithNullImageUrl_DoesNotAdd() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      article.addImage("IMG_001", null);

      // then
      assertThat(article.getImages()).isEmpty();
    }

    @Test
    @DisplayName("정상: 이미지를 제거할 수 있다")
    void removeImage_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      article.addImage("IMG_001", "https://example.com/image1.jpg");
      article.addImage("IMG_002", "https://example.com/image2.jpg");
      ArticleImage imageToRemove = article.getImages().get(0);

      // when
      article.removeImage(imageToRemove);

      // then
      assertThat(article.getImages()).hasSize(1);
      assertThat(article.getImages().get(0).getImageId()).isEqualTo("IMG_002");
    }

    @Test
    @DisplayName("정상: 대표 이미지 제거 시 다음 이미지가 대표 이미지가 된다")
    void removeFirstImage_NextImageBecomesFirst() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      article.addImage("IMG_001", "https://example.com/image1.jpg");
      article.addImage("IMG_002", "https://example.com/image2.jpg");
      ArticleImage firstImage = article.getImages().get(0);

      // when
      article.removeImage(firstImage);

      // then
      assertThat(article.getFirstImageUrl()).isEqualTo("https://example.com/image2.jpg");
    }

    @Test
    @DisplayName("엣지: 마지막 이미지 제거 시 대표 이미지가 null이 된다")
    void removeLastImage_FirstImageUrlBecomesNull() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      article.addImage("IMG_001", "https://example.com/image1.jpg");
      ArticleImage image = article.getImages().get(0);

      // when
      article.removeImage(image);

      // then
      assertThat(article.getImages()).isEmpty();
      assertThat(article.getFirstImageUrl()).isNull();
    }

    @Test
    @DisplayName("정상: 모든 이미지를 제거할 수 있다")
    void removeAllImages_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      article.addImage("IMG_001", "https://example.com/image1.jpg");
      article.addImage("IMG_002", "https://example.com/image2.jpg");

      // when
      article.removeImages();

      // then
      assertThat(article.getImages()).isEmpty();
      assertThat(article.getFirstImageUrl()).isNull();
    }

    @Test
    @DisplayName("엣지: null 이미지 제거 시 아무 일도 일어나지 않는다")
    void removeNullImage_DoesNothing() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      article.addImage("IMG_001", "https://example.com/image1.jpg");

      // when
      article.removeImage(null);

      // then
      assertThat(article.getImages()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("키워드 관리 테스트")
  class KeywordManagementTest {

    @Test
    @DisplayName("정상: 키워드를 추가할 수 있다")
    void addKeyword_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      article.addKeyword(keyword);

      // then
      assertThat(article.getKeywordMappings()).hasSize(1);
      assertThat(keyword.getUsageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("정상: 여러 키워드를 추가할 수 있다")
    void addMultipleKeywords_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword1 = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword keyword2 = KeywordFixture.createCommonKeywordWithId(2L);
      Keyword keyword3 = KeywordFixture.createCommonKeywordWithId(3L);

      // when
      article.addKeyword(keyword1);
      article.addKeyword(keyword2);
      article.addKeyword(keyword3);

      // then
      assertThat(article.getKeywordMappings()).hasSize(3);
      assertThat(keyword1.getUsageCount()).isEqualTo(1);
      assertThat(keyword2.getUsageCount()).isEqualTo(1);
      assertThat(keyword3.getUsageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("정상: 중복 키워드는 추가되지 않는다")
    void addDuplicateKeyword_DoesNotAdd() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      article.addKeyword(keyword);
      article.addKeyword(keyword); // 중복 추가 시도

      // then
      assertThat(article.getKeywordMappings()).hasSize(1);
      assertThat(keyword.getUsageCount()).isEqualTo(1); // 1번만 증가
    }

    @Test
    @DisplayName("예외: null 키워드는 추가되지 않는다")
    void addNullKeyword_DoesNotAdd() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      article.addKeyword(null);

      // then
      assertThat(article.getKeywordMappings()).isEmpty();
    }

    @Test
    @DisplayName("정상: 키워드 리스트를 일괄 추가할 수 있다")
    void addKeywords_WithList_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      List<Keyword> keywords =
          List.of(
              KeywordFixture.createCommonKeywordWithId(1L),
              KeywordFixture.createCommonKeywordWithId(2L),
              KeywordFixture.createCommonKeywordWithId(3L));

      // when
      article.addKeywords(keywords);

      // then
      assertThat(article.getKeywordMappings()).hasSize(3);
    }

    @Test
    @DisplayName("정상: 키워드를 제거할 수 있다")
    void removeKeyword_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      article.addKeyword(keyword);

      // when
      article.removeKeyword(keyword);

      // then
      assertThat(article.getKeywordMappings()).isEmpty();
      assertThat(keyword.getUsageCount()).isZero();
    }

    @Test
    @DisplayName("정상: 모든 키워드를 제거할 수 있다")
    void removeAllKeywords_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword1 = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword keyword2 = KeywordFixture.createCommonKeywordWithId(2L);
      article.addKeyword(keyword1);
      article.addKeyword(keyword2);

      // when
      article.removeKeywords();

      // then
      assertThat(article.getKeywordMappings()).isEmpty();
      assertThat(keyword1.getUsageCount()).isZero();
      assertThat(keyword2.getUsageCount()).isZero();
    }

    @Test
    @DisplayName("정상: 키워드를 교체할 수 있다")
    void replaceKeywords_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword oldKeyword = KeywordFixture.createCommonKeywordWithId(1L);
      article.addKeyword(oldKeyword);

      List<Keyword> newKeywords =
          List.of(
              KeywordFixture.createCommonKeywordWithId(2L),
              KeywordFixture.createCommonKeywordWithId(3L));

      // when
      article.replaceKeywords(newKeywords);

      // then
      assertThat(article.getKeywordMappings()).hasSize(2);
      assertThat(oldKeyword.getUsageCount()).isZero();
    }

    @Test
    @DisplayName("엣지: null 키워드 제거 시 아무 일도 일어나지 않는다")
    void removeNullKeyword_DoesNothing() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      article.addKeyword(keyword);

      // when
      article.removeKeyword(null);

      // then
      assertThat(article.getKeywordMappings()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Board 연관관계 테스트")
  class BoardRelationTest {

    @Test
    @DisplayName("정상: Board를 변경할 수 있다")
    void setBoard_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Board newBoard = BoardFixture.createBoardWithId(2L);

      // when
      article.setBoard(newBoard);

      // then
      assertThat(article.getBoard()).isEqualTo(newBoard);
    }

    @Test
    @DisplayName("정상: Board 변경 시 양방향 관계가 유지된다")
    void setBoard_MaintainsBidirectionalRelation() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Board oldBoard = article.getBoard();
      Board newBoard = BoardFixture.createBoardWithId(2L);

      // when
      article.setBoard(newBoard);

      // then
      assertThat(newBoard.getArticles()).contains(article);
      assertThat(oldBoard.getArticles()).doesNotContain(article);
    }
  }

  @Nested
  @DisplayName("equals & hashCode 테스트")
  class EqualsAndHashCodeTest {

    @Test
    @DisplayName("정상: 같은 ID를 가진 Article은 동등하다")
    void equals_SameId_ReturnsTrue() {
      // given
      RegularArticle article1 = ArticleFixture.createRegularArticleWithId("ART_001");
      RegularArticle article2 = ArticleFixture.createRegularArticleWithId("ART_001");

      // when & then
      assertThat(article1).isEqualTo(article2);
      assertThat(article1.hashCode()).isEqualTo(article2.hashCode());
    }

    @Test
    @DisplayName("정상: 다른 ID를 가진 Article은 동등하지 않다")
    void equals_DifferentId_ReturnsFalse() {
      // given
      RegularArticle article1 = ArticleFixture.createRegularArticleWithId("ART_001");
      RegularArticle article2 = ArticleFixture.createRegularArticleWithId("ART_002");

      // when & then
      assertThat(article1).isNotEqualTo(article2);
    }

    @Test
    @DisplayName("정상: 자기 자신과는 동등하다")
    void equals_Self_ReturnsTrue() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThat(article).isEqualTo(article);
    }

    @Test
    @DisplayName("정상: null과는 동등하지 않다")
    void equals_Null_ReturnsFalse() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThat(article).isNotEqualTo(null);
    }
  }

  @Nested
  @DisplayName("toString 테스트")
  class ToStringTest {

    @Test
    @DisplayName("정상: toString이 주요 정보를 포함한다")
    void toString_ContainsMainInfo() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      String result = article.toString();

      // then
      assertThat(result).contains("ART_20251025_001");
      assertThat(result).contains("일반 게시글 제목");
      assertThat(result).contains("user123");
      assertThat(result).contains("ACTIVE");
    }
  }
}

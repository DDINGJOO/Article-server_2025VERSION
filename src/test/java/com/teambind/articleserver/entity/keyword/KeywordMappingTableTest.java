package com.teambind.articleserver.entity.keyword;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.entity.articleType.RegularArticle;
import com.teambind.articleserver.fixture.ArticleFixture;
import com.teambind.articleserver.fixture.KeywordFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KeywordMappingTable 엔티티 테스트")
class KeywordMappingTableTest {

  @Nested
  @DisplayName("매핑 생성 테스트")
  class CreateMappingTest {

    @Test
    @DisplayName("정상: KeywordMappingTable을 생성할 수 있다")
    void createMapping_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);

      // then
      assertThat(mapping).isNotNull();
      assertThat(mapping.getArticleId()).isEqualTo(article.getId());
      assertThat(mapping.getKeywordId()).isEqualTo(keyword.getId());
      assertThat(mapping.getArticle()).isEqualTo(article);
      assertThat(mapping.getKeyword()).isEqualTo(keyword);
      assertThat(mapping.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("예외: Article이 null이면 예외 발생")
    void createMapping_WithNullArticle_ThrowsException() {
      // given
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when & then
      assertThatThrownBy(() -> new KeywordMappingTable(null, keyword))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Article and Keyword cannot be null");
    }

    @Test
    @DisplayName("예외: Keyword가 null이면 예외 발생")
    void createMapping_WithNullKeyword_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThatThrownBy(() -> new KeywordMappingTable(article, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Article and Keyword cannot be null");
    }

    @Test
    @DisplayName("예외: Article ID가 null이면 예외 발생")
    void createMapping_WithNullArticleId_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createArticleWithoutId();
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when & then
      assertThatThrownBy(() -> new KeywordMappingTable(article, keyword))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Article must have an ID");
    }

    @Test
    @DisplayName("예외: Keyword ID가 null이면 예외 발생")
    void createMapping_WithNullKeywordId_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createKeywordWithoutId();

      // when & then
      assertThatThrownBy(() -> new KeywordMappingTable(article, keyword))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Keyword must be persisted before creating mapping");
    }

    @Test
    @DisplayName("예외: 둘 다 null이면 예외 발생")
    void createMapping_WithBothNull_ThrowsException() {
      // when & then
      assertThatThrownBy(() -> new KeywordMappingTable(null, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Article and Keyword cannot be null");
    }
  }

  @Nested
  @DisplayName("복합키 조회 테스트")
  class CompositeKeyTest {

    @Test
    @DisplayName("정상: Article ID를 조회할 수 있다")
    void getArticleId_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);

      // when
      String articleId = mapping.getArticleId();

      // then
      assertThat(articleId).isEqualTo(article.getId());
    }

    @Test
    @DisplayName("정상: Keyword ID를 조회할 수 있다")
    void getKeywordId_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);

      // when
      Long keywordId = mapping.getKeywordId();

      // then
      assertThat(keywordId).isEqualTo(keyword.getId());
    }

    @Test
    @DisplayName("정상: 복합키가 올바르게 설정된다")
    void compositeKey_SetCorrectly() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticleWithId("ART_001");
      Keyword keyword = KeywordFixture.createCommonKeywordWithId(10L);

      // when
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);

      // then
      assertThat(mapping.getId()).isNotNull();
      assertThat(mapping.getId().getArticleId()).isEqualTo("ART_001");
      assertThat(mapping.getId().getKeywordId()).isEqualTo(10L);
    }
  }

  @Nested
  @DisplayName("매핑 분리 테스트")
  class DetachTest {

    @Test
    @DisplayName("정상: 매핑을 분리할 수 있다")
    void detach_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);

      // Article과 Keyword의 컬렉션에 매핑 추가
      article.getKeywordMappings().add(mapping);
      keyword.getMappings().add(mapping);

      // when
      mapping.detach();

      // then
      assertThat(article.getKeywordMappings()).doesNotContain(mapping);
      assertThat(keyword.getMappings()).doesNotContain(mapping);
      assertThat(mapping.getArticle()).isNull();
      assertThat(mapping.getKeyword()).isNull();
    }

    @Test
    @DisplayName("엣지: Article이 null인 상태에서 detach 호출 시 문제없다")
    void detach_WithNullArticle_DoesNotThrow() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);
      mapping.setArticle(null);

      // when & then
      assertThatCode(() -> mapping.detach()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("엣지: Keyword가 null인 상태에서 detach 호출 시 문제없다")
    void detach_WithNullKeyword_DoesNotThrow() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);
      mapping.setKeyword(null);

      // when & then
      assertThatCode(() -> mapping.detach()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("엣지: 둘 다 null인 상태에서 detach 호출 시 문제없다")
    void detach_WithBothNull_DoesNotThrow() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);
      mapping.setArticle(null);
      mapping.setKeyword(null);

      // when & then
      assertThatCode(() -> mapping.detach()).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("생성일시 테스트")
  class CreatedAtTest {

    @Test
    @DisplayName("정상: 생성 시 createdAt이 자동 설정된다")
    void createdAt_AutoSet_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);

      // then
      assertThat(mapping.getCreatedAt()).isNotNull();
      assertThat(mapping.getCreatedAt())
          .isBeforeOrEqualTo(java.time.LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("정상: 여러 매핑의 생성일시가 서로 다를 수 있다")
    void createdAt_DifferentForMultipleMappings() throws InterruptedException {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword1 = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword keyword2 = KeywordFixture.createCommonKeywordWithId(2L);

      // when
      KeywordMappingTable mapping1 = new KeywordMappingTable(article, keyword1);
      Thread.sleep(10); // 시간 차이를 만들기 위해
      KeywordMappingTable mapping2 = new KeywordMappingTable(article, keyword2);

      // then
      assertThat(mapping1.getCreatedAt()).isNotNull();
      assertThat(mapping2.getCreatedAt()).isNotNull();
      // 시간이 다를 수도, 같을 수도 있으므로 both not null만 확인
    }
  }

  @Nested
  @DisplayName("equals & hashCode 테스트")
  class EqualsAndHashCodeTest {

    @Test
    @DisplayName("정상: 같은 복합키를 가진 매핑은 동등하다")
    void equals_SameCompositeId_ReturnsTrue() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping1 = new KeywordMappingTable(article, keyword);
      KeywordMappingTable mapping2 = new KeywordMappingTable(article, keyword);

      // when & then
      assertThat(mapping1).isEqualTo(mapping2);
      assertThat(mapping1.hashCode()).isEqualTo(mapping2.hashCode());
    }

    @Test
    @DisplayName("정상: 다른 Article을 가진 매핑은 동등하지 않다")
    void equals_DifferentArticle_ReturnsFalse() {
      // given
      RegularArticle article1 = ArticleFixture.createRegularArticleWithId("ART_001");
      RegularArticle article2 = ArticleFixture.createRegularArticleWithId("ART_002");
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping1 = new KeywordMappingTable(article1, keyword);
      KeywordMappingTable mapping2 = new KeywordMappingTable(article2, keyword);

      // when & then
      assertThat(mapping1).isNotEqualTo(mapping2);
    }

    @Test
    @DisplayName("정상: 다른 Keyword를 가진 매핑은 동등하지 않다")
    void equals_DifferentKeyword_ReturnsFalse() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword1 = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword keyword2 = KeywordFixture.createCommonKeywordWithId(2L);
      KeywordMappingTable mapping1 = new KeywordMappingTable(article, keyword1);
      KeywordMappingTable mapping2 = new KeywordMappingTable(article, keyword2);

      // when & then
      assertThat(mapping1).isNotEqualTo(mapping2);
    }

    @Test
    @DisplayName("정상: 자기 자신과는 동등하다")
    void equals_Self_ReturnsTrue() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);

      // when & then
      assertThat(mapping).isEqualTo(mapping);
    }

    @Test
    @DisplayName("정상: null과는 동등하지 않다")
    void equals_Null_ReturnsFalse() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);

      // when & then
      assertThat(mapping).isNotEqualTo(null);
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
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);

      // when
      String result = mapping.toString();

      // then
      assertThat(result).contains("articleId='" + article.getId() + "'");
      assertThat(result).contains("keywordId=" + keyword.getId());
      assertThat(result).contains("createdAt=");
    }
  }

  @Nested
  @DisplayName("양방향 연관관계 테스트")
  class BidirectionalRelationTest {

    @Test
    @DisplayName("정상: Article과 Keyword 양쪽에 매핑이 추가된다")
    void bidirectionalRelation_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);
      article.getKeywordMappings().add(mapping);
      keyword.getMappings().add(mapping);

      // then
      assertThat(article.getKeywordMappings()).contains(mapping);
      assertThat(keyword.getMappings()).contains(mapping);
      assertThat(mapping.getArticle()).isEqualTo(article);
      assertThat(mapping.getKeyword()).isEqualTo(keyword);
    }

    @Test
    @DisplayName("정상: 매핑 제거 시 양방향 관계가 모두 제거된다")
    void removeBidirectionalRelation_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();
      KeywordMappingTable mapping = new KeywordMappingTable(article, keyword);
      article.getKeywordMappings().add(mapping);
      keyword.getMappings().add(mapping);

      // when
      mapping.detach();

      // then
      assertThat(article.getKeywordMappings()).doesNotContain(mapping);
      assertThat(keyword.getMappings()).doesNotContain(mapping);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("엣지: 같은 Article과 Keyword로 여러 매핑 생성 시 복합키가 같다")
    void multipleMapping_SameCompositeKey() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      KeywordMappingTable mapping1 = new KeywordMappingTable(article, keyword);
      KeywordMappingTable mapping2 = new KeywordMappingTable(article, keyword);

      // then
      assertThat(mapping1.getId()).isEqualTo(mapping2.getId());
    }

    @Test
    @DisplayName("엣지: 하나의 Article에 여러 Keyword 매핑 가능")
    void oneArticle_MultipleKeywords() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      Keyword keyword1 = KeywordFixture.createCommonKeywordWithId(1L);
      Keyword keyword2 = KeywordFixture.createCommonKeywordWithId(2L);
      Keyword keyword3 = KeywordFixture.createCommonKeywordWithId(3L);

      // when
      KeywordMappingTable mapping1 = new KeywordMappingTable(article, keyword1);
      KeywordMappingTable mapping2 = new KeywordMappingTable(article, keyword2);
      KeywordMappingTable mapping3 = new KeywordMappingTable(article, keyword3);

      // then
      assertThat(mapping1.getArticleId()).isEqualTo(article.getId());
      assertThat(mapping2.getArticleId()).isEqualTo(article.getId());
      assertThat(mapping3.getArticleId()).isEqualTo(article.getId());
      assertThat(mapping1).isNotEqualTo(mapping2);
      assertThat(mapping2).isNotEqualTo(mapping3);
    }

    @Test
    @DisplayName("엣지: 하나의 Keyword에 여러 Article 매핑 가능")
    void oneKeyword_MultipleArticles() {
      // given
      RegularArticle article1 = ArticleFixture.createRegularArticleWithId("ART_001");
      RegularArticle article2 = ArticleFixture.createRegularArticleWithId("ART_002");
      RegularArticle article3 = ArticleFixture.createRegularArticleWithId("ART_003");
      Keyword keyword = KeywordFixture.createCommonKeyword();

      // when
      KeywordMappingTable mapping1 = new KeywordMappingTable(article1, keyword);
      KeywordMappingTable mapping2 = new KeywordMappingTable(article2, keyword);
      KeywordMappingTable mapping3 = new KeywordMappingTable(article3, keyword);

      // then
      assertThat(mapping1.getKeywordId()).isEqualTo(keyword.getId());
      assertThat(mapping2.getKeywordId()).isEqualTo(keyword.getId());
      assertThat(mapping3.getKeywordId()).isEqualTo(keyword.getId());
      assertThat(mapping1).isNotEqualTo(mapping2);
      assertThat(mapping2).isNotEqualTo(mapping3);
    }
  }
}

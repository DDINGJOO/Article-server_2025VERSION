package com.teambind.articleserver.entity.image;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.entity.articleType.RegularArticle;
import com.teambind.articleserver.fixture.ArticleFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ArticleImage 엔티티 테스트")
class ArticleImageTest {

  @Nested
  @DisplayName("ArticleImage 생성 테스트")
  class CreateArticleImageTest {

    @Test
    @DisplayName("정상: ArticleImage를 생성할 수 있다")
    void createArticleImage_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // then
      assertThat(articleImage).isNotNull();
      assertThat(articleImage.getArticleId()).isEqualTo(article.getId());
      assertThat(articleImage.getSequence()).isEqualTo(1L);
      assertThat(articleImage.getImageUrl()).isEqualTo("https://example.com/image1.jpg");
      assertThat(articleImage.getImageId()).isEqualTo("IMG_001");
      assertThat(articleImage.getArticle()).isEqualTo(article);
    }

    @Test
    @DisplayName("예외: Article이 null이면 예외 발생")
    void createArticleImage_WithNullArticle_ThrowsException() {
      // when & then
      assertThatThrownBy(
              () -> new ArticleImage(null, 1L, "https://example.com/image1.jpg", "IMG_001"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Article must have an ID");
    }

    @Test
    @DisplayName("예외: Article ID가 null이면 예외 발생")
    void createArticleImage_WithNullArticleId_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createArticleWithoutId();

      // when & then
      assertThatThrownBy(
              () -> new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Article must have an ID");
    }

    @Test
    @DisplayName("예외: sequence가 null이면 예외 발생")
    void createArticleImage_WithNullSequence_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThatThrownBy(
              () -> new ArticleImage(article, null, "https://example.com/image1.jpg", "IMG_001"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Sequence must be greater than 0");
    }

    @Test
    @DisplayName("예외: sequence가 0이면 예외 발생")
    void createArticleImage_WithZeroSequence_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThatThrownBy(
              () -> new ArticleImage(article, 0L, "https://example.com/image1.jpg", "IMG_001"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Sequence must be greater than 0");
    }

    @Test
    @DisplayName("예외: sequence가 음수이면 예외 발생")
    void createArticleImage_WithNegativeSequence_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThatThrownBy(
              () -> new ArticleImage(article, -1L, "https://example.com/image1.jpg", "IMG_001"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Sequence must be greater than 0");
    }

    @Test
    @DisplayName("예외: imageUrl이 null이면 예외 발생")
    void createArticleImage_WithNullImageUrl_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThatThrownBy(() -> new ArticleImage(article, 1L, null, "IMG_001"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image URL cannot be null or empty");
    }

    @Test
    @DisplayName("예외: imageUrl이 빈 문자열이면 예외 발생")
    void createArticleImage_WithEmptyImageUrl_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThatThrownBy(() -> new ArticleImage(article, 1L, "", "IMG_001"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image URL cannot be null or empty");
    }

    @Test
    @DisplayName("예외: imageId가 null이면 예외 발생")
    void createArticleImage_WithNullImageId_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThatThrownBy(
              () -> new ArticleImage(article, 1L, "https://example.com/image1.jpg", null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image ID cannot be null or empty");
    }

    @Test
    @DisplayName("예외: imageId가 빈 문자열이면 예외 발생")
    void createArticleImage_WithEmptyImageId_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();

      // when & then
      assertThatThrownBy(() -> new ArticleImage(article, 1L, "https://example.com/image1.jpg", ""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image ID cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("이미지 URL 변경 테스트")
  class UpdateImageUrlTest {

    @Test
    @DisplayName("정상: 이미지 URL을 변경할 수 있다")
    void updateImageUrl_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when
      articleImage.updateImageUrl("https://example.com/image2.jpg");

      // then
      assertThat(articleImage.getImageUrl()).isEqualTo("https://example.com/image2.jpg");
    }

    @Test
    @DisplayName("예외: null URL로 변경 시 예외 발생")
    void updateImageUrl_WithNull_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThatThrownBy(() -> articleImage.updateImageUrl(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image URL cannot be null or empty");
    }

    @Test
    @DisplayName("예외: 빈 문자열 URL로 변경 시 예외 발생")
    void updateImageUrl_WithEmptyString_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThatThrownBy(() -> articleImage.updateImageUrl(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image URL cannot be null or empty");
    }

    @Test
    @DisplayName("엣지: 공백 문자열 URL로 변경 시 예외 발생")
    void updateImageUrl_WithBlankString_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThatThrownBy(() -> articleImage.updateImageUrl("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image URL cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("이미지 ID 변경 테스트")
  class UpdateImageIdTest {

    @Test
    @DisplayName("정상: 이미지 ID를 변경할 수 있다")
    void updateImageId_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when
      articleImage.updateImageId("IMG_002");

      // then
      assertThat(articleImage.getImageId()).isEqualTo("IMG_002");
    }

    @Test
    @DisplayName("예외: null ID로 변경 시 예외 발생")
    void updateImageId_WithNull_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThatThrownBy(() -> articleImage.updateImageId(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image ID cannot be null or empty");
    }

    @Test
    @DisplayName("예외: 빈 문자열 ID로 변경 시 예외 발생")
    void updateImageId_WithEmptyString_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThatThrownBy(() -> articleImage.updateImageId(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Image ID cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("순서 변경 테스트")
  class UpdateSequenceTest {

    @Test
    @DisplayName("정상: 순서를 변경할 수 있다")
    void updateSequence_Success() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when
      articleImage.updateSequence(2L);

      // then
      assertThat(articleImage.getSequence()).isEqualTo(2L);
    }

    @Test
    @DisplayName("예외: null 순서로 변경 시 예외 발생")
    void updateSequence_WithNull_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThatThrownBy(() -> articleImage.updateSequence(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Sequence must be greater than 0");
    }

    @Test
    @DisplayName("예외: 0 순서로 변경 시 예외 발생")
    void updateSequence_WithZero_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThatThrownBy(() -> articleImage.updateSequence(0L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Sequence must be greater than 0");
    }

    @Test
    @DisplayName("예외: 음수 순서로 변경 시 예외 발생")
    void updateSequence_WithNegative_ThrowsException() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage articleImage =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThatThrownBy(() -> articleImage.updateSequence(-1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Sequence must be greater than 0");
    }
  }

  @Nested
  @DisplayName("equals & hashCode 테스트")
  class EqualsAndHashCodeTest {

    @Test
    @DisplayName("정상: 같은 복합키를 가진 ArticleImage는 동등하다")
    void equals_SameCompositeId_ReturnsTrue() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage image1 =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");
      ArticleImage image2 =
          new ArticleImage(article, 1L, "https://example.com/image2.jpg", "IMG_002");

      // when & then
      assertThat(image1).isEqualTo(image2);
      assertThat(image1.hashCode()).isEqualTo(image2.hashCode());
    }

    @Test
    @DisplayName("정상: 다른 sequence를 가진 ArticleImage는 동등하지 않다")
    void equals_DifferentSequence_ReturnsFalse() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage image1 =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");
      ArticleImage image2 =
          new ArticleImage(article, 2L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThat(image1).isNotEqualTo(image2);
    }

    @Test
    @DisplayName("정상: 다른 article을 가진 ArticleImage는 동등하지 않다")
    void equals_DifferentArticle_ReturnsFalse() {
      // given
      RegularArticle article1 = ArticleFixture.createRegularArticleWithId("ART_001");
      RegularArticle article2 = ArticleFixture.createRegularArticleWithId("ART_002");
      ArticleImage image1 =
          new ArticleImage(article1, 1L, "https://example.com/image1.jpg", "IMG_001");
      ArticleImage image2 =
          new ArticleImage(article2, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThat(image1).isNotEqualTo(image2);
    }

    @Test
    @DisplayName("정상: 자기 자신과는 동등하다")
    void equals_Self_ReturnsTrue() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage image =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThat(image).isEqualTo(image);
    }

    @Test
    @DisplayName("정상: null과는 동등하지 않다")
    void equals_Null_ReturnsFalse() {
      // given
      RegularArticle article = ArticleFixture.createRegularArticle();
      ArticleImage image =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when & then
      assertThat(image).isNotEqualTo(null);
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
      ArticleImage image =
          new ArticleImage(article, 1L, "https://example.com/image1.jpg", "IMG_001");

      // when
      String result = image.toString();

      // then
      assertThat(result).contains("articleId='" + article.getId() + "'");
      assertThat(result).contains("sequence=1");
      assertThat(result).contains("imageId='IMG_001'");
      assertThat(result).contains("imageUrl='https://example.com/image1.jpg'");
    }
  }
}

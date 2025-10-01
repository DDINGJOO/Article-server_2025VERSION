package com.teambind.articleserver.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.exceptions.CustomException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ArticleResponseTest {

  @Test
  @DisplayName("fromEntity 정상: 엔티티를 응답 DTO로 변환한다 (맵 구조 반영)")
  void fromEntity_success() {
    // given
    Article article =
        Article.builder()
            .id("ART-200")
            .title("테스트 제목")
            .content("테스트 내용")
            .writerId("writer-9")
            .board(Board.builder().id(3L).boardName("Q&A").build())
            .updatedAt(LocalDateTime.now())
            .images(new ArrayList<>())
            .keywords(new ArrayList<>())
            .build();

    // imageId가 맵의 키로 사용되므로 imageId가 설정되는 addImage 오버로드 사용
    article.addImage("https://static.example/1.png", article.getId());

    article.addKeywords(
        List.of(
            Keyword.builder().id(4L).keyword("버그").build(),
            Keyword.builder().id(6L).keyword("답변").build()));

    // when
    ArticleResponse response = ArticleResponse.fromEntity(article);

    // then
    assertEquals("ART-200", response.getArticleId());
    assertEquals("테스트 제목", response.getTitle());
    assertEquals("테스트 내용", response.getContent());

    // board는 id->name 형태의 맵
    assertNotNull(response.getBoard());
    assertEquals("Q&A", response.getBoard().get(3L));

    // imageUrls는 imageId->imageUrl 맵 (여기서는 articleId를 imageId로 사용)
    assertNotNull(response.getImageUrls());
    assertEquals("https://static.example/1.png", response.getImageUrls().get("ART-200"));

    // keywords는 keywordId->keyword 맵
    assertEquals("버그", response.getKeywords().get(4L));
    assertEquals("답변", response.getKeywords().get(6L));

    assertNotNull(response.getLastestUpdateId());
  }

  @Test
  @DisplayName("fromEntity 예외: null 인수 전달시 CustomException 발생")
  void fromEntity_error_null() {
    // when
    Executable call = () -> ArticleResponse.fromEntity(null);

    // then
    assertThrows(CustomException.class, call);
  }
}

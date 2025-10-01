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
  @DisplayName("fromEntity 정상: 엔티티를 응답 DTO로 변환한다")
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

    article.addImage("https://static.example/1.png");
    article.addImage("https://static.example/2.png");
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
    assertNotNull(response.getBoard());
    assertEquals("Q&A", response.getBoard().getBoardName());
    assertEquals("https://static.example/1.png", response.getImageUrls().get(0));
    assertEquals("https://static.example/2.png", response.getImageUrls().get(1));
    assertEquals("버그", response.getKeywords().get(4L));
    assertEquals("답변", response.getKeywords().get(6L));
  }

  @Test
  @DisplayName("fromEntity 예외: null 인수 전달시 NullPointerException 발생")
  void fromEntity_error_null() {
    // when
    Executable call = () -> ArticleResponse.fromEntity(null);

    // then
    assertThrows(CustomException.class, call);
  }
}

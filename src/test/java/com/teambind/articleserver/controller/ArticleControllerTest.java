package com.teambind.articleserver.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ArticleController.class)
class ArticleControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private ArticleCreateService articleCreateService;

  @MockBean private Convertor convertor;

  @Test
  @DisplayName("POST /api/articles 게시글을 생성하고 아이디를 리턴받는다.")
  void createArticle_success() throws Exception {
    // given
    ArticleCreateRequest request = new ArticleCreateRequest();
    request.setTitle("제목");
    request.setContent("내용");
    request.setWriterId("writer-1");
    request.setBoard("공지사항");
    request.setKeywords(List.of("공지", "이벤트"));

    List<Keyword> keywords =
        List.of(
            Keyword.builder().id(1L).keyword("공지").build(),
            Keyword.builder().id(9L).keyword("이벤트").build());
    Board board = Board.builder().id(1L).boardName("공지사항").build();
    Article article = Article.builder().id("ART-001").build();

    Mockito.when(convertor.convertKeywords(anyList())).thenReturn(keywords);
    Mockito.when(convertor.convertBoard(eq("공지사항"))).thenReturn(board);
    Mockito.when(
            articleCreateService.createArticle(
                anyString(), anyString(), anyString(), any(Board.class), anyList()))
        .thenReturn(article);

    // when & then
    mockMvc
        .perform(
            post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("ART-001"));
  }
}

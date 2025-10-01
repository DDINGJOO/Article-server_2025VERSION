package com.teambind.articleserver.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ArticleController.class)
@Import(ArticleControllerTest.TestConfig.class)
class ArticleControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ArticleCreateService articleCreateService;

  @Autowired private Convertor convertor;

  @Autowired private ArticleReadService articleReadService;

  @Test
  @DisplayName("GET /api/articles/{id} 정상: ArticleResponse 반환")
  void fetchArticle_success() throws Exception {
    // given
    String id = "ART-100";
    com.teambind.articleserver.entity.Article article = com.teambind.articleserver.entity.Article.builder()
        .id(id)
        .title("제목")
        .content("내용")
        .writerId("writer-1")
        .board(com.teambind.articleserver.entity.Board.builder().id(2L).boardName("자유게시판").build())
        .updatedAt(java.time.LocalDateTime.now())
        .images(new java.util.ArrayList<>())
        .keywords(new java.util.ArrayList<>())
        .build();
    // 관계 편의 메서드 활용
    article.addImage("https://img/1.png");
    article.addKeywords(java.util.List.of(
        com.teambind.articleserver.entity.Keyword.builder().id(5L).keyword("질문").build(),
        com.teambind.articleserver.entity.Keyword.builder().id(7L).keyword("팁").build()
    ));

    Mockito.when(articleReadService.fetchArticleById(id)).thenReturn(article);

    // when & then
    mockMvc.perform(get("/api/articles/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.articleId").value(id))
        .andExpect(jsonPath("$.title").value("제목"))
        .andExpect(jsonPath("$.board.boardName").value("자유게시판"))
        .andExpect(jsonPath("$.imageUrls[0]").value("https://img/1.png"))
        .andExpect(jsonPath("$.keywords['5']").value("질문"))
        .andExpect(jsonPath("$.keywords['7']").value("팁"));
  }

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

  @Test
  @DisplayName("POST /api/articles 오류: Convertor가 REQUIRED_FIELD_NULL 던지면 400과 메시지를 응답한다")
  void createArticle_error_requiredFieldNull() throws Exception {
    // given
    ArticleCreateRequest request = new ArticleCreateRequest();
    request.setTitle("제목");
    request.setContent("내용");
    request.setWriterId("writer-1");
    request.setBoard(null); // 의도적으로 null
    request.setKeywords(List.of());

    Mockito.when(convertor.convertKeywords(anyList())).thenReturn(List.of());
    Mockito.when(convertor.convertBoard(any()))
        .thenThrow(
            new com.teambind.articleserver.exceptions.CustomException(
                com.teambind.articleserver.exceptions.ErrorCode.REQUIRED_FIELD_NULL));

    // when & then
    mockMvc
        .perform(
            post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("REQUIRED_FIELD_IS_NULL")));
  }

  @Test
  @DisplayName("POST /api/articles 오류: Service가 ARTICLE_NOT_FOUND 던지면 404와 메시지를 응답한다")
  void createArticle_error_articleNotFound() throws Exception {
    // given
    ArticleCreateRequest request = new ArticleCreateRequest();
    request.setTitle("제목");
    request.setContent("내용");
    request.setWriterId("writer-1");
    request.setBoard("공지사항");
    request.setKeywords(List.of("공지"));

    Mockito.when(convertor.convertKeywords(anyList())).thenReturn(List.of());
    Mockito.when(convertor.convertBoard(any()))
        .thenReturn(Board.builder().id(1L).boardName("공지사항").build());
    Mockito.when(
            articleCreateService.createArticle(
                anyString(), anyString(), anyString(), any(Board.class), anyList()))
        .thenThrow(
            new com.teambind.articleserver.exceptions.CustomException(
                com.teambind.articleserver.exceptions.ErrorCode.ARTICLE_NOT_FOUND));

    // when & then
    mockMvc
        .perform(
            post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ARTICLE_NOT_FOUND")));
  }

  @Test
  @DisplayName("GET /api/articles/{id} 오류: ARTICLE_NOT_FOUND → 404")
  void fetchArticle_error_notFound() throws Exception {
    // given
    String id = "NOT-EXIST";
    Mockito.when(articleReadService.fetchArticleById(id))
        .thenThrow(new com.teambind.articleserver.exceptions.CustomException(
            com.teambind.articleserver.exceptions.ErrorCode.ARTICLE_NOT_FOUND));

    // when & then
    mockMvc.perform(get("/api/articles/" + id))
        .andExpect(status().isNotFound())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ARTICLE_NOT_FOUND")));
  }

  @Test
  @DisplayName("GET /api/articles/{id} 오류: ARTICLE_IS_BLOCKED → 400")
  void fetchArticle_error_blocked() throws Exception {
    // given
    String id = "BLOCKED";
    Mockito.when(articleReadService.fetchArticleById(id))
        .thenThrow(new com.teambind.articleserver.exceptions.CustomException(
            com.teambind.articleserver.exceptions.ErrorCode.ARTICLE_IS_BLOCKED));

    // when & then
    mockMvc.perform(get("/api/articles/" + id))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ARTICLE_IS_BLOCKED")));
  }

  @Test
  @DisplayName("GET /api/articles/search 정상: 모든 파라미터 제공 시 Convertor와 Service가 올바르게 호출되고 응답을 반환")
  void searchArticles_success_allParams() throws Exception {
    // given
    java.util.List<com.teambind.articleserver.entity.Keyword> convertedKeywords =
        java.util.List.of(
            com.teambind.articleserver.entity.Keyword.builder().id(10L).keyword("질문").build(),
            com.teambind.articleserver.entity.Keyword.builder().id(11L).keyword("팁").build());
    com.teambind.articleserver.entity.Board convertedBoard =
        com.teambind.articleserver.entity.Board.builder().id(2L).boardName("자유게시판").build();

    Mockito.when(convertor.convertKeywords(anyList())).thenReturn(convertedKeywords);
    Mockito.when(convertor.convertBoard(eq("자유게시판"))).thenReturn(convertedBoard);

    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    com.teambind.articleserver.dto.response.ArticleResponse item =
        com.teambind.articleserver.dto.response.ArticleResponse.builder()
            .articleId("ART-010")
            .title("t")
            .content("c")
            .writerId("writer-9")
            .board(convertedBoard)
            .LastestUpdateId(now)
            .imageUrls((java.util.Map<String, String>) List.of())
            .keywords(java.util.Map.of())
            .build();

    com.teambind.articleserver.dto.response.ArticleCursorPageResponse page =
        com.teambind.articleserver.dto.response.ArticleCursorPageResponse.builder()
            .items(java.util.List.of(item))
            .nextCursorUpdatedAt(now)
            .nextCursorId("ART-010")
            .hasNext(true)
            .size(10)
            .build();

    Mockito.when(articleReadService.searchArticles(any(), any())).thenReturn(page);

    // when
    mockMvc
        .perform(
            get("/api/articles/search")
                .param("size", "10")
                .param("cursorId", "ART-005")
                .param("board", "자유게시판")
                .param("keyword", "질문")
                .param("keyword", "팁")
                .param("title", "테스트")
                .param("content", "내용")
                .param("writerIds", "writer-1")
                .param("writerIds", "writer-2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.nextCursorId").value("ART-010"))
        .andExpect(jsonPath("$.items[0].articleId").value("ART-010"))
        .andExpect(jsonPath("$.items[0].board.boardName").value("자유게시판"));

    // then - verify interactions and captured arguments
    org.mockito.ArgumentCaptor<com.teambind.articleserver.dto.condition.ArticleSearchCriteria>
        criteriaCaptor =
            org.mockito.ArgumentCaptor.forClass(
                com.teambind.articleserver.dto.condition.ArticleSearchCriteria.class);
    org.mockito.ArgumentCaptor<com.teambind.articleserver.dto.request.ArticleCursorPageRequest>
        pageReqCaptor =
            org.mockito.ArgumentCaptor.forClass(
                com.teambind.articleserver.dto.request.ArticleCursorPageRequest.class);

    Mockito.verify(convertor, Mockito.times(1)).convertBoard("자유게시판");
    Mockito.verify(convertor, Mockito.times(1)).convertKeywords(anyList());
    Mockito.verify(articleReadService)
        .searchArticles(criteriaCaptor.capture(), pageReqCaptor.capture());

    com.teambind.articleserver.dto.condition.ArticleSearchCriteria captured =
        criteriaCaptor.getValue();
    org.junit.jupiter.api.Assertions.assertEquals(convertedBoard, captured.getBoard());
    org.junit.jupiter.api.Assertions.assertEquals(2, captured.getKeywords().size());
    org.junit.jupiter.api.Assertions.assertEquals("테스트", captured.getTitle());
    org.junit.jupiter.api.Assertions.assertEquals("내용", captured.getContent());
    org.junit.jupiter.api.Assertions.assertEquals(
        java.util.List.of("writer-1", "writer-2"), captured.getWriterId());
    org.junit.jupiter.api.Assertions.assertEquals(
        com.teambind.articleserver.entity.enums.Status.ACTIVE, captured.getStatus());

    com.teambind.articleserver.dto.request.ArticleCursorPageRequest capturedPageReq =
        pageReqCaptor.getValue();
    org.junit.jupiter.api.Assertions.assertEquals(10, capturedPageReq.getSize());
    org.junit.jupiter.api.Assertions.assertEquals("ART-005", capturedPageReq.getCursorId());
  }



  @Test
  @DisplayName("GET /api/articles/search 오류: Service에서 예외 발생 시 GlobalExceptionHandler 매핑 확인")
  void searchArticles_error_serviceThrows() throws Exception {
    // given
    Mockito.when(articleReadService.searchArticles(any(), any()))
        .thenThrow(
            new com.teambind.articleserver.exceptions.CustomException(
                com.teambind.articleserver.exceptions.ErrorCode.REQUIRED_FIELD_NULL));

    // when & then
    mockMvc
        .perform(get("/api/articles/search").param("size", "20"))
        .andExpect(status().isBadRequest())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("REQUIRED_FIELD_IS_NULL")));
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    ArticleCreateService articleCreateService() {
      return org.mockito.Mockito.mock(ArticleCreateService.class);
    }
    @Bean
    ArticleReadService articleReadService() {
      return org.mockito.Mockito.mock(ArticleReadService.class);
    }
    @Bean
    Convertor convertor() {
      return org.mockito.Mockito.mock(Convertor.class);
    }
  }
}

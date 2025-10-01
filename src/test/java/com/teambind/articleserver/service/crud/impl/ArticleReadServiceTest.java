package com.teambind.articleserver.service.crud.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.repository.ArticleRepository;
import com.teambind.articleserver.repository.ArticleRepositoryCustomImpl;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleReadServiceTest {

  @Mock ArticleRepository articleRepository;
  @Mock ArticleRepositoryCustomImpl articleRepositoryCustom;
  @Mock Convertor convertor; // currently unused in service, but kept for constructor

  @InjectMocks ArticleReadService articleReadService;

  private Article activeArticle;
  private Article blockedArticle;
  private Article deletedArticle;

  @BeforeEach
  void setUp() {
    activeArticle =
        Article.builder()
            .id("ART-1")
            .title("t1")
            .content("c1")
            .writerId("w1")
            .board(Board.builder().id(1L).boardName("공지사항").build())
            .status(Status.ACTIVE)
            .createdAt(LocalDateTime.now().minusDays(1))
            .updatedAt(LocalDateTime.now())
            .keywords(new ArrayList<>())
            .images(new ArrayList<>())
            .build();

    blockedArticle =
        Article.builder()
            .id("ART-B")
            .title("tb")
            .content("cb")
            .writerId("wb")
            .board(Board.builder().id(2L).boardName("자유게시판").build())
            .status(Status.BLOCKED)
            .createdAt(LocalDateTime.now().minusDays(2))
            .updatedAt(LocalDateTime.now().minusDays(1))
            .build();

    deletedArticle =
        Article.builder()
            .id("ART-D")
            .title("td")
            .content("cd")
            .writerId("wd")
            .board(Board.builder().id(3L).boardName("Q&A").build())
            .status(Status.DELETED)
            .createdAt(LocalDateTime.now().minusDays(3))
            .updatedAt(LocalDateTime.now().minusDays(2))
            .build();
  }

  @Test
  @DisplayName("fetchArticleById: ACTIVE 글은 정상 반환")
  void fetchArticleById_active() {
    when(articleRepository.findById("ART-1")).thenReturn(Optional.of(activeArticle));

    Article found = articleReadService.fetchArticleById("ART-1");

    assertAll(
        () -> assertNotNull(found),
        () -> assertEquals("ART-1", found.getId()),
        () -> assertEquals(Status.ACTIVE, found.getStatus()));
  }

  @Test
  @DisplayName("fetchArticleById: 미존재 → ARTICLE_NOT_FOUND")
  void fetchArticleById_notFound() {
    when(articleRepository.findById("NONE")).thenReturn(Optional.empty());

    assertThrows(CustomException.class, () -> articleReadService.fetchArticleById("NONE"));
  }

  @Test
  @DisplayName("fetchArticleById: BLOCKED → ARTICLE_IS_BLOCKED")
  void fetchArticleById_blocked() {
    when(articleRepository.findById("ART-B")).thenReturn(Optional.of(blockedArticle));

    assertThrows(CustomException.class, () -> articleReadService.fetchArticleById("ART-B"));
  }

  @Test
  @DisplayName("fetchArticleById: DELETED → ARTICLE_NOT_FOUND")
  void fetchArticleById_deleted() {
    when(articleRepository.findById("ART-D")).thenReturn(Optional.of(deletedArticle));

    assertThrows(CustomException.class, () -> articleReadService.fetchArticleById("ART-D"));
  }

  @Test
  @DisplayName("searchArticles: cursorId 없이 호출해도 오류 없이 수행되고 size 기본값 20 적용")
  void searchArticles_noCursor_okWithDefaultSize() {
    // given
    ArticleSearchCriteria criteria =
        ArticleSearchCriteria.builder()
            .status(null) // default filter should exclude BLOCKED/DELETED in repo
            .build();

    ArticleCursorPageRequest pageReq =
        ArticleCursorPageRequest.builder()
            .size(null) // default to 20
            .cursorId(null)
            .cursorUpdatedAt(null)
            .build();

    // repository returns fewer than requested to indicate no next page
    when(articleRepositoryCustom.searchByCursor(eq(criteria), isNull(), isNull(), eq(21)))
        .thenReturn(List.of(activeArticle));

    // when
    ArticleCursorPageResponse resp = articleReadService.searchArticles(criteria, pageReq);

    // then
    assertAll(
        () -> assertThat(resp.getItems()).hasSize(1),
        () -> assertThat(resp.isHasNext()).isFalse(),
        () -> assertThat(resp.getSize()).isEqualTo(20),
        () -> assertThat(resp.getNextCursorId()).isEqualTo("ART-1"));

    verify(articleRepositoryCustom, times(1))
        .searchByCursor(eq(criteria), isNull(), isNull(), eq(21));
    // must NOT attempt to read cursor by null id
    verify(articleRepository, never()).findById(isNull());
  }

  @Test
  @DisplayName("searchArticles: cursorId만 주면 updatedAt을 내부에서 조회하여 페이징")
  void searchArticles_withCursorId_derivesUpdatedAt() {
    // given
    ArticleSearchCriteria criteria = ArticleSearchCriteria.builder().build();

    ArticleCursorPageRequest pageReq =
        ArticleCursorPageRequest.builder().size(10).cursorId("ART-1").cursorUpdatedAt(null).build();

    // fetch cursor article for updatedAt
    when(articleRepository.findById("ART-1")).thenReturn(Optional.of(activeArticle));

    // repository returns size+1 to indicate hasNext
    List<Article> fetched = new ArrayList<>();
    for (int i = 0; i < 11; i++) {
      fetched.add(
          Article.builder().id("ART-" + (100 - i)).updatedAt(activeArticle.getUpdatedAt()).build());
    }
    when(articleRepositoryCustom.searchByCursor(
            eq(criteria), eq(activeArticle.getUpdatedAt()), eq("ART-1"), eq(11)))
        .thenReturn(fetched);

    // when
    ArticleCursorPageResponse resp = articleReadService.searchArticles(criteria, pageReq);

    // then
    assertAll(
        () -> assertThat(resp.getItems()).hasSize(10),
        () -> assertThat(resp.isHasNext()).isTrue(),
        () -> assertThat(resp.getSize()).isEqualTo(10),
        () -> assertNotNull(resp.getNextCursorId()),
        () -> assertNotNull(resp.getNextCursorUpdatedAt()));

    verify(articleRepository, times(1)).findById("ART-1");
    verify(articleRepositoryCustom, times(1))
        .searchByCursor(eq(criteria), eq(activeArticle.getUpdatedAt()), eq("ART-1"), eq(11));
  }

  @Test
  @DisplayName("searchArticles: 검색 조건(보드/키워드/제목/내용/작성자/상태)이 변경 없이 Repository에 전달된다")
  void searchArticles_passesCriteriaThrough() {
    // given criteria fully populated
    Board board = Board.builder().id(2L).boardName("자유게시판").build();
    List<Keyword> keywords =
        List.of(
            Keyword.builder().id(5L).keyword("질문").build(),
            Keyword.builder().id(7L).keyword("팁").build());

    ArticleSearchCriteria criteria =
        ArticleSearchCriteria.builder()
            .board(board)
            .keywords(keywords)
            .title("테스트")
            .content("내용")
            .writerId(List.of("w1", "w2"))
            .status(Status.ACTIVE)
            .build();

    ArticleCursorPageRequest pageReq = ArticleCursorPageRequest.builder().size(5).build();

    when(articleRepositoryCustom.searchByCursor(eq(criteria), isNull(), isNull(), eq(6)))
        .thenReturn(List.of());

    // when
    ArticleCursorPageResponse resp = articleReadService.searchArticles(criteria, pageReq);

    // then
    assertAll(
        () -> assertThat(resp.getItems()).isEmpty(),
        () -> assertThat(resp.isHasNext()).isFalse(),
        () -> assertThat(resp.getSize()).isEqualTo(5));

    // verify criteria was passed as-is
    verify(articleRepositoryCustom, times(1))
        .searchByCursor(eq(criteria), isNull(), isNull(), eq(6));
  }

  @Test
  @DisplayName("searchArticles: cursorId가 삭제/차단 게시글이면 예외")
  void searchArticles_cursorIdInvalid_throws() {
    // given
    ArticleSearchCriteria criteria = ArticleSearchCriteria.builder().build();
    ArticleCursorPageRequest pageReq =
        ArticleCursorPageRequest.builder().size(10).cursorId("ART-D").build();

    when(articleRepository.findById("ART-D")).thenReturn(Optional.of(deletedArticle));

    // when & then
    assertThrows(CustomException.class, () -> articleReadService.searchArticles(criteria, pageReq));
    verify(articleRepository, times(1)).findById("ART-D");
    verify(articleRepositoryCustom, never()).searchByCursor(any(), any(), any(), anyInt());
  }
}

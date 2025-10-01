package com.teambind.articleserver.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.teambind.articleserver.config.CustomConfig;
import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.entity.enums.Status;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CustomConfig.class, ArticleRepositoryCustomImpl.class})
class ArticleRepositoryCustomImplTest {

  @Autowired private ArticleRepositoryCustomImpl articleRepositoryCustom;

  @PersistenceContext private EntityManager em;

  private Board freeBoard;
  private Keyword kwQuestion; // "질문"
  private Keyword kwTip; // "팁"

  @BeforeEach
  void setUp() {
    // boards
    freeBoard = Board.builder().boardName("자유게시판").build();
    em.persist(freeBoard);

    // keywords
    kwQuestion = Keyword.builder().id(100L).keyword("질문").build();
    kwTip = Keyword.builder().id(101L).keyword("팁").build();
    em.persist(kwQuestion);
    em.persist(kwTip);

    // articles: create 5 ACTIVE, 1 BLOCKED, 1 DELETED
    // We want ordering by updatedAt DESC then id DESC when cursor is null
    LocalDateTime base = LocalDateTime.now().withNano(0);

    // A5 most recent
    persistArticleWithKeywords(
        "ART-005", base.plusSeconds(5), Status.ACTIVE, freeBoard, kwQuestion);
    persistArticleWithKeywords(
        "ART-004", base.plusSeconds(4), Status.ACTIVE, freeBoard, kwQuestion, kwTip);
    persistArticleWithKeywords(
        "ART-003",
        base.plusSeconds(3),
        Status.ACTIVE,
        freeBoard,
        kwTip); // won't match kwQuestion-only
    persistArticleWithKeywords(
        "ART-002", base.plusSeconds(2), Status.ACTIVE, freeBoard, kwQuestion);
    persistArticleWithKeywords(
        "ART-001", base.plusSeconds(1), Status.ACTIVE, freeBoard, kwQuestion);

    // Newer but BLOCKED/DELETED should be filtered out by default statusFilter
    persistArticleWithKeywords(
        "ART-BLK", base.plusSeconds(6), Status.BLOCKED, freeBoard, kwQuestion);
    persistArticleWithKeywords(
        "ART-DEL", base.plusSeconds(7), Status.DELETED, freeBoard, kwQuestion);

    em.flush();
    em.clear();
  }

  private void persistArticleWithKeywords(
      String id, LocalDateTime updatedAt, Status status, Board board, Keyword... keywords) {
    Article a =
        Article.builder()
            .id(id)
            .title("테스트 제목 " + id)
            .content("내용 " + id)
            .writerId("writer-1")
            .status(status)
            .createdAt(updatedAt.minusMinutes(10))
            .updatedAt(updatedAt)
            .board(board)
            .build();

    // use convenience method to add mappings, then persist article (cascade persists mappings)
    if (keywords != null && keywords.length > 0) {
      a.addKeywords(java.util.Arrays.asList(keywords));
    }

    em.persist(a);
  }

  @Test
  @DisplayName("cursorId가 null일 때: 조건(보드+키워드)에 맞는 최신 게시물부터 정렬되어 size만큼 반환")
  void searchByCursor_firstPage_returnsLatestMatching() {
    // given: criteria requires board = 자유게시판 and keyword contains '질문'
    ArticleSearchCriteria criteria =
        ArticleSearchCriteria.builder().board(freeBoard).keywords(List.of(kwQuestion)).build();

    int size = 3;

    // when
    List<Article> result = articleRepositoryCustom.searchByCursor(criteria, null, null, size);

    // then: 'ART-005', 'ART-004', 'ART-002' expected (ART-003 excluded; ART-001 next)
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getId()).isEqualTo("ART-005");
    assertThat(result.get(1).getId()).isEqualTo("ART-004");
    assertThat(result.get(2).getId()).isEqualTo("ART-002");

    // ensure blocked/deleted newer rows are excluded by default filter
    assertThat(result).noneMatch(a -> a.getId().equals("ART-BLK") || a.getId().equals("ART-DEL"));

    // verify ordering strictly by updatedAt DESC then id DESC when updatedAt same
    // (ART-004 and ART-005 have different updatedAt so main ordering suffices here)
  }
}

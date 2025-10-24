package com.teambind.articleserver.repository;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.config.TestIdGeneratorConfig;
import com.teambind.articleserver.config.TestIdGeneratorConfig.TestIdGenerator;
import com.teambind.articleserver.entity.articleType.NoticeArticle;
import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.enums.Status;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestIdGeneratorConfig.class)
@DisplayName("NoticeArticleRepository 테스트")
class NoticeArticleRepositoryTest {

  @Autowired private NoticeArticleRepository noticeArticleRepository;

  @Autowired private BoardRepository boardRepository;

  @Autowired private TestIdGenerator idGenerator;

  private Board board;

  @BeforeEach
  void setUp() {
    board = Board.builder().name("공지게시판").build();
    boardRepository.save(board);
  }

  @AfterEach
  void tearDown() {
    noticeArticleRepository.deleteAll();
    boardRepository.deleteAll();
  }

  @Nested
  @DisplayName("save() 테스트")
  class SaveTest {

    @Test
    @DisplayName("정상: 공지사항을 저장할 수 있다")
    void save_Success() {
      // given
      NoticeArticle notice =
          NoticeArticle.builder()
              .title("중요 공지")
              .content("공지 내용")
              .writerId("admin")
              .board(board)
              .build();
      notice.setId(idGenerator.generate());

      // when
      NoticeArticle savedNotice = noticeArticleRepository.save(notice);

      // then
      assertThat(savedNotice.getId()).isNotNull();
      assertThat(savedNotice.getTitle()).isEqualTo("중요 공지");
      assertThat(savedNotice.getContent()).isEqualTo("공지 내용");
      assertThat(savedNotice.getWriterId()).isEqualTo("admin");
      assertThat(savedNotice.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("정상: 여러 공지사항을 저장할 수 있다")
    void saveAll_Success() {
      // given
      NoticeArticle notice1 =
          NoticeArticle.builder()
              .title("공지1")
              .content("내용1")
              .writerId("admin")
              .board(board)
              .build();
      notice1.setId(idGenerator.generate());

      NoticeArticle notice2 =
          NoticeArticle.builder()
              .title("공지2")
              .content("내용2")
              .writerId("admin")
              .board(board)
              .build();
      notice2.setId(idGenerator.generate());

      NoticeArticle notice3 =
          NoticeArticle.builder()
              .title("공지3")
              .content("내용3")
              .writerId("admin")
              .board(board)
              .build();
      notice3.setId(idGenerator.generate());

      // when
      List<NoticeArticle> savedNotices =
          noticeArticleRepository.saveAll(List.of(notice1, notice2, notice3));

      // then
      assertThat(savedNotices).hasSize(3);
      assertThat(savedNotices).extracting("title").containsExactly("공지1", "공지2", "공지3");
    }
  }

  @Nested
  @DisplayName("findById() 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("정상: ID로 공지사항을 조회할 수 있다")
    void findById_Success() {
      // given
      NoticeArticle notice =
          NoticeArticle.builder().title("공지").content("내용").writerId("admin").board(board).build();
      notice.setId(idGenerator.generate());
      NoticeArticle savedNotice = noticeArticleRepository.save(notice);

      // when
      Optional<NoticeArticle> foundNotice = noticeArticleRepository.findById(savedNotice.getId());

      // then
      assertThat(foundNotice).isPresent();
      assertThat(foundNotice.get().getTitle()).isEqualTo("공지");
    }

    @Test
    @DisplayName("예외: 존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    void findById_NotFound_ReturnsEmpty() {
      // when
      Optional<NoticeArticle> foundNotice = noticeArticleRepository.findById("non-existent-id");

      // then
      assertThat(foundNotice).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByStatusOrderByCreatedAtDesc() 테스트")
  class FindByStatusTest {

    @Test
    @DisplayName("정상: 상태로 공지사항을 조회할 수 있다")
    void findByStatus_Success() {
      // given
      NoticeArticle activeNotice =
          NoticeArticle.builder()
              .title("활성공지")
              .content("내용")
              .writerId("admin")
              .board(board)
              .status(Status.ACTIVE)
              .build();
      activeNotice.setId(idGenerator.generate());

      NoticeArticle deletedNotice =
          NoticeArticle.builder()
              .title("삭제공지")
              .content("내용")
              .writerId("admin")
              .board(board)
              .status(Status.DELETED)
              .build();
      deletedNotice.setId(idGenerator.generate());

      noticeArticleRepository.saveAll(List.of(activeNotice, deletedNotice));

      Pageable pageable = PageRequest.of(0, 10);

      // when
      Page<NoticeArticle> result =
          noticeArticleRepository.findByStatusOrderByCreatedAtDesc(Status.ACTIVE, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getTitle()).isEqualTo("활성공지");
    }

    @Test
    @DisplayName("정상: 생성일 내림차순으로 정렬된다")
    void findByStatus_OrderByCreatedAtDesc() {
      // given
      NoticeArticle notice1 =
          NoticeArticle.builder().title("첫번째").content("내용").writerId("admin").board(board).build();
      notice1.setId(idGenerator.generate());

      NoticeArticle notice2 =
          NoticeArticle.builder().title("두번째").content("내용").writerId("admin").board(board).build();
      notice2.setId(idGenerator.generate());

      NoticeArticle notice3 =
          NoticeArticle.builder().title("세번째").content("내용").writerId("admin").board(board).build();
      notice3.setId(idGenerator.generate());

      noticeArticleRepository.save(notice1);
      noticeArticleRepository.save(notice2);
      noticeArticleRepository.save(notice3);

      Pageable pageable = PageRequest.of(0, 10);

      // when
      Page<NoticeArticle> result =
          noticeArticleRepository.findByStatusOrderByCreatedAtDesc(Status.ACTIVE, pageable);

      // then
      assertThat(result.getContent()).hasSize(3);
      // 최신순 정렬이므로 세번째가 먼저
      assertThat(result.getContent().get(0).getTitle()).isEqualTo("세번째");
      assertThat(result.getContent().get(1).getTitle()).isEqualTo("두번째");
      assertThat(result.getContent().get(2).getTitle()).isEqualTo("첫번째");
    }

    @Test
    @DisplayName("엣지: 해당 상태의 공지사항이 없으면 빈 페이지를 반환한다")
    void findByStatus_NoResults_ReturnsEmptyPage() {
      // given
      Pageable pageable = PageRequest.of(0, 10);

      // when
      Page<NoticeArticle> result =
          noticeArticleRepository.findByStatusOrderByCreatedAtDesc(Status.ACTIVE, pageable);

      // then
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }
  }

  @Nested
  @DisplayName("update() 테스트")
  class UpdateTest {

    @Test
    @DisplayName("정상: 공지사항을 수정할 수 있다")
    void update_Success() {
      // given
      NoticeArticle notice =
          NoticeArticle.builder()
              .title("원래제목")
              .content("원래내용")
              .writerId("admin")
              .board(board)
              .build();
      notice.setId(idGenerator.generate());
      NoticeArticle savedNotice = noticeArticleRepository.save(notice);

      // when
      savedNotice.updateContent("수정제목", "수정내용");
      NoticeArticle updatedNotice = noticeArticleRepository.save(savedNotice);

      // then
      assertThat(updatedNotice.getTitle()).isEqualTo("수정제목");
      assertThat(updatedNotice.getContent()).isEqualTo("수정내용");
    }

    @Test
    @DisplayName("정상: 상태를 변경할 수 있다")
    void update_Status_Success() {
      // given
      NoticeArticle notice =
          NoticeArticle.builder().title("공지").content("내용").writerId("admin").board(board).build();
      notice.setId(idGenerator.generate());
      NoticeArticle savedNotice = noticeArticleRepository.save(notice);

      // when
      savedNotice.delete();
      noticeArticleRepository.save(savedNotice);

      // then
      NoticeArticle foundNotice = noticeArticleRepository.findById(savedNotice.getId()).get();
      assertThat(foundNotice.getStatus()).isEqualTo(Status.DELETED);
    }
  }

  @Nested
  @DisplayName("delete() 테스트")
  class DeleteTest {

    @Test
    @DisplayName("정상: 공지사항을 삭제할 수 있다")
    void delete_Success() {
      // given
      NoticeArticle notice =
          NoticeArticle.builder().title("공지").content("내용").writerId("admin").board(board).build();
      notice.setId(idGenerator.generate());
      NoticeArticle savedNotice = noticeArticleRepository.save(notice);

      // when
      noticeArticleRepository.delete(savedNotice);

      // then
      Optional<NoticeArticle> foundNotice = noticeArticleRepository.findById(savedNotice.getId());
      assertThat(foundNotice).isEmpty();
    }

    @Test
    @DisplayName("정상: ID로 공지사항을 삭제할 수 있다")
    void deleteById_Success() {
      // given
      NoticeArticle notice =
          NoticeArticle.builder().title("공지").content("내용").writerId("admin").board(board).build();
      notice.setId(idGenerator.generate());
      NoticeArticle savedNotice = noticeArticleRepository.save(notice);

      // when
      noticeArticleRepository.deleteById(savedNotice.getId());

      // then
      Optional<NoticeArticle> foundNotice = noticeArticleRepository.findById(savedNotice.getId());
      assertThat(foundNotice).isEmpty();
    }
  }

  @Nested
  @DisplayName("count() 테스트")
  class CountTest {

    @Test
    @DisplayName("정상: 공지사항 개수를 조회할 수 있다")
    void count_Success() {
      // given
      NoticeArticle notice1 =
          NoticeArticle.builder().title("공지1").content("내용").writerId("admin").board(board).build();
      notice1.setId(idGenerator.generate());

      NoticeArticle notice2 =
          NoticeArticle.builder().title("공지2").content("내용").writerId("admin").board(board).build();
      notice2.setId(idGenerator.generate());

      noticeArticleRepository.saveAll(List.of(notice1, notice2));

      // when
      long count = noticeArticleRepository.count();

      // then
      assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("엣지: 공지사항이 없으면 0을 반환한다")
    void count_Empty_ReturnsZero() {
      // when
      long count = noticeArticleRepository.count();

      // then
      assertThat(count).isZero();
    }
  }

  @Nested
  @DisplayName("연관관계 테스트")
  class RelationshipTest {

    @Test
    @DisplayName("정상: Board와의 다대일 관계가 작동한다")
    void relationship_WithBoard_Success() {
      // given
      NoticeArticle notice =
          NoticeArticle.builder().title("공지").content("내용").writerId("admin").board(board).build();
      notice.setId(idGenerator.generate());
      NoticeArticle savedNotice = noticeArticleRepository.save(notice);

      // when
      NoticeArticle foundNotice = noticeArticleRepository.findById(savedNotice.getId()).get();

      // then
      assertThat(foundNotice.getBoard()).isNotNull();
      assertThat(foundNotice.getBoard().getId()).isEqualTo(board.getId());
      assertThat(foundNotice.getBoard().getName()).isEqualTo("공지게시판");
    }
  }

  @Nested
  @DisplayName("페이징 테스트")
  class PagingTest {

    @Test
    @DisplayName("정상: 페이징이 올바르게 작동한다")
    void paging_Success() {
      // given
      for (int i = 1; i <= 25; i++) {
        NoticeArticle notice =
            NoticeArticle.builder()
                .title("공지" + i)
                .content("내용")
                .writerId("admin")
                .board(board)
                .build();
        notice.setId(idGenerator.generate());
        noticeArticleRepository.save(notice);
      }

      Pageable pageable = PageRequest.of(0, 10);

      // when
      Page<NoticeArticle> firstPage =
          noticeArticleRepository.findByStatusOrderByCreatedAtDesc(Status.ACTIVE, pageable);

      // then
      assertThat(firstPage.getContent()).hasSize(10);
      assertThat(firstPage.getTotalElements()).isEqualTo(25);
      assertThat(firstPage.getTotalPages()).isEqualTo(3);
      assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    @DisplayName("정상: 두 번째 페이지를 조회할 수 있다")
    void paging_SecondPage_Success() {
      // given
      for (int i = 1; i <= 25; i++) {
        NoticeArticle notice =
            NoticeArticle.builder()
                .title("공지" + i)
                .content("내용")
                .writerId("admin")
                .board(board)
                .build();
        notice.setId(idGenerator.generate());
        noticeArticleRepository.save(notice);
      }

      Pageable pageable = PageRequest.of(1, 10);

      // when
      Page<NoticeArticle> secondPage =
          noticeArticleRepository.findByStatusOrderByCreatedAtDesc(Status.ACTIVE, pageable);

      // then
      assertThat(secondPage.getContent()).hasSize(10);
      assertThat(secondPage.getNumber()).isEqualTo(1);
      assertThat(secondPage.hasNext()).isTrue();
      assertThat(secondPage.hasPrevious()).isTrue();
    }
  }
}

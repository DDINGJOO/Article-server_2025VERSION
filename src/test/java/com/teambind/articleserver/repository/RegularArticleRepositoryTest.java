package com.teambind.articleserver.repository;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.config.TestIdGeneratorConfig;
import com.teambind.articleserver.config.TestIdGeneratorConfig.TestIdGenerator;
import com.teambind.articleserver.entity.articleType.RegularArticle;
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
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestIdGeneratorConfig.class)
@DisplayName("RegularArticleRepository 테스트")
class RegularArticleRepositoryTest {

  @Autowired private RegularArticleRepository regularArticleRepository;

  @Autowired private BoardRepository boardRepository;

  @Autowired private TestIdGenerator idGenerator;

  private Board board;

  @BeforeEach
  void setUp() {
    board = Board.builder().name("자유게시판").build();
    boardRepository.save(board);
  }

  @AfterEach
  void tearDown() {
    regularArticleRepository.deleteAll();
    boardRepository.deleteAll();
  }

  @Nested
  @DisplayName("save() 테스트")
  class SaveTest {

    @Test
    @DisplayName("정상: 일반글사항을 저장할 수 있다")
    void save_Success() {
      // given
      RegularArticle regular =
          RegularArticle.builder()
              .title("중요 일반글")
              .content("일반글 내용")
              .writerId("admin")
              .board(board)
              .build();
      regular.setId(idGenerator.generate());

      // when
      RegularArticle savedNotice = regularArticleRepository.save(regular);

      // then
      assertThat(savedNotice.getId()).isNotNull();
      assertThat(savedNotice.getTitle()).isEqualTo("중요 일반글");
      assertThat(savedNotice.getContent()).isEqualTo("일반글 내용");
      assertThat(savedNotice.getWriterId()).isEqualTo("admin");
      assertThat(savedNotice.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("정상: 여러 일반글사항을 저장할 수 있다")
    void saveAll_Success() {
      // given
      RegularArticle regular1 =
          RegularArticle.builder()
              .title("일반글1")
              .content("내용1")
              .writerId("admin")
              .board(board)
              .build();
      regular1.setId(idGenerator.generate());

      RegularArticle regular2 =
          RegularArticle.builder()
              .title("일반글2")
              .content("내용2")
              .writerId("admin")
              .board(board)
              .build();
      regular2.setId(idGenerator.generate());

      RegularArticle regular3 =
          RegularArticle.builder()
              .title("일반글3")
              .content("내용3")
              .writerId("admin")
              .board(board)
              .build();
      regular3.setId(idGenerator.generate());

      // when
      List<RegularArticle> savedNotices =
          regularArticleRepository.saveAll(List.of(regular1, regular2, regular3));

      // then
      assertThat(savedNotices).hasSize(3);
      assertThat(savedNotices).extracting("title").containsExactly("일반글1", "일반글2", "일반글3");
    }
  }

  @Nested
  @DisplayName("findById() 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("정상: ID로 일반글사항을 조회할 수 있다")
    void findById_Success() {
      // given
      RegularArticle regular =
          RegularArticle.builder()
              .title("일반글")
              .content("내용")
              .writerId("admin")
              .board(board)
              .build();
      regular.setId(idGenerator.generate());
      RegularArticle savedNotice = regularArticleRepository.save(regular);

      // when
      Optional<RegularArticle> foundNotice = regularArticleRepository.findById(savedNotice.getId());

      // then
      assertThat(foundNotice).isPresent();
      assertThat(foundNotice.get().getTitle()).isEqualTo("일반글");
    }

    @Test
    @DisplayName("예외: 존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    void findById_NotFound_ReturnsEmpty() {
      // when
      Optional<RegularArticle> foundNotice = regularArticleRepository.findById("non-existent-id");

      // then
      assertThat(foundNotice).isEmpty();
    }
  }

  @Nested
  @DisplayName("update() 테스트")
  class UpdateTest {

    @Test
    @DisplayName("정상: 일반글사항을 수정할 수 있다")
    void update_Success() {
      // given
      RegularArticle regular =
          RegularArticle.builder()
              .title("원래제목")
              .content("원래내용")
              .writerId("admin")
              .board(board)
              .build();
      regular.setId(idGenerator.generate());
      RegularArticle savedNotice = regularArticleRepository.save(regular);

      // when
      savedNotice.updateContent("수정제목", "수정내용");
      RegularArticle updatedNotice = regularArticleRepository.save(savedNotice);

      // then
      assertThat(updatedNotice.getTitle()).isEqualTo("수정제목");
      assertThat(updatedNotice.getContent()).isEqualTo("수정내용");
    }

    @Test
    @DisplayName("정상: 상태를 변경할 수 있다")
    void update_Status_Success() {
      // given
      RegularArticle regular =
          RegularArticle.builder()
              .title("일반글")
              .content("내용")
              .writerId("admin")
              .board(board)
              .build();
      regular.setId(idGenerator.generate());
      RegularArticle savedNotice = regularArticleRepository.save(regular);

      // when
      savedNotice.delete();
      regularArticleRepository.save(savedNotice);

      // then
      RegularArticle foundNotice = regularArticleRepository.findById(savedNotice.getId()).get();
      assertThat(foundNotice.getStatus()).isEqualTo(Status.DELETED);
    }
  }

  @Nested
  @DisplayName("delete() 테스트")
  class DeleteTest {

    @Test
    @DisplayName("정상: 일반글사항을 삭제할 수 있다")
    void delete_Success() {
      // given
      RegularArticle regular =
          RegularArticle.builder()
              .title("일반글")
              .content("내용")
              .writerId("admin")
              .board(board)
              .build();
      regular.setId(idGenerator.generate());
      RegularArticle savedNotice = regularArticleRepository.save(regular);

      // when
      regularArticleRepository.delete(savedNotice);

      // then
      Optional<RegularArticle> foundNotice = regularArticleRepository.findById(savedNotice.getId());
      assertThat(foundNotice).isEmpty();
    }

    @Test
    @DisplayName("정상: ID로 일반글사항을 삭제할 수 있다")
    void deleteById_Success() {
      // given
      RegularArticle regular =
          RegularArticle.builder()
              .title("일반글")
              .content("내용")
              .writerId("admin")
              .board(board)
              .build();
      regular.setId(idGenerator.generate());
      RegularArticle savedNotice = regularArticleRepository.save(regular);

      // when
      regularArticleRepository.deleteById(savedNotice.getId());

      // then
      Optional<RegularArticle> foundNotice = regularArticleRepository.findById(savedNotice.getId());
      assertThat(foundNotice).isEmpty();
    }
  }

  @Nested
  @DisplayName("count() 테스트")
  class CountTest {

    @Test
    @DisplayName("정상: 일반글사항 개수를 조회할 수 있다")
    void count_Success() {
      // given
      RegularArticle regular1 =
          RegularArticle.builder()
              .title("일반글1")
              .content("내용")
              .writerId("admin")
              .board(board)
              .build();
      regular1.setId(idGenerator.generate());

      RegularArticle regular2 =
          RegularArticle.builder()
              .title("일반글2")
              .content("내용")
              .writerId("admin")
              .board(board)
              .build();
      regular2.setId(idGenerator.generate());

      regularArticleRepository.saveAll(List.of(regular1, regular2));

      // when
      long count = regularArticleRepository.count();

      // then
      assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("엣지: 일반글사항이 없으면 0을 반환한다")
    void count_Empty_ReturnsZero() {
      // when
      long count = regularArticleRepository.count();

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
      RegularArticle regular =
          RegularArticle.builder()
              .title("일반글")
              .content("내용")
              .writerId("admin")
              .board(board)
              .build();
      regular.setId(idGenerator.generate());
      RegularArticle savedNotice = regularArticleRepository.save(regular);

      // when
      RegularArticle foundNotice = regularArticleRepository.findById(savedNotice.getId()).get();

      // then
      assertThat(foundNotice.getBoard()).isNotNull();
      assertThat(foundNotice.getBoard().getId()).isEqualTo(board.getId());
      assertThat(foundNotice.getBoard().getName()).isEqualTo("자유게시판");
    }
  }

  @Nested
  @DisplayName("findAll() 테스트")
  class FindAllTest {

    @Test
    @DisplayName("정상: 모든 게시글을 조회할 수 있다")
    void findAll_Success() {
      // given
      RegularArticle regular1 =
          RegularArticle.builder()
              .title("게시글1")
              .content("내용")
              .writerId("user1")
              .board(board)
              .build();
      regular1.setId(idGenerator.generate());

      RegularArticle regular2 =
          RegularArticle.builder()
              .title("게시글2")
              .content("내용")
              .writerId("user2")
              .board(board)
              .build();
      regular2.setId(idGenerator.generate());

      regularArticleRepository.saveAll(List.of(regular1, regular2));

      // when
      List<RegularArticle> allArticles = regularArticleRepository.findAll();

      // then
      assertThat(allArticles).hasSize(2);
      assertThat(allArticles).extracting("title").containsExactlyInAnyOrder("게시글1", "게시글2");
    }

    @Test
    @DisplayName("엣지: 게시글이 없으면 빈 리스트를 반환한다")
    void findAll_Empty_ReturnsEmptyList() {
      // when
      List<RegularArticle> allArticles = regularArticleRepository.findAll();

      // then
      assertThat(allArticles).isEmpty();
    }
  }
}

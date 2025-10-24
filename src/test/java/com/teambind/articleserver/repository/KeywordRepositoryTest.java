package com.teambind.articleserver.repository;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.keyword.Keyword;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("KeywordRepository 테스트")
class KeywordRepositoryTest {

  @Autowired private KeywordRepository keywordRepository;

  @Autowired private BoardRepository boardRepository;

  @Autowired private TestEntityManager entityManager;

  @AfterEach
  void tearDown() {
    keywordRepository.deleteAll();
    boardRepository.deleteAll();
    entityManager.flush();
    entityManager.clear();
  }

  @Nested
  @DisplayName("save() 테스트")
  class SaveTest {

    @Test
    @DisplayName("정상: 공통 키워드를 저장할 수 있다")
    void save_CommonKeyword_Success() {
      // given
      Keyword keyword = Keyword.builder().name("공통키워드").board(null).build();

      // when
      Keyword savedKeyword = keywordRepository.save(keyword);

      // then
      assertThat(savedKeyword.getId()).isNotNull();
      assertThat(savedKeyword.getName()).isEqualTo("공통키워드");
      assertThat(savedKeyword.getBoard()).isNull();
      assertThat(savedKeyword.isCommonKeyword()).isTrue();
      assertThat(savedKeyword.getIsActive()).isTrue();
      assertThat(savedKeyword.getUsageCount()).isZero();
    }

    @Test
    @DisplayName("정상: 게시판 전용 키워드를 저장할 수 있다")
    void save_BoardKeyword_Success() {
      // given
      Board board = Board.builder().name("게시판").build();
      boardRepository.save(board);

      Keyword keyword = Keyword.builder().name("전용키워드").board(board).build();

      // when
      Keyword savedKeyword = keywordRepository.save(keyword);

      // then
      assertThat(savedKeyword.getId()).isNotNull();
      assertThat(savedKeyword.getName()).isEqualTo("전용키워드");
      assertThat(savedKeyword.getBoard()).isNotNull();
      assertThat(savedKeyword.getBoard().getName()).isEqualTo("게시판");
      assertThat(savedKeyword.isCommonKeyword()).isFalse();
    }

    // Note: Unique constraint 위반 테스트는 실제 DB 환경에서 수행하는 것이 더 적합합니다.
    // H2 테스트 환경에서는 Hibernate의 세션 관리로 인해 예외 발생 후 flush 시
    // null identifier 에러가 발생할 수 있어 테스트 방식을 변경합니다.

    @Test
    @DisplayName("예외: 같은 게시판에 중복된 키워드 이름은 저장할 수 없다")
    void save_DuplicateNameInSameBoard_ThrowsException() {
      // given
      Board board = Board.builder().name("게시판").build();
      boardRepository.saveAndFlush(board);

      Keyword keyword1 = Keyword.builder().name("키워드").board(board).build();
      keywordRepository.saveAndFlush(keyword1);

      // when
      Keyword keyword2 = Keyword.builder().name("키워드").board(board).build();

      // then
      assertThatThrownBy(
              () -> {
                keywordRepository.saveAndFlush(keyword2);
              })
          .isInstanceOf(DataIntegrityViolationException.class);

      // 예외 발생 후 세션 정리
      entityManager.clear();
    }

    @Test
    @DisplayName("정상: 다른 게시판에는 같은 이름의 키워드를 저장할 수 있다")
    void save_SameNameInDifferentBoards_Success() {
      // given
      Board board1 = Board.builder().name("게시판1").build();
      Board board2 = Board.builder().name("게시판2").build();
      boardRepository.saveAll(List.of(board1, board2));

      Keyword keyword1 = Keyword.builder().name("키워드").board(board1).build();
      Keyword keyword2 = Keyword.builder().name("키워드").board(board2).build();

      // when
      keywordRepository.saveAll(List.of(keyword1, keyword2));

      // then
      assertThat(keywordRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("정상: 공통 키워드와 게시판 전용 키워드는 같은 이름을 가질 수 있다")
    void save_SameNameCommonAndBoardKeyword_Success() {
      // given
      Board board = Board.builder().name("게시판").build();
      boardRepository.save(board);

      Keyword commonKeyword = Keyword.builder().name("키워드").board(null).build();
      Keyword boardKeyword = Keyword.builder().name("키워드").board(board).build();

      // when
      keywordRepository.saveAll(List.of(commonKeyword, boardKeyword));

      // then
      assertThat(keywordRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("예외: 공통 키워드는 중복될 수 없다")
    void save_DuplicateCommonKeyword_ThrowsException() {
      // given
      Keyword keyword1 = Keyword.builder().name("공통").board(null).build();
      keywordRepository.saveAndFlush(keyword1);

      // when
      Keyword keyword2 = Keyword.builder().name("공통").board(null).build();

      // then
      assertThatThrownBy(
              () -> {
                keywordRepository.saveAndFlush(keyword2);
              })
          .isInstanceOf(DataIntegrityViolationException.class);

      // 예외 발생 후 세션 정리
      entityManager.clear();
    }
  }

  @Nested
  @DisplayName("findById() 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("정상: ID로 키워드를 조회할 수 있다")
    void findById_Success() {
      // given
      Keyword keyword = Keyword.builder().name("키워드").build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      Optional<Keyword> foundKeyword = keywordRepository.findById(savedKeyword.getId());

      // then
      assertThat(foundKeyword).isPresent();
      assertThat(foundKeyword.get().getName()).isEqualTo("키워드");
    }

    @Test
    @DisplayName("예외: 존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    void findById_NotFound_ReturnsEmpty() {
      // when
      Optional<Keyword> foundKeyword = keywordRepository.findById(999L);

      // then
      assertThat(foundKeyword).isEmpty();
    }

    @Test
    @DisplayName("엣지: null ID로 조회하면 예외가 발생한다")
    void findById_NullId_ThrowsException() {
      // when & then
      assertThatThrownBy(() -> keywordRepository.findById(null))
          .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class);
    }
  }

  @Nested
  @DisplayName("findAllByNameIn() 테스트")
  class FindAllByNameInTest {

    @Test
    @DisplayName("정상: 이름 리스트로 키워드를 조회할 수 있다")
    void findAllByNameIn_Success() {
      // given
      Keyword keyword1 = Keyword.builder().name("키워드1").build();
      Keyword keyword2 = Keyword.builder().name("키워드2").build();
      Keyword keyword3 = Keyword.builder().name("키워드3").build();
      keywordRepository.saveAll(List.of(keyword1, keyword2, keyword3));

      // when
      List<Keyword> keywords = keywordRepository.findAllByNameIn(List.of("키워드1", "키워드3"));

      // then
      assertThat(keywords).hasSize(2);
      assertThat(keywords).extracting("name").containsExactlyInAnyOrder("키워드1", "키워드3");
    }

    @Test
    @DisplayName("예외: 존재하지 않는 이름으로 조회하면 빈 리스트를 반환한다")
    void findAllByNameIn_NotFound_ReturnsEmpty() {
      // when
      List<Keyword> keywords = keywordRepository.findAllByNameIn(List.of("존재하지않음"));

      // then
      assertThat(keywords).isEmpty();
    }

    @Test
    @DisplayName("엣지: 빈 리스트로 조회하면 빈 리스트를 반환한다")
    void findAllByNameIn_EmptyList_ReturnsEmpty() {
      // when
      List<Keyword> keywords = keywordRepository.findAllByNameIn(List.of());

      // then
      assertThat(keywords).isEmpty();
    }

    @Test
    @DisplayName("정상: 일부만 존재하는 경우 존재하는 것만 반환한다")
    void findAllByNameIn_PartialMatch_ReturnsExisting() {
      // given
      Keyword keyword1 = Keyword.builder().name("키워드1").build();
      keywordRepository.save(keyword1);

      // when
      List<Keyword> keywords = keywordRepository.findAllByNameIn(List.of("키워드1", "키워드2"));

      // then
      assertThat(keywords).hasSize(1);
      assertThat(keywords.get(0).getName()).isEqualTo("키워드1");
    }

    @Test
    @DisplayName("정상: 공통 키워드와 게시판 전용 키워드 모두 조회된다")
    void findAllByNameIn_BothCommonAndBoardKeywords() {
      // given
      Board board = Board.builder().name("게시판").build();
      boardRepository.save(board);

      Keyword commonKeyword = Keyword.builder().name("키워드").board(null).build();
      Keyword boardKeyword = Keyword.builder().name("전용").board(board).build();
      keywordRepository.saveAll(List.of(commonKeyword, boardKeyword));

      // when
      List<Keyword> keywords = keywordRepository.findAllByNameIn(List.of("키워드", "전용"));

      // then
      assertThat(keywords).hasSize(2);
    }
  }

  @Nested
  @DisplayName("countByIdIn() 테스트")
  class CountByIdInTest {

    @Test
    @DisplayName("정상: ID 리스트로 존재하는 키워드 개수를 조회할 수 있다")
    void countByIdIn_Success() {
      // given
      Keyword keyword1 = Keyword.builder().name("키워드1").build();
      Keyword keyword2 = Keyword.builder().name("키워드2").build();
      Keyword keyword3 = Keyword.builder().name("키워드3").build();
      List<Keyword> savedKeywords =
          keywordRepository.saveAll(List.of(keyword1, keyword2, keyword3));

      List<Long> ids = savedKeywords.stream().limit(2).map(Keyword::getId).toList(); // 첫 2개만

      // when
      long count = keywordRepository.countByIdIn(ids);

      // then
      assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("예외: 존재하지 않는 ID로 조회하면 0을 반환한다")
    void countByIdIn_NotFound_ReturnsZero() {
      // when
      long count = keywordRepository.countByIdIn(List.of(999L, 888L));

      // then
      assertThat(count).isZero();
    }

    @Test
    @DisplayName("엣지: 빈 리스트로 조회하면 0을 반환한다")
    void countByIdIn_EmptyList_ReturnsZero() {
      // when
      long count = keywordRepository.countByIdIn(List.of());

      // then
      assertThat(count).isZero();
    }

    @Test
    @DisplayName("정상: 일부만 존재하는 경우 존재하는 개수만 반환한다")
    void countByIdIn_PartialMatch_ReturnsExistingCount() {
      // given
      Keyword keyword = Keyword.builder().name("키워드").build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      long count = keywordRepository.countByIdIn(List.of(savedKeyword.getId(), 999L));

      // then
      assertThat(count).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("update() 테스트")
  class UpdateTest {

    @Test
    @DisplayName("정상: 키워드 사용 횟수를 증가시킬 수 있다")
    void update_IncrementUsageCount_Success() {
      // given
      Keyword keyword = Keyword.builder().name("키워드").build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      savedKeyword.incrementUsageCount();
      savedKeyword.incrementUsageCount();
      keywordRepository.save(savedKeyword);

      // then
      Keyword foundKeyword = keywordRepository.findById(savedKeyword.getId()).get();
      assertThat(foundKeyword.getUsageCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("정상: 키워드 사용 횟수를 감소시킬 수 있다")
    void update_DecrementUsageCount_Success() {
      // given
      Keyword keyword = Keyword.builder().name("키워드").usageCount(5).build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      savedKeyword.decrementUsageCount();
      keywordRepository.save(savedKeyword);

      // then
      Keyword foundKeyword = keywordRepository.findById(savedKeyword.getId()).get();
      assertThat(foundKeyword.getUsageCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("정상: 키워드를 비활성화할 수 있다")
    void update_Deactivate_Success() {
      // given
      Keyword keyword = Keyword.builder().name("키워드").build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      savedKeyword.deactivate();
      keywordRepository.save(savedKeyword);

      // then
      Keyword foundKeyword = keywordRepository.findById(savedKeyword.getId()).get();
      assertThat(foundKeyword.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("정상: 키워드를 활성화할 수 있다")
    void update_Activate_Success() {
      // given
      Keyword keyword = Keyword.builder().name("키워드").isActive(false).build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      savedKeyword.activate();
      keywordRepository.save(savedKeyword);

      // then
      Keyword foundKeyword = keywordRepository.findById(savedKeyword.getId()).get();
      assertThat(foundKeyword.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("정상: 게시판을 분리할 수 있다")
    void update_DetachFromBoard_Success() {
      // given
      Board board = Board.builder().name("게시판").build();
      boardRepository.save(board);

      Keyword keyword = Keyword.builder().name("키워드").board(board).build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      savedKeyword.detachFromBoard();
      keywordRepository.save(savedKeyword);

      // then
      Keyword foundKeyword = keywordRepository.findById(savedKeyword.getId()).get();
      assertThat(foundKeyword.getBoard()).isNull();
      assertThat(foundKeyword.isCommonKeyword()).isTrue();
    }
  }

  @Nested
  @DisplayName("delete() 테스트")
  class DeleteTest {

    @Test
    @DisplayName("정상: 키워드를 삭제할 수 있다")
    void delete_Success() {
      // given
      Keyword keyword = Keyword.builder().name("키워드").build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      keywordRepository.delete(savedKeyword);

      // then
      Optional<Keyword> foundKeyword = keywordRepository.findById(savedKeyword.getId());
      assertThat(foundKeyword).isEmpty();
    }

    @Test
    @DisplayName("정상: 게시판이 삭제되면 전용 키워드도 함께 삭제된다 (CASCADE DELETE)")
    void delete_BoardDeleted_KeywordsAlsoDeleted() {
      // given
      Board board = Board.builder().name("게시판").build();
      boardRepository.saveAndFlush(board);

      Keyword keyword = Keyword.builder().name("키워드").board(board).build();
      keywordRepository.saveAndFlush(keyword);
      Long keywordId = keyword.getId();

      // 세션 클리어로 영속성 컨텍스트 초기화
      entityManager.clear();

      // when
      Board foundBoard = boardRepository.findById(board.getId()).orElseThrow();
      boardRepository.delete(foundBoard);
      boardRepository.flush();

      // then
      Optional<Keyword> foundKeyword = keywordRepository.findById(keywordId);
      assertThat(foundKeyword).isEmpty();
    }
  }

  @Nested
  @DisplayName("findAll() 테스트")
  class FindAllTest {

    @Test
    @DisplayName("정상: 모든 키워드를 조회할 수 있다")
    void findAll_Success() {
      // given
      Keyword keyword1 = Keyword.builder().name("키워드1").build();
      Keyword keyword2 = Keyword.builder().name("키워드2").build();
      keywordRepository.saveAll(List.of(keyword1, keyword2));

      // when
      List<Keyword> keywords = keywordRepository.findAll();

      // then
      assertThat(keywords).hasSize(2);
    }

    @Test
    @DisplayName("엣지: 키워드가 없으면 빈 리스트를 반환한다")
    void findAll_Empty_ReturnsEmptyList() {
      // when
      List<Keyword> keywords = keywordRepository.findAll();

      // then
      assertThat(keywords).isEmpty();
    }
  }

  @Nested
  @DisplayName("영속성 컨텍스트 테스트")
  class PersistenceContextTest {

    @Test
    @DisplayName("정상: 같은 트랜잭션 내에서 동일성이 보장된다")
    void sameTransaction_SameInstance() {
      // given
      Keyword keyword = Keyword.builder().name("키워드").build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      Keyword foundKeyword1 = keywordRepository.findById(savedKeyword.getId()).get();
      Keyword foundKeyword2 = keywordRepository.findById(savedKeyword.getId()).get();

      // then
      assertThat(foundKeyword1).isSameAs(foundKeyword2);
    }

    @Test
    @DisplayName("정상: 변경 감지가 작동한다")
    void dirtyChecking_Works() {
      // given
      Keyword keyword = Keyword.builder().name("키워드").build();
      Keyword savedKeyword = keywordRepository.save(keyword);

      // when
      savedKeyword.incrementUsageCount();
      keywordRepository.flush();

      // then
      Keyword foundKeyword = keywordRepository.findById(savedKeyword.getId()).get();
      assertThat(foundKeyword.getUsageCount()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("연관관계 테스트")
  class RelationshipTest {

    @Test
    @DisplayName("정상: Board와의 다대일 관계가 작동한다")
    void manyToOneRelationship_WithBoard_Works() {
      // given
      Board board = Board.builder().name("게시판").build();
      boardRepository.save(board);

      Keyword keyword1 = Keyword.builder().name("키워드1").board(board).build();
      Keyword keyword2 = Keyword.builder().name("키워드2").board(board).build();
      keywordRepository.saveAll(List.of(keyword1, keyword2));

      // when
      List<Keyword> keywords = keywordRepository.findAll();

      // then
      assertThat(keywords).hasSize(2);
      assertThat(keywords).allMatch(k -> k.getBoard() != null);
      assertThat(keywords).allMatch(k -> k.getBoard().getName().equals("게시판"));
    }

    @Test
    @DisplayName("정상: 양방향 관계가 올바르게 설정된다")
    void bidirectionalRelationship_Works() {
      // given
      Board board = Board.builder().name("게시판").build();
      boardRepository.save(board);

      Keyword keyword = Keyword.builder().name("키워드").build();
      keyword.assignToBoard(board);
      keywordRepository.save(keyword);

      // when
      Keyword foundKeyword = keywordRepository.findById(keyword.getId()).get();

      // then
      assertThat(foundKeyword.getBoard()).isNotNull();
      assertThat(foundKeyword.getBoard().getName()).isEqualTo("게시판");
    }
  }

  @Nested
  @DisplayName("성능 테스트")
  class PerformanceTest {

    @Test
    @DisplayName("성능: 대량 키워드 조회가 빠르게 처리된다")
    void bulkFind_Fast() {
      // given
      List<Keyword> keywords =
          java.util.stream.IntStream.range(0, 100)
              .mapToObj(i -> Keyword.builder().name("키워드" + i).build())
              .toList();
      keywordRepository.saveAll(keywords);

      List<Long> ids = keywords.stream().map(Keyword::getId).toList();

      // when
      long startTime = System.currentTimeMillis();
      long count = keywordRepository.countByIdIn(ids);
      long duration = System.currentTimeMillis() - startTime;

      // then
      assertThat(count).isEqualTo(100);
      assertThat(duration).isLessThan(1000); // 1초 이내
    }
  }
}

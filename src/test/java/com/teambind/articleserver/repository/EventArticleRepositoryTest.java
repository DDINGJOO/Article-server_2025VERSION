package com.teambind.articleserver.repository;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.config.TestIdGeneratorConfig;
import com.teambind.articleserver.config.TestIdGeneratorConfig.TestIdGenerator;
import com.teambind.articleserver.entity.articleType.EventArticle;
import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.enums.Status;
import java.time.LocalDateTime;
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
@DisplayName("EventArticleRepository 테스트")
class EventArticleRepositoryTest {

  @Autowired private EventArticleRepository eventArticleRepository;

  @Autowired private BoardRepository boardRepository;

  @Autowired private TestIdGenerator idGenerator;

  private Board board;

  @BeforeEach
  void setUp() {
    board = Board.builder().name("이벤트게시판").build();
    boardRepository.save(board);
  }

  @AfterEach
  void tearDown() {
    eventArticleRepository.deleteAll();
    boardRepository.deleteAll();
  }

  private EventArticle createEvent(
      String title, LocalDateTime startDate, LocalDateTime endDate, Status status) {
    EventArticle event =
        EventArticle.builder()
            .title(title)
            .content("내용")
            .writerId("writer1")
            .board(board)
            .eventStartDate(startDate)
            .eventEndDate(endDate)
            .status(status)
            .build();
    event.setId(idGenerator.generate());
    return event;
  }

  @Nested
  @DisplayName("save() 테스트")
  class SaveTest {

    @Test
    @DisplayName("정상: 이벤트 게시글을 저장할 수 있다")
    void save_Success() {
      // given
      LocalDateTime startDate = LocalDateTime.now().plusDays(1);
      LocalDateTime endDate = LocalDateTime.now().plusDays(7);

      EventArticle event =
          EventArticle.builder()
              .title("이벤트")
              .content("이벤트 내용")
              .writerId("writer1")
              .board(board)
              .eventStartDate(startDate)
              .eventEndDate(endDate)
              .build();
      event.setId(idGenerator.generate());

      // when
      EventArticle savedEvent = eventArticleRepository.save(event);

      // then
      assertThat(savedEvent.getId()).isNotNull();
      assertThat(savedEvent.getTitle()).isEqualTo("이벤트");
      assertThat(savedEvent.getEventStartDate()).isEqualTo(startDate);
      assertThat(savedEvent.getEventEndDate()).isEqualTo(endDate);
      assertThat(savedEvent.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("정상: 시작일과 종료일이 같을 수 있다")
    void save_SameStartAndEndDate_Success() {
      // given
      LocalDateTime sameDate = LocalDateTime.now();

      EventArticle event =
          EventArticle.builder()
              .title("단일날짜이벤트")
              .content("내용")
              .writerId("writer1")
              .board(board)
              .eventStartDate(sameDate)
              .eventEndDate(sameDate)
              .build();
      event.setId(idGenerator.generate());

      // when
      EventArticle savedEvent = eventArticleRepository.save(event);

      // then
      assertThat(savedEvent.getEventStartDate()).isEqualTo(savedEvent.getEventEndDate());
    }
  }

  @Nested
  @DisplayName("findOngoingEvents() 테스트 - 진행중인 이벤트 조회")
  class FindOngoingEventsTest {

    @Test
    @DisplayName("정상: 현재 진행중인 이벤트를 조회할 수 있다")
    void findOngoingEvents_Success() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      // 진행중 이벤트 (시작 < 현재 < 종료)
      EventArticle ongoingEvent =
          createEvent("진행중이벤트", now.minusDays(1), now.plusDays(1), Status.ACTIVE);
      eventArticleRepository.save(ongoingEvent);

      // 종료된 이벤트
      EventArticle endedEvent =
          createEvent("종료된이벤트", now.minusDays(10), now.minusDays(1), Status.ACTIVE);
      eventArticleRepository.save(endedEvent);

      // 예정 이벤트
      EventArticle upcomingEvent =
          createEvent("예정이벤트", now.plusDays(1), now.plusDays(7), Status.ACTIVE);
      eventArticleRepository.save(upcomingEvent);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getTitle()).isEqualTo("진행중이벤트");
    }

    @Test
    @DisplayName("정상: 현재 시간이 시작일과 정확히 같을 때 진행중으로 조회된다")
    void findOngoingEvents_ExactStartTime() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event = createEvent("이벤트", now, now.plusDays(1), Status.ACTIVE);
      eventArticleRepository.save(event);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("정상: 현재 시간이 종료일과 정확히 같을 때 진행중으로 조회된다")
    void findOngoingEvents_ExactEndTime() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event = createEvent("이벤트", now.minusDays(1), now, Status.ACTIVE);
      eventArticleRepository.save(event);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("예외: DELETED 상태는 조회되지 않는다")
    void findOngoingEvents_DeletedStatus_NotFound() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event = createEvent("삭제된이벤트", now.minusDays(1), now.plusDays(1), Status.DELETED);
      eventArticleRepository.save(event);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("정상: 여러 진행중 이벤트를 최신순으로 조회한다")
    void findOngoingEvents_MultipleEvents_OrderedByCreatedDate() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event1 = createEvent("이벤트1", now.minusDays(1), now.plusDays(1), Status.ACTIVE);
      EventArticle event2 = createEvent("이벤트2", now.minusDays(1), now.plusDays(1), Status.ACTIVE);
      eventArticleRepository.save(event1);
      eventArticleRepository.save(event2);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("정상: 페이징이 올바르게 작동한다")
    void findOngoingEvents_Paging_Works() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 2);

      for (int i = 0; i < 5; i++) {
        EventArticle event =
            createEvent("이벤트" + i, now.minusDays(1), now.plusDays(1), Status.ACTIVE);
        eventArticleRepository.save(event);
      }

      // when
      Page<EventArticle> result =
          eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getTotalElements()).isEqualTo(5);
      assertThat(result.getTotalPages()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("findEndedEvents() 테스트 - 종료된 이벤트 조회")
  class FindEndedEventsTest {

    @Test
    @DisplayName("정상: 종료된 이벤트를 조회할 수 있다")
    void findEndedEvents_Success() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      // 종료된 이벤트 (종료일 < 현재)
      EventArticle endedEvent =
          createEvent("종료된이벤트", now.minusDays(10), now.minusDays(1), Status.ACTIVE);
      eventArticleRepository.save(endedEvent);

      // 진행중 이벤트
      EventArticle ongoingEvent =
          createEvent("진행중이벤트", now.minusDays(1), now.plusDays(1), Status.ACTIVE);
      eventArticleRepository.save(ongoingEvent);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findEndedEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getTitle()).isEqualTo("종료된이벤트");
    }

    @Test
    @DisplayName("정상: 종료된 이벤트를 종료일 내림차순으로 조회한다")
    void findEndedEvents_OrderedByEndDateDesc() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event1 =
          createEvent("오래전종료", now.minusDays(10), now.minusDays(5), Status.ACTIVE);
      EventArticle event2 = createEvent("최근종료", now.minusDays(5), now.minusDays(1), Status.ACTIVE);
      eventArticleRepository.save(event1);
      eventArticleRepository.save(event2);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findEndedEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent().get(0).getTitle()).isEqualTo("최근종료");
      assertThat(result.getContent().get(1).getTitle()).isEqualTo("오래전종료");
    }

    @Test
    @DisplayName("엣지: 현재 시간과 종료일이 정확히 같으면 조회되지 않는다")
    void findEndedEvents_ExactEndTime_NotFound() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event = createEvent("이벤트", now.minusDays(1), now, Status.ACTIVE);
      eventArticleRepository.save(event);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findEndedEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).isEmpty();
    }
  }

  @Nested
  @DisplayName("findUpcomingEvents() 테스트 - 예정된 이벤트 조회")
  class FindUpcomingEventsTest {

    @Test
    @DisplayName("정상: 예정된 이벤트를 조회할 수 있다")
    void findUpcomingEvents_Success() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      // 예정 이벤트 (시작일 > 현재)
      EventArticle upcomingEvent =
          createEvent("예정이벤트", now.plusDays(1), now.plusDays(7), Status.ACTIVE);
      eventArticleRepository.save(upcomingEvent);

      // 진행중 이벤트
      EventArticle ongoingEvent =
          createEvent("진행중이벤트", now.minusDays(1), now.plusDays(1), Status.ACTIVE);
      eventArticleRepository.save(ongoingEvent);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findUpcomingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getTitle()).isEqualTo("예정이벤트");
    }

    @Test
    @DisplayName("정상: 예정된 이벤트를 시작일 오름차순으로 조회한다")
    void findUpcomingEvents_OrderedByStartDateAsc() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event1 =
          createEvent("먼미래이벤트", now.plusDays(10), now.plusDays(15), Status.ACTIVE);
      EventArticle event2 = createEvent("가까운미래", now.plusDays(1), now.plusDays(5), Status.ACTIVE);
      eventArticleRepository.save(event1);
      eventArticleRepository.save(event2);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findUpcomingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent().get(0).getTitle()).isEqualTo("가까운미래");
      assertThat(result.getContent().get(1).getTitle()).isEqualTo("먼미래이벤트");
    }

    @Test
    @DisplayName("엣지: 현재 시간과 시작일이 정확히 같으면 조회되지 않는다")
    void findUpcomingEvents_ExactStartTime_NotFound() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event = createEvent("이벤트", now, now.plusDays(1), Status.ACTIVE);
      eventArticleRepository.save(event);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findUpcomingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByStatusOrderByCreatedAtDesc() 테스트")
  class FindByStatusTest {

    @Test
    @DisplayName("정상: 상태로 이벤트를 조회할 수 있다")
    void findByStatus_Success() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle activeEvent =
          createEvent("활성이벤트", now.minusDays(1), now.plusDays(1), Status.ACTIVE);
      EventArticle deletedEvent =
          createEvent("삭제된이벤트", now.minusDays(1), now.plusDays(1), Status.DELETED);
      eventArticleRepository.save(activeEvent);
      eventArticleRepository.save(deletedEvent);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findByStatusOrderByCreatedAtDesc(Status.ACTIVE, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getTitle()).isEqualTo("활성이벤트");
    }

    @Test
    @DisplayName("정상: 생성일 내림차순으로 정렬된다")
    void findByStatus_OrderedByCreatedAtDesc() {
      // given
      Pageable pageable = PageRequest.of(0, 10);
      LocalDateTime now = LocalDateTime.now();

      EventArticle event1 = createEvent("이벤트1", now, now.plusDays(1), Status.ACTIVE);
      EventArticle event2 = createEvent("이벤트2", now, now.plusDays(1), Status.ACTIVE);
      eventArticleRepository.save(event1);
      eventArticleRepository.save(event2);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findByStatusOrderByCreatedAtDesc(Status.ACTIVE, pageable);

      // then
      assertThat(result.getContent()).hasSize(2);
      // 최근에 생성된 것이 먼저 나와야 함
    }
  }

  @Nested
  @DisplayName("복합 시나리오 테스트")
  class ComplexScenarioTest {

    @Test
    @DisplayName("정상: 같은 이벤트가 시간에 따라 다른 카테고리로 조회된다")
    void sameEvent_DifferentCategoriesOverTime() {
      // given
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime startDate = now.plusHours(1);
      LocalDateTime endDate = now.plusHours(2);
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event = createEvent("이벤트", startDate, endDate, Status.ACTIVE);
      eventArticleRepository.save(event);

      // when & then - 현재는 예정
      Page<EventArticle> upcoming =
          eventArticleRepository.findUpcomingEvents(Status.ACTIVE, now, pageable);
      assertThat(upcoming.getContent()).hasSize(1);

      // when & then - 시작 후에는 진행중
      Page<EventArticle> ongoing =
          eventArticleRepository.findOngoingEvents(
              Status.ACTIVE, startDate.plusMinutes(30), pageable);
      assertThat(ongoing.getContent()).hasSize(1);

      // when & then - 종료 후에는 종료됨
      Page<EventArticle> ended =
          eventArticleRepository.findEndedEvents(Status.ACTIVE, endDate.plusMinutes(1), pageable);
      assertThat(ended.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("정상: 여러 상태의 이벤트를 시간대별로 올바르게 분류한다")
    void multipleEvents_CorrectlyClassifiedByTime() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle ended = createEvent("종료", now.minusDays(2), now.minusDays(1), Status.ACTIVE);
      EventArticle ongoing = createEvent("진행중", now.minusDays(1), now.plusDays(1), Status.ACTIVE);
      EventArticle upcoming = createEvent("예정", now.plusDays(1), now.plusDays(2), Status.ACTIVE);
      eventArticleRepository.saveAll(java.util.List.of(ended, ongoing, upcoming));

      // when
      Page<EventArticle> endedResult =
          eventArticleRepository.findEndedEvents(Status.ACTIVE, now, pageable);
      Page<EventArticle> ongoingResult =
          eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, pageable);
      Page<EventArticle> upcomingResult =
          eventArticleRepository.findUpcomingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(endedResult.getContent()).hasSize(1);
      assertThat(ongoingResult.getContent()).hasSize(1);
      assertThat(upcomingResult.getContent()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("엣지: 1초 차이로 진행중 여부가 결정된다")
    void oneSecondDifference_MattersForOngoing() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      EventArticle event =
          createEvent("이벤트", now.minusSeconds(1), now.plusSeconds(1), Status.ACTIVE);
      eventArticleRepository.save(event);

      // when - 현재 시간에는 진행중
      Page<EventArticle> ongoing =
          eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, pageable);

      // when - 종료 후 1초에는 종료됨
      Page<EventArticle> ended =
          eventArticleRepository.findEndedEvents(Status.ACTIVE, now.plusSeconds(2), pageable);

      // then
      assertThat(ongoing.getContent()).hasSize(1);
      assertThat(ended.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("엣지: 이벤트가 없으면 빈 페이지를 반환한다")
    void noEvents_ReturnsEmptyPage() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Pageable pageable = PageRequest.of(0, 10);

      // when
      Page<EventArticle> result =
          eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, pageable);

      // then
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }
  }
}

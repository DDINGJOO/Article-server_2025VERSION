package com.teambind.articleserver.factory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.articleType.EventArticle;
import com.teambind.articleserver.entity.articleType.NoticeArticle;
import com.teambind.articleserver.entity.articleType.RegularArticle;
import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.factory.impl.EventArticleFactory;
import com.teambind.articleserver.factory.impl.NoticeArticleFactory;
import com.teambind.articleserver.factory.impl.RegularArticleFactory;
import com.teambind.articleserver.repository.BoardRepository;
import com.teambind.articleserver.repository.KeywordRepository;
import com.teambind.articleserver.utils.generator.primay_key.PrimaryKetGenerator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleFactory 테스트")
class ArticleFactoryTest {

  @Mock private BoardRepository boardRepository;

  @Mock private KeywordRepository keywordRepository;

  @Mock private PrimaryKetGenerator primaryKetGenerator;

  @InjectMocks private RegularArticleFactory regularArticleFactory;

  @InjectMocks private EventArticleFactory eventArticleFactory;

  @InjectMocks private NoticeArticleFactory noticeArticleFactory;

  private ArticleFactoryRegistry registry;

  @BeforeEach
  void setUp() {
    // Registry 설정
    registry =
        new ArticleFactoryRegistry(
            List.of(regularArticleFactory, eventArticleFactory, noticeArticleFactory));
    registry.init();
  }

  @Nested
  @DisplayName("RegularArticleFactory 테스트")
  class RegularArticleFactoryTest {

    @Test
    @DisplayName("정상: 일반 게시글을 생성할 수 있다")
    void createRegularArticle_Success() {
      // given
      ArticleCreateRequest request =
          ArticleCreateRequest.builder()
              .title("테스트 제목")
              .content("테스트 내용")
              .writerId("user123")
              .boardIds(1L)
              .build();

      Board board = Board.builder().id(1L).name("자유게시판").build();

      when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
      when(primaryKetGenerator.generateKey()).thenReturn("article-001");

      // when
      Article article = regularArticleFactory.create(request);

      // then
      assertThat(article).isInstanceOf(RegularArticle.class);
      assertThat(article.getId()).isEqualTo("article-001");
      assertThat(article.getTitle()).isEqualTo("테스트 제목");
      assertThat(article.getContent()).isEqualTo("테스트 내용");
      assertThat(article.getBoard()).isEqualTo(board);
    }

    @Test
    @DisplayName("예외: Board가 존재하지 않으면 예외 발생")
    void createRegularArticle_BoardNotFound_ThrowsException() {
      // given
      ArticleCreateRequest request =
          ArticleCreateRequest.builder()
              .title("테스트 제목")
              .content("테스트 내용")
              .writerId("user123")
              .boardIds(999L)
              .build();

      when(boardRepository.findById(999L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> regularArticleFactory.create(request))
          .isInstanceOf(CustomException.class);
    }
  }

  @Nested
  @DisplayName("EventArticleFactory 테스트")
  class EventArticleFactoryTest {

    @Test
    @DisplayName("정상: 이벤트 게시글을 생성할 수 있다")
    void createEventArticle_Success() {
      // given
      LocalDateTime startDate = LocalDateTime.now();
      LocalDateTime endDate = startDate.plusDays(7);

      ArticleCreateRequest request =
          ArticleCreateRequest.builder()
              .title("이벤트 제목")
              .content("이벤트 내용")
              .writerId("admin")
              .eventStartDate(startDate)
              .eventEndDate(endDate)
              .build();

      Board eventBoard = Board.builder().id(2L).name("이벤트").build();

      when(boardRepository.findByName("이벤트")).thenReturn(Optional.of(eventBoard));
      when(primaryKetGenerator.generateKey()).thenReturn("event-001");

      // when
      Article article = eventArticleFactory.create(request);

      // then
      assertThat(article).isInstanceOf(EventArticle.class);
      EventArticle eventArticle = (EventArticle) article;
      assertThat(eventArticle.getEventStartDate()).isEqualTo(startDate);
      assertThat(eventArticle.getEventEndDate()).isEqualTo(endDate);
      assertThat(eventArticle.getBoard().getName()).isEqualTo("이벤트");
    }
  }

  @Nested
  @DisplayName("NoticeArticleFactory 테스트")
  class NoticeArticleFactoryTest {

    @Test
    @DisplayName("정상: 공지사항을 생성할 수 있다")
    void createNoticeArticle_Success() {
      // given
      ArticleCreateRequest request =
          ArticleCreateRequest.builder()
              .title("공지사항 제목")
              .content("공지사항 내용")
              .writerId("admin")
              .build();

      Board noticeBoard = Board.builder().id(3L).name("공지사항").build();

      when(boardRepository.findByName("공지사항")).thenReturn(Optional.of(noticeBoard));
      when(primaryKetGenerator.generateKey()).thenReturn("notice-001");

      // when
      Article article = noticeArticleFactory.create(request);

      // then
      assertThat(article).isInstanceOf(NoticeArticle.class);
      assertThat(article.getBoard().getName()).isEqualTo("공지사항");
    }
  }

  @Nested
  @DisplayName("ArticleFactoryRegistry 테스트")
  class ArticleFactoryRegistryTest {

    @Test
    @DisplayName("정상: 타입별로 적절한 팩토리를 반환한다")
    void getFactory_Success() {
      // when & then
      assertThat(registry.getFactory(ArticleType.REGULAR))
          .isInstanceOf(RegularArticleFactory.class);
      assertThat(registry.getFactory(ArticleType.EVENT)).isInstanceOf(EventArticleFactory.class);
      assertThat(registry.getFactory(ArticleType.NOTICE)).isInstanceOf(NoticeArticleFactory.class);
    }

    @Test
    @DisplayName("정상: 지원되는 모든 타입을 반환한다")
    void getSupportedTypes_Success() {
      // when
      ArticleType[] types = registry.getSupportedTypes();

      // then
      assertThat(types)
          .containsExactlyInAnyOrder(ArticleType.REGULAR, ArticleType.EVENT, ArticleType.NOTICE);
    }
  }

  @Nested
  @DisplayName("ArticleType 테스트")
  class ArticleTypeTest {

    @Test
    @DisplayName("정상: 이벤트 기간이 있으면 EVENT 타입을 반환한다")
    void determineType_WithEventPeriod_ReturnsEvent() {
      // when
      ArticleType type = ArticleType.determineType(1L, "자유게시판", true);

      // then
      assertThat(type).isEqualTo(ArticleType.EVENT);
    }

    @Test
    @DisplayName("정상: 공지사항 보드면 NOTICE 타입을 반환한다")
    void determineType_NoticeBoard_ReturnsNotice() {
      // when
      ArticleType type = ArticleType.determineType(3L, "공지사항", false);

      // then
      assertThat(type).isEqualTo(ArticleType.NOTICE);
    }

    @Test
    @DisplayName("정상: 기본값은 REGULAR 타입이다")
    void determineType_Default_ReturnsRegular() {
      // when
      ArticleType type = ArticleType.determineType(1L, "자유게시판", false);

      // then
      assertThat(type).isEqualTo(ArticleType.REGULAR);
    }
  }
}

package com.teambind.articleserver.fixture;

import com.teambind.articleserver.entity.articleType.EventArticle;
import com.teambind.articleserver.entity.articleType.NoticeArticle;
import com.teambind.articleserver.entity.articleType.RegularArticle;
import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.enums.Status;
import java.time.LocalDateTime;

/** Article 엔티티 테스트 픽스처 팩토리 */
public class ArticleFixture {

  /** 기본 RegularArticle 생성 */
  public static RegularArticle createRegularArticle() {
    Board board = BoardFixture.createBoard();
    return RegularArticle.builder()
        .id("ART_20251025_001")
        .title("일반 게시글 제목")
        .content("일반 게시글 내용입니다.")
        .writerId("user123")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(0L)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** ID가 있는 RegularArticle 생성 */
  public static RegularArticle createRegularArticleWithId(String id) {
    Board board = BoardFixture.createBoard();
    return RegularArticle.builder()
        .id(id)
        .title("게시글 " + id)
        .content("게시글 " + id + " 내용")
        .writerId("user123")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(0L)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** 커스텀 Board를 가진 RegularArticle 생성 */
  public static RegularArticle createRegularArticleWithBoard(Board board) {
    return RegularArticle.builder()
        .id("ART_20251025_001")
        .title("일반 게시글")
        .content("내용")
        .writerId("user123")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(0L)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** 삭제된 상태의 RegularArticle 생성 */
  public static RegularArticle createDeletedArticle() {
    Board board = BoardFixture.createBoard();
    return RegularArticle.builder()
        .id("ART_20251025_002")
        .title("삭제된 게시글")
        .content("삭제된 내용")
        .writerId("user123")
        .board(board)
        .status(Status.DELETED)
        .version(0L)
        .viewCount(0L)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** 차단된 상태의 RegularArticle 생성 */
  public static RegularArticle createBlockedArticle() {
    Board board = BoardFixture.createBoard();
    return RegularArticle.builder()
        .id("ART_20251025_003")
        .title("차단된 게시글")
        .content("차단된 내용")
        .writerId("user123")
        .board(board)
        .status(Status.BLOCKED)
        .version(0L)
        .viewCount(0L)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** 조회수가 높은 RegularArticle 생성 */
  public static RegularArticle createHighViewCountArticle(Long viewCount) {
    Board board = BoardFixture.createBoard();
    return RegularArticle.builder()
        .id("ART_20251025_004")
        .title("인기 게시글")
        .content("인기 게시글 내용")
        .writerId("user123")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(viewCount)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** 기본 EventArticle 생성 */
  public static EventArticle createEventArticle() {
    Board board = BoardFixture.createBoard();
    LocalDateTime now = LocalDateTime.now();
    return EventArticle.builder()
        .id("EVT_20251025_001")
        .title("이벤트 게시글")
        .content("이벤트 내용")
        .writerId("admin")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(0L)
        .eventStartDate(now)
        .eventEndDate(now.plusDays(7))
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /** 진행중인 EventArticle 생성 */
  public static EventArticle createOngoingEventArticle() {
    Board board = BoardFixture.createBoard();
    LocalDateTime now = LocalDateTime.now();
    return EventArticle.builder()
        .id("EVT_20251025_002")
        .title("진행중 이벤트")
        .content("이벤트 내용")
        .writerId("admin")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(0L)
        .eventStartDate(now.minusDays(1))
        .eventEndDate(now.plusDays(7))
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /** 종료된 EventArticle 생성 */
  public static EventArticle createEndedEventArticle() {
    Board board = BoardFixture.createBoard();
    LocalDateTime now = LocalDateTime.now();
    return EventArticle.builder()
        .id("EVT_20251025_003")
        .title("종료된 이벤트")
        .content("이벤트 내용")
        .writerId("admin")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(0L)
        .eventStartDate(now.minusDays(10))
        .eventEndDate(now.minusDays(1))
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /** 예정된 EventArticle 생성 */
  public static EventArticle createUpcomingEventArticle() {
    Board board = BoardFixture.createBoard();
    LocalDateTime now = LocalDateTime.now();
    return EventArticle.builder()
        .id("EVT_20251025_004")
        .title("예정된 이벤트")
        .content("이벤트 내용")
        .writerId("admin")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(0L)
        .eventStartDate(now.plusDays(1))
        .eventEndDate(now.plusDays(7))
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /** 기본 NoticeArticle 생성 */
  public static NoticeArticle createNoticeArticle() {
    Board board = BoardFixture.createBoard();
    LocalDateTime now = LocalDateTime.now();
    return NoticeArticle.builder()
        .id("NOT_20251025_001")
        .title("공지사항")
        .content("공지사항 내용입니다.")
        .writerId("admin")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(0L)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /** ID 없는 RegularArticle 생성 (persist 전) */
  public static RegularArticle createArticleWithoutId() {
    Board board = BoardFixture.createBoard();
    return RegularArticle.builder()
        .title("새 게시글")
        .content("새 게시글 내용")
        .writerId("user123")
        .board(board)
        .status(Status.ACTIVE)
        .version(0L)
        .viewCount(0L)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /** 대표 이미지가 있는 RegularArticle 생성 */
  public static RegularArticle createArticleWithFirstImage() {
    Board board = BoardFixture.createBoard();
    RegularArticle article =
        RegularArticle.builder()
            .id("ART_20251025_005")
            .title("이미지 게시글")
            .content("이미지가 있는 게시글")
            .writerId("user123")
            .board(board)
            .status(Status.ACTIVE)
            .version(0L)
            .viewCount(0L)
            .firstImageUrl("https://example.com/image1.jpg")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    return article;
  }
}

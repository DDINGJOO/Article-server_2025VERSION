package com.teambind.articleserver.application.port.in;

/**
 * 게시글 생성 Use Case (Inbound Port)
 *
 * <p>Hexagonal Architecture의 Inbound Port입니다. 외부에서 애플리케이션으로 들어오는 요청을 정의합니다.
 */
public interface CreateArticleUseCase {

  /**
   * 게시글 생성
   *
   * @param command 게시글 생성 명령
   * @return 생성된 게시글 정보
   */
  ArticleInfo createArticle(CreateArticleCommand command);

  /** 게시글 생성 명령 (Input DTO) */
  record CreateArticleCommand(
      String title,
      String content,
      String writerId,
      Long boardId,
      java.util.List<Long> keywordIds,
      java.time.LocalDateTime eventStartDate,
      java.time.LocalDateTime eventEndDate) {
    /** 이벤트 게시글 여부 확인 */
    public boolean isEventArticle() {
      return eventStartDate != null && eventEndDate != null;
    }
  }

  /** 게시글 정보 (Output DTO) */
  record ArticleInfo(
      String id,
      String title,
      String content,
      String writerId,
      String boardName,
      String status,
      java.time.LocalDateTime createdAt) {}
}

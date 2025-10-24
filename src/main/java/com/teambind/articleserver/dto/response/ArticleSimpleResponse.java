package com.teambind.articleserver.dto.response;

import com.teambind.articleserver.entity.article.Article;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 게시글 간단 응답 DTO
 *
 * <p>게시글 목록 조회 시 사용되는 간소화된 응답 DTO입니다.
 *
 * <p>상세 정보는 제외하고 목록 표시에 필요한 최소 정보만 포함합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleSimpleResponse {

  /** 게시글 ID */
  private String articleId;

  /** 게시글 제목 */
  private String title;

  /** 작성자 ID */
  private String writerId;

  /** 게시판 ID */
  private Long boardId;

  /** 게시판 이름 */
  private String boardName;

  /** 게시글 타입 (REGULAR, EVENT, NOTICE) */
  private String articleType;

  /** 게시글 상태 (ACTIVE, DELETED, BLOCKED) */
  private String status;

  /** 조회 수 */
  private Long viewCount;

  /** 대표 이미지 URL (썸네일용) */
  private String firstImageUrl;

  /** 생성 일시 */
  private LocalDateTime createdAt;

  /** 수정 일시 */
  private LocalDateTime updatedAt;

  /**
   * Article 엔티티로부터 ArticleSimpleResponse 생성
   *
   * @param article Article 엔티티
   * @return ArticleSimpleResponse
   */
  public static ArticleSimpleResponse fromEntity(Article article) {
    if (article == null) {
      return null;
    }

    return ArticleSimpleResponse.builder()
        .articleId(article.getId())
        .title(article.getTitle())
        .writerId(article.getWriterId())
        .boardId(article.getBoard() != null ? article.getBoard().getId() : null)
        .boardName(article.getBoard() != null ? article.getBoard().getName() : null)
        .articleType(getArticleType(article))
        .status(article.getStatus() != null ? article.getStatus().name() : null)
        .viewCount(article.getViewCount())
        .firstImageUrl(article.getFirstImageUrl())
        .createdAt(article.getCreatedAt())
        .updatedAt(article.getUpdatedAt())
        .build();
  }

  /**
   * Article의 실제 클래스 타입을 문자열로 반환
   *
   * @param article Article 엔티티
   * @return "REGULAR", "EVENT", "NOTICE"
   */
  private static String getArticleType(Article article) {
    String className = article.getClass().getSimpleName();
    // RegularArticle → REGULAR
    // EventArticle → EVENT
    // NoticeArticle → NOTICE
    return className.replace("Article", "").toUpperCase();
  }

  /**
   * 하위 호환성을 위한 메서드
   *
   * @deprecated fromEntity() 사용 권장
   */
  @Deprecated
  public static ArticleSimpleResponse from(Article article) {
    return fromEntity(article);
  }
}

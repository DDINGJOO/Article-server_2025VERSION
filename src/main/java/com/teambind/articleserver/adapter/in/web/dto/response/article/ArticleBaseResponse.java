package com.teambind.articleserver.adapter.in.web.dto.response.article;

import com.teambind.articleserver.adapter.in.web.dto.response.common.BoardInfo;
import com.teambind.articleserver.adapter.in.web.dto.response.common.ImageInfo;
import com.teambind.articleserver.adapter.in.web.dto.response.common.KeywordInfo;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 게시글 응답 DTO 추상 클래스
 *
 * <p>모든 게시글 타입(일반, 이벤트, 공지)의 공통 필드를 정의합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class ArticleBaseResponse {

  /** 게시글 ID */
  private String articleId;

  /** 게시글 제목 */
  private String title;

  /** 게시글 내용 */
  private String content;

  /** 작성자 ID */
  private String writerId;

  /** 게시판 정보 */
  private BoardInfo board;

  /** 게시글 상태 (ACTIVE, DELETED, BLOCKED) */
  private String status;

  /** 조회 수 */
  private Long viewCount;

  /** 대표 이미지 URL (첫 번째 이미지) */
  private String firstImageUrl;

  /** 생성 일시 */
  private LocalDateTime createdAt;

  /** 수정 일시 */
  private LocalDateTime updatedAt;

  /** 이미지 목록 */
  private List<ImageInfo> images;

  /** 키워드 목록 */
  private List<KeywordInfo> keywords;

  /**
   * Article 엔티티로부터 공통 필드를 설정합니다.
   *
   * <p>하위 클래스에서 fromEntity 메서드 구현 시 호출하여 사용합니다.
   *
   * @param article Article 엔티티
   */
  protected void setCommonFields(Article article) {
    if (article == null) {
      return;
    }

    this.articleId = article.getId();
    this.title = article.getTitle();
    this.content = article.getContent();
    this.writerId = article.getWriterId();
    this.status = article.getStatus() != null ? article.getStatus().name() : null;
    this.viewCount = article.getViewCount();
    this.firstImageUrl = article.getFirstImageUrl();
    this.createdAt = article.getCreatedAt();
    this.updatedAt = article.getUpdatedAt();

    // Board 정보 변환
    this.board = BoardInfo.fromEntity(article.getBoard());

    // 이미지 목록 변환
    if (article.getImages() != null) {
      this.images =
          article.getImages().stream().map(ImageInfo::fromEntity).collect(Collectors.toList());
    }

    // 키워드 목록 변환
    if (article.getKeywordMappings() != null) {
      this.keywords =
          article.getKeywordMappings().stream()
              .map(mapping -> KeywordInfo.fromEntity(mapping.getKeyword()))
              .filter(keywordInfo -> keywordInfo != null)
              .collect(Collectors.toList());
    }
  }
}

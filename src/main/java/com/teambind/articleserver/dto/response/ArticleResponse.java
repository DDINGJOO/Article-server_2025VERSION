package com.teambind.articleserver.dto.response;

import com.teambind.articleserver.dto.response.article.ArticleBaseResponse;
import com.teambind.articleserver.dto.response.article.EventArticleResponse;
import com.teambind.articleserver.dto.response.article.NoticeArticleResponse;
import com.teambind.articleserver.dto.response.article.RegularArticleResponse;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.articleType.EventArticle;
import com.teambind.articleserver.entity.articleType.NoticeArticle;
import com.teambind.articleserver.entity.articleType.RegularArticle;

/**
 * 게시글 응답 DTO 팩토리 클래스
 *
 * <p>게시글 타입에 따라 적절한 Response DTO를 생성합니다.
 *
 * <p>다형성을 활용하여 Article 엔티티를 자동으로 올바른 Response 타입으로 변환합니다.
 */
public class ArticleResponse {

  /**
   * Article 엔티티로부터 적절한 타입의 Response DTO 생성
   *
   * <p>Article의 실제 타입에 따라 다음과 같이 변환됩니다:
   *
   * <ul>
   *   <li>RegularArticle → RegularArticleResponse
   *   <li>EventArticle → EventArticleResponse
   *   <li>NoticeArticle → NoticeArticleResponse
   * </ul>
   *
   * @param article Article 엔티티
   * @return ArticleBaseResponse (실제 타입은 게시글 타입에 따라 달라짐)
   * @throws IllegalArgumentException 지원하지 않는 게시글 타입인 경우
   */
  public static ArticleBaseResponse fromEntity(Article article) {
    if (article == null) {
      return null;
    }

    // 게시글 타입에 따라 적절한 Response 생성
    if (article instanceof EventArticle) {
      return EventArticleResponse.fromEntity((EventArticle) article);
    } else if (article instanceof NoticeArticle) {
      return NoticeArticleResponse.fromEntity((NoticeArticle) article);
    } else if (article instanceof RegularArticle) {
      return RegularArticleResponse.fromEntity((RegularArticle) article);
    } else {
      throw new IllegalArgumentException(
          "Unsupported article type: " + article.getClass().getSimpleName());
    }
  }

  /**
   * RegularArticle 전용 변환 메서드
   *
   * @param article 일반 게시글 엔티티
   * @return RegularArticleResponse
   */
  public static RegularArticleResponse fromRegularArticle(RegularArticle article) {
    return RegularArticleResponse.fromEntity(article);
  }

  /**
   * EventArticle 전용 변환 메서드
   *
   * @param article 이벤트 게시글 엔티티
   * @return EventArticleResponse
   */
  public static EventArticleResponse fromEventArticle(EventArticle article) {
    return EventArticleResponse.fromEntity(article);
  }

  /**
   * NoticeArticle 전용 변환 메서드
   *
   * @param article 공지사항 엔티티
   * @return NoticeArticleResponse
   */
  public static NoticeArticleResponse fromNoticeArticle(NoticeArticle article) {
    return NoticeArticleResponse.fromEntity(article);
  }
}

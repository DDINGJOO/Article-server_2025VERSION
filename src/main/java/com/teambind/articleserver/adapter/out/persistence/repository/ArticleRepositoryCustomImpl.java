package com.teambind.articleserver.adapter.out.persistence.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.teambind.articleserver.adapter.in.web.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.article.QArticle;
import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.board.QBoard;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.QKeyword;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.QKeywordMappingTable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleRepositoryCustomImpl implements ArticleRepositoryCustom {

  // static 상수로 선언하여 객체 생성 최소화
  private static final QArticle ARTICLE = QArticle.article;
  private static final QBoard BOARD = QBoard.board;
  private final JPAQueryFactory queryFactory;

  private static BooleanExpression statusFilter(ArticleSearchCriteria criteria) {
    if (criteria == null) return notDeletedOrBlocked();
    Status status = criteria.getStatus();
    if (status == null) {
      return notDeletedOrBlocked();
    }
    return ARTICLE.status.eq(status);
  }

  private static BooleanExpression notDeletedOrBlocked() {
    return ARTICLE.status.ne(Status.DELETED).and(ARTICLE.status.ne(Status.BLOCKED));
  }

  private static BooleanExpression boardFilter(ArticleSearchCriteria criteria) {
    if (criteria == null || criteria.getBoard() == null) return null;
    Board board = criteria.getBoard();
    return ARTICLE.board.name.eq(board.getName());
  }

  private static BooleanExpression titleFilter(ArticleSearchCriteria criteria) {
    if (criteria == null) return null;
    String title = criteria.getTitle();
    if (title == null || title.isBlank()) return null;
    return ARTICLE.title.containsIgnoreCase(title);
  }

  private static BooleanExpression contentFilter(ArticleSearchCriteria criteria) {
    if (criteria == null) return null;
    String content = criteria.getContent();
    if (content == null || content.isBlank()) return null;
    return ARTICLE.content.containsIgnoreCase(content);
  }

  private static BooleanExpression writerFilter(ArticleSearchCriteria criteria) {
    if (criteria == null) return null;
    String writerId = criteria.getWriterId();
    if (writerId == null || writerId.isBlank()) return null;
    return ARTICLE.writerId.eq(writerId);
  }

  private static BooleanExpression cursorFilter(LocalDateTime cursorUpdatedAt, String cursorId) {
    if (cursorUpdatedAt == null && (cursorId == null || cursorId.isBlank())) return null;

    BooleanExpression byUpdatedAt = ARTICLE.updatedAt.lt(cursorUpdatedAt);
    if (cursorId == null || cursorId.isBlank()) return byUpdatedAt;

    return byUpdatedAt.or(ARTICLE.updatedAt.eq(cursorUpdatedAt).and(ARTICLE.id.lt(cursorId)));
  }

  private static BooleanExpression and(BooleanExpression... predicates) {
    BooleanExpression result = null;
    for (BooleanExpression p : predicates) {
      if (p == null) continue;
      result = (result == null) ? p : result.and(p);
    }
    return result;
  }

  @Override
  public List<Article> searchByCursor(
      ArticleSearchCriteria criteria, LocalDateTime cursorUpdatedAt, String cursorId, int size) {

    // 키워드 필터링이 필요한 경우, 서브쿼리로 처리
    BooleanExpression keywordCondition = null;
    if (criteria != null && criteria.getKeywords() != null && !criteria.getKeywords().isEmpty()) {
      QKeywordMappingTable kmt = QKeywordMappingTable.keywordMappingTable;
      QKeyword qKeyword = QKeyword.keyword;

      List<Long> keywordIds = criteria.getKeywords().stream().map(Keyword::getId).toList();

      // 서브쿼리: 해당 키워드를 가진 게시글 ID만 조회
      keywordCondition =
          ARTICLE.id.in(
              queryFactory
                  .select(kmt.article.id)
                  .from(kmt)
                  .join(kmt.keyword, qKeyword)
                  .where(qKeyword.id.in(keywordIds))
                  .fetch());
    }

    var query =
        queryFactory
            .selectFrom(ARTICLE)
            .leftJoin(ARTICLE.board, BOARD)
            .fetchJoin()
            // 키워드 Join 제거 - BatchSize로 자동 로딩됨
            .where(
                and(
                    statusFilter(criteria),
                    boardFilter(criteria),
                    titleFilter(criteria),
                    contentFilter(criteria),
                    writerFilter(criteria),
                    keywordCondition,
                    cursorFilter(cursorUpdatedAt, cursorId)))
            .orderBy(
                new OrderSpecifier<>(Order.DESC, ARTICLE.createdAt),
                new OrderSpecifier<>(Order.DESC, ARTICLE.id))
            .limit(size);

    // distinct() 제거 - 더 이상 중복 행이 없음
    return query.fetch();
  }
}

package com.teambind.articleserver.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.entity.*;
import com.teambind.articleserver.entity.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleRepositoryCustomImpl implements ArticleRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  private static BooleanExpression statusFilter(ArticleSearchCriteria criteria) {
    QArticle article = QArticle.article;
    if (criteria == null) return notDeletedOrBlocked(article);
    Status status = criteria.getStatus();
    if (status == null) {
      return notDeletedOrBlocked(article);
    }
    return article.status.eq(status);
  }

  private static BooleanExpression notDeletedOrBlocked(QArticle article) {
    return article.status.ne(Status.DELETED).and(article.status.ne(Status.BLOCKED));
  }

  private static BooleanExpression boardFilter(ArticleSearchCriteria criteria) {
    if (criteria == null || criteria.getBoard() == null) return null;
    QArticle article = QArticle.article;
    Board board = criteria.getBoard();
    return article.board.boardName.eq(board.getBoardName());
  }

  private static BooleanExpression titleFilter(ArticleSearchCriteria criteria) {
    if (criteria == null) return null;
    String title = criteria.getTitle();
    if (title == null || title.isBlank()) return null;
    return QArticle.article.title.containsIgnoreCase(title);
  }

  private static BooleanExpression contentFilter(ArticleSearchCriteria criteria) {
    if (criteria == null) return null;
    String content = criteria.getContent();
    if (content == null || content.isBlank()) return null;
    return QArticle.article.content.containsIgnoreCase(content);
  }

  private static BooleanExpression writerFilter(ArticleSearchCriteria criteria) {
    if (criteria == null) return null;
    List<String> writerIds = criteria.getWriterId();
    if (writerIds == null || writerIds.isEmpty()) return null;
    return QArticle.article.writerId.in(writerIds);
  }

  @SuppressWarnings("unchecked")
  private static BooleanExpression keywordsFilter(
      ArticleSearchCriteria criteria, QKeyword keyword) {
    if (criteria == null) return null;
    List<Keyword> keywords = criteria.getKeywords();
    if (keywords == null || keywords.isEmpty()) return null;

    return keyword.id.in(keywords.stream().map(Keyword::getId).toList());
  }

  private static BooleanExpression cursorFilter(
      QArticle article, LocalDateTime cursorUpdatedAt, String cursorId) {
    if (cursorUpdatedAt == null && (cursorId == null || cursorId.isBlank())) return null;

    BooleanExpression byUpdatedAt = article.updatedAt.lt(cursorUpdatedAt);
    if (cursorId == null || cursorId.isBlank()) return byUpdatedAt;

    return byUpdatedAt.or(article.updatedAt.eq(cursorUpdatedAt).and(article.id.lt(cursorId)));
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

    QArticle article = QArticle.article;
    QBoard board = QBoard.board;
    QKeywordMappingTable kmt = QKeywordMappingTable.keywordMappingTable;
    QKeyword qKeyword = new QKeyword("keyword");

    var query =
        queryFactory
            .selectFrom(article)
            .leftJoin(article.board, board)
            .fetchJoin()
            .leftJoin(article.keywords, kmt)
            .leftJoin(kmt.keyword, qKeyword)
            .where(
                and(
                    statusFilter(criteria),
                    boardFilter(criteria),
                    titleFilter(criteria),
                    contentFilter(criteria),
                    writerFilter(criteria),
                    keywordsFilter(criteria, qKeyword),
                    cursorFilter(article, cursorUpdatedAt, cursorId)))
            .orderBy(
                new OrderSpecifier<>(Order.DESC, article.createdAt),
                new OrderSpecifier<>(Order.DESC, article.id))
            .limit(size);

    // Use distinct to avoid duplicates when joining keywords
    return query.distinct().fetch();
  }
}

package com.teambind.articleserver.adapter.out.persistence.repository;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.projection.ArticleSimpleView;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, String>, ArticleRepositoryCustom {

  Article findAllByWriterId(String writerId, Limit limit);

  @Query(
      "select a.id as id, a.title as title, a.writerId as writerId, a.version as version, a.createdAt as createdAt from Article a where a.id in :ids and a.status <> com.teambind.articleserver.adapter.out.persistence.entity.enums.Status.DELETED and a.status <> com.teambind.articleserver.adapter.out.persistence.entity.enums.Status.BLOCKED")
  List<ArticleSimpleView> findSimpleByIdIn(@Param("ids") Collection<String> ids);

  void deleteByStatus(Status status);
}

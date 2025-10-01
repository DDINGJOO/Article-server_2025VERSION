package com.teambind.articleserver.repository;

import com.teambind.articleserver.entity.Article;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, String> {

  Article findAllByWriterId(String writerId, Limit limit);
  
}

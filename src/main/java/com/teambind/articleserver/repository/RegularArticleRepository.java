package com.teambind.articleserver.repository;

import com.teambind.articleserver.entity.articleType.RegularArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegularArticleRepository extends JpaRepository<RegularArticle, String> {}

package com.teambind.articleserver.repository;

import com.teambind.articleserver.entity.articleType.NoticeArticle;
import com.teambind.articleserver.entity.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeArticleRepository extends JpaRepository<NoticeArticle, String> {
  Page<NoticeArticle> findByStatusOrderByCreatedAtDesc(Status status, Pageable pageable);
}

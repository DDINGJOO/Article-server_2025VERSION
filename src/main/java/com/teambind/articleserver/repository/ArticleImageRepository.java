package com.teambind.articleserver.repository;

import com.teambind.articleserver.entity.ArticleImage;
import com.teambind.articleserver.entity.embeddable_id.ArticleImagesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleImageRepository extends JpaRepository<ArticleImage, ArticleImagesId> {
}

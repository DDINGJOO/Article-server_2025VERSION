package com.teambind.articleserver.repository;

import com.teambind.articleserver.entity.embeddable_id.ArticleImagesId;
import com.teambind.articleserver.entity.image.ArticleImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleImageRepository extends JpaRepository<ArticleImage, ArticleImagesId> {
}

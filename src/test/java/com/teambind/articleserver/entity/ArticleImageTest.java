package com.teambind.articleserver.entity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.teambind.articleserver.entity.embeddable_id.ArticleImagesId;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleImageTest {
	
	@Autowired
	EntityManager entityManager;
	
	
	@Test
	@DisplayName("이미지가 한장인 게시글 저장")
	public void articleSaveWithImageTest() {
		Board board = Board.builder()
				.boardName("test-board")
				.build();
		entityManager.persist(board);
		entityManager.flush();
		Article article = new Article();
		article.setId("article1");
		article.setTitle("test-title");
		article.setContent("test-content");
		article.setWriterId("test-writer-id");
		article.setCreatedAt(LocalDateTime.now());
		article.setUpdatedAt(LocalDateTime.now());
		article.setBoard(board);
		
		entityManager.persist(article);
		entityManager.flush();

    article.addImage("test-url-1");

		entityManager.flush();
		
		
		assertThat(article.getId()).isNotNull();
		assertThat(article.getBoard()).isNotNull();
		assertThat(article.getImages()).isNotNull();
		
		log.info("article id : {}", article.getId());
	}
	
	@Test
	@DisplayName("이미지가 여러장인 게시글 저장")
	public void articleSaveWithMultiImageTest() {
		Board board = Board.builder()
				.boardName("test-board")
				.build();
		entityManager.persist(board);
		entityManager.flush();
		Article article = new Article();
		article.setId("article1");
		article.setTitle("test-title");
		article.setContent("test-content");
		article.setWriterId("test-writer-id");
		article.setCreatedAt(LocalDateTime.now());
		article.setUpdatedAt(LocalDateTime.now());
		article.setBoard(board);
		
		entityManager.persist(article);
		entityManager.flush();
		
		List<ArticleImage> images = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			ArticleImagesId imagesId = new ArticleImagesId("article1", (long) i);
			ArticleImage articleImage = new ArticleImage();
			articleImage.setId(imagesId);
			articleImage.setImageUrl("test-url-" + i);
			images.add(articleImage);
			articleImage.setArticle(article);
			article.getImages().add(articleImage);
			
		}
		entityManager.flush();
		
		
		assertThat(article.getId()).isNotNull();
		assertThat(article.getBoard()).isNotNull();
		assertThat(article.getImages()).isNotNull();
		
		log.info("article id : {}", article.getId());
		log.info("article image size : {}", article.getImages().size());
		article.getImages().forEach(image -> {
			log.info("article image url : {}", image.getImageUrl());
		});
	}
	
	
}

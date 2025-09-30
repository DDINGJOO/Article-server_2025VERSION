package com.teambind.articleserver.entity;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleTest {
	
	@Autowired
	EntityManager entityManager;
	
	@Test
	@DisplayName("이미지와 키워드와 보드가 없는 게시글 저장")
	public void articleSaveTest() {
		Board board = Board.builder()
				.boardName("테스트 게시판")
				.build();
		entityManager.persist(board);
		
		Article article = Article.builder()
				.id("test-article-id")
				.title("테스트 게시글")
				.content("테스트 게시글 내용")
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.board(board)
				.build();
		
		entityManager.persist(article);
		entityManager.flush();
		
		assertThat(article.getId()).isNotNull();
		log.info("article id : {}", article.getId());
		log.info("article title : {}", article.getTitle());
		log.info("article content : {}", article.getContent());
		log.info("article board id : {}", article.getBoard().getId());
		log.info("article board name : {}", article.getBoard().getBoardName());
		
	}
	
	
}

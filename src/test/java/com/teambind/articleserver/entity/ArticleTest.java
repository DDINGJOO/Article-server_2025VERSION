package com.teambind.articleserver.entity;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Profile;


@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Profile("dev")
class ArticleTest {
	
	@Autowired
	EntityManager entityManager;
	
	@Test
	@DisplayName("이미지와 키워드와 보드가 없는 게시글 저장")
	public void articleSaveTest() {
		Article article = Article.builder()
				.title("테스트 게시글")
				.contents("테스트 게시글 내용")
				.build();
		
		entityManager.persist(article);
	}
	
	
}

package com.teambind.articleserver.entity;

import com.teambind.articleserver.entity.embeddable_id.KeywordMappingTableId;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KeywordMappingTableTest {
	
	@Autowired
	EntityManager entityManager;
	
	
	@Test
	@DisplayName("키워드가 한개인 게시글 저장")
	public void saveArticleWithOneKeywordTest() {
		Board board = Board.builder()
				.boardName("테스트 게시판")
				.build();
		entityManager.persist(board);
		entityManager.flush();
		
		Article article = Article.builder()
				.id("test-article-id")
				.title("테스트 게시글")
				.content("테스트 게시글 내용")
				.writerId("test-writer-id")
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.board(board)
				.build();
		
		entityManager.persist(article);
		entityManager.flush();
		
		
		Keyword keyword = Keyword.builder()
				.keyword("test-keyword")
				.build();
		
		List<KeywordMappingTable> keywordMappingTables = new ArrayList<>();
		keywordMappingTables.add(
				KeywordMappingTable.builder()
						.id(new KeywordMappingTableId(keyword.getId(), article.getId()))
						.keyword(keyword)
						.article(article)
						.build()
		);
		article.setKeywords(keywordMappingTables);
		entityManager.persist(keyword);
		entityManager.flush();
		
		assertEquals(1, keywordMappingTables.size());
		log.info("keyword mapping table id : {}", entityManager.find(Article.class, article.getId()).getKeywords().get(0).getKeyword().getKeyword());
	}
	
}

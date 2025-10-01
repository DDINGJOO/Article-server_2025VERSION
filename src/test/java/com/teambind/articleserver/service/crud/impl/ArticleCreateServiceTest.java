package com.teambind.articleserver.service.crud.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.repository.ArticleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleCreateServiceTest {
	
	@Mock
	private ArticleRepository articleRepository;
	
	
	@InjectMocks
	private ArticleCreateService articleCreateService;
	
	private Board board;
	private List<Keyword> keywords;
	
	@Captor
	private ArgumentCaptor<Article> articleCaptor;
	
	@BeforeEach
	void setUp() {
		board = Board.builder()
				.id(1L)
				.boardName("테스트 게시판")
				.build();
		keywords = new ArrayList<>();
		keywords.add(Keyword.builder().id(1L).keyword("k1").build());
		keywords.add(Keyword.builder().id(2L).keyword("k2").build());
	}
	
	@Test
	@DisplayName("createArticle: 게시글 생성 시 필드/상태/키워드가 세팅되고 저장된다")
	void createArticle_success() {
		// given
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
		
		// when
		Article saved = articleCreateService.createArticle(
				"title", "content", "writer-1", board, keywords
		);
		
		// then
		assertAll(
				() -> assertNotNull(saved),
				() -> assertEquals("title", saved.getTitle()),
				() -> assertEquals("content", saved.getContent()),
				() -> assertEquals("writer-1", saved.getWriterId()),
				() -> assertEquals(board, saved.getBoard()),
				() -> assertEquals(Status.ACTIVE, saved.getStatus()),
				() -> assertNotNull(saved.getCreatedAt()),
				() -> assertNotNull(saved.getUpdatedAt()),
				() -> assertThat(saved.getKeywords()).hasSize(2)
		);
		verify(articleRepository, times(1)).save(any(Article.class));
	}
	
	@Test
	@DisplayName("updateArticle: 존재하는 게시글을 업데이트하면 필드와 키워드/이미지가 갱신된다")
	void updateArticle_success() {
		// given existing article with different values and some images/keywords
		Article existing = Article.builder()
				.id("art-1")
				.title("old-title")
				.content("old-content")
				.writerId("old-writer")
				.board(Board.builder().id(2L).boardName("old-board").build())
				.status(Status.ACTIVE)
				.createdAt(LocalDateTime.now().minusDays(1))
				.updatedAt(LocalDateTime.now().minusDays(1))
				.build();
		// pre-populate images and keywords so we can verify removal
		existing.addImage("img1");
		existing.addImage("img2");
		existing.addKeyword("old-k1");
		existing.addKeyword("old-k2");
		
		when(articleRepository.findById(eq("art-1"))).thenReturn(Optional.of(existing));
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
		
		// when
		Article updated = articleCreateService.updateArticle(
				"art-1", "new-title", "new-content", "new-writer", board, keywords
		);
		
		// then
		assertAll(
				() -> assertEquals("new-title", updated.getTitle()),
				() -> assertEquals("new-content", updated.getContent()),
				() -> assertEquals("new-writer", updated.getWriterId()),
				() -> assertEquals(board, updated.getBoard()),
				() -> assertThat(updated.getKeywords()).hasSize(2),
				() -> assertThat(updated.getImages()).hasSize(0),
				() -> assertThat(updated.getUpdatedAt()).isNotNull()
		);
		verify(articleRepository, times(1)).findById("art-1");
		verify(articleRepository, times(1)).save(any(Article.class));
	}
	
	@Test
	@DisplayName("updateArticle: 게시글을 찾지 못하면 CustomException을 던진다")
	void updateArticle_notFound() {
		// given
		when(articleRepository.findById(eq("missing"))).thenReturn(Optional.empty());
		
		// when & then
		assertThrows(CustomException.class, () -> articleCreateService.updateArticle(
				"missing", "t", "c", "w", board, keywords
		));
		verify(articleRepository, never()).save(any());
	}
	
	@Test
	@DisplayName("deleteArticle: 상태가 DELETED로 바뀌고 저장된다")
	void deleteArticle_success() {
		// given
		Article article = Article.builder()
				.id("art-2")
				.title("t")
				.content("c")
				.writerId("w")
				.board(board)
				.status(Status.ACTIVE)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

    when(articleRepository.findById(eq("art-2"))).thenReturn(Optional.of(article));
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    articleCreateService.deleteArticle("art-2");

		// then
		assertThat(article.getStatus()).isEqualTo(Status.DELETED);
    verify(articleRepository, times(1)).findById("art-2");
		verify(articleRepository, times(1)).save(article);
	}
}

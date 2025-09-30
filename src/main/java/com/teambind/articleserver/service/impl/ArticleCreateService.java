package com.teambind.articleserver.service.impl;


import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArticleCreateService {
	
	private final ArticleRepository articleRepository;
	
	public Article createArticle(
			String title, String content, String writerId, Board board, List<Keyword> keywords
	) {
		Article article = Article.builder()
				.title(title)
				.content(content)
				.writerId(writerId)
				.board(board)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.status(Status.ACTIVE)
				.build();
		article.addKeywords(keywords);
		
		articleRepository.save(article);
		return article;
	}
	
	public Article updateArticle(String articleId, String title, String content, String writerId, Board board, List<Keyword> keywords) {
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));
		article.setTitle(title);
		article.setContent(content);
		article.setWriterId(writerId);
		article.setBoard(board);
		article.setUpdatedAt(LocalDateTime.now());
		article.removeKeywords();
		article.removeImages();
		article.addKeywords(keywords);
		articleRepository.save(article);
		return article;
	}
	
	public void deleteArticle(Article article) {
		article.setStatus(Status.DELETED);
		articleRepository.save(article);
		log.info("Article deleted : {}", article.getId());
		
	}
}

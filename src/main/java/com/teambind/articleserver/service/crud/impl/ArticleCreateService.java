package com.teambind.articleserver.service.crud.impl;

import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.event.events.ArticleCreatedEvent;
import com.teambind.articleserver.event.publish.KafkaPublisher;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.ArticleRepository;
import com.teambind.articleserver.utils.generator.primay_key.KeyProvider;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArticleCreateService {
	
	private final ArticleRepository articleRepository;
  private final KeyProvider keyProvider;
  private final KafkaPublisher kafkaPublisher;

	public Article createArticle(
			String title, String content, String writerId, Board board, List<Keyword> keywords
	) {
    Article article =
        Article.builder()
            .id(keyProvider.generateKey())
            .title(title)
            .content(content)
            .writerId(writerId)
            .board(board)
            .keywords(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .status(Status.ACTIVE)
            .build();

		article.addKeywords(keywords);
		
		articleRepository.save(article);
    publishArticleCreatedEvent(article);
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
    publishArticleCreatedEvent(article);
		return article;
	}

  public void deleteArticle(String articeId) {
    Article article =
        articleRepository
            .findById(articeId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));
		article.setStatus(Status.DELETED);
		articleRepository.save(article);
		log.info("Article deleted : {}", article.getId());
		
	}

  private void publishArticleCreatedEvent(Article article) {
    kafkaPublisher.articleUpdatedEvent(ArticleCreatedEvent.from(article));
  }
}

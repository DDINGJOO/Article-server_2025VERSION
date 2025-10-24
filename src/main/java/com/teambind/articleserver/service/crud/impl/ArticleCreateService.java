package com.teambind.articleserver.service.crud.impl;

import com.teambind.articleserver.entity.*;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.event.events.ArticleCreatedEvent;
import com.teambind.articleserver.event.publish.KafkaPublisher;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.ArticleRepository;
import com.teambind.articleserver.repository.EventArticleRepository;
import com.teambind.articleserver.repository.NoticeArticleRepository;
import com.teambind.articleserver.repository.RegularArticleRepository;
import com.teambind.articleserver.utils.generator.primay_key.PrimaryKetGenerator;
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
	private final RegularArticleRepository regularArticleRepository;
	private final EventArticleRepository eventArticleRepository;
	private final NoticeArticleRepository noticeArticleRepository;
	private final com.teambind.articleserver.repository.BoardRepository boardRepository;
  private final PrimaryKetGenerator primaryKetGenerator;
  private final KafkaPublisher kafkaPublisher;

	// 일반 게시글 생성
	public RegularArticle createRegularArticle(
			String title, String content, String writerId,Board board,  List<Keyword> keywords
	) {
    RegularArticle article =
        RegularArticle.builder()
            .id(primaryKetGenerator.generateKey())
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

		regularArticleRepository.save(article);
    publishArticleCreatedEvent(article);
		return article;
	}

	// 이벤트 게시글 생성 (Board 자동 설정)
	public EventArticle createEventArticle(
			String title, String content, String writerId, List<Keyword> keywords,
			LocalDateTime eventStartDate, LocalDateTime eventEndDate
	) {
		Board eventBoard = boardRepository.findByBoardName("이벤트")
				.orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    EventArticle article =
        EventArticle.builder()
            .id(primaryKetGenerator.generateKey())
            .title(title)
            .content(content)
            .writerId(writerId)
            .board(eventBoard)
            .keywords(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .status(Status.ACTIVE)
            .eventStartDate(eventStartDate)
            .eventEndDate(eventEndDate)
            .build();

		article.addKeywords(keywords);

		eventArticleRepository.save(article);
    publishArticleCreatedEvent(article);
		return article;
	}

	// 공지사항 생성 (Board 자동 설정)
	public NoticeArticle createNoticeArticle(
			String title, String content, String writerId, List<Keyword> keywords
	) {
		Board noticeBoard = boardRepository.findByBoardName("공지사항")
				.orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

    NoticeArticle article =
        NoticeArticle.builder()
            .id(primaryKetGenerator.generateKey())
            .title(title)
            .content(content)
            .writerId(writerId)
            .board(noticeBoard)
            .keywords(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .status(Status.ACTIVE)
            .build();

		article.addKeywords(keywords);

		noticeArticleRepository.save(article);
    publishArticleCreatedEvent(article);
		return article;
	}

	// 하위 호환성을 위한 기존 메서드 (일반 게시글로 처리)
	public Article createArticle(
			String title, String content, String writerId, Board board,  List<Keyword> keywords
	) {
		return createRegularArticle(title, content, writerId, board, keywords);
	}
	
	public Article updateArticle(String articleId, String title, String content, String writerId, Board board, List<Keyword> keywords) {
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));
		article.setTitle(title);
		article.setContent(content);
		article.setWriterId(writerId);
		article.setBoard(board);
		article.setUpdatedAt(LocalDateTime.now());

		// EventArticle인 경우 이벤트 날짜는 별도 메서드로 처리해야 함
		article.removeKeywords();
		article.removeImages();
		article.addKeywords(keywords);
		articleRepository.save(article);
    publishArticleCreatedEvent(article);
		return article;
	}

	public EventArticle updateEventArticle(String articleId, String title, String content, String writerId,
										   List<Keyword> keywords,
										   LocalDateTime eventStartDate, LocalDateTime eventEndDate) {
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));

		if (!(article instanceof EventArticle)) {
			throw new CustomException(ErrorCode.ARTICLE_NOT_FOUND);
		}

		EventArticle eventArticle = (EventArticle) article;
		eventArticle.setTitle(title);
		eventArticle.setContent(content);
		eventArticle.setWriterId(writerId);
		// Board는 변경하지 않음 (항상 "이벤트" Board)
		eventArticle.setUpdatedAt(LocalDateTime.now());
		eventArticle.setEventStartDate(eventStartDate);
		eventArticle.setEventEndDate(eventEndDate);
		eventArticle.removeKeywords();
		eventArticle.removeImages();
		eventArticle.addKeywords(keywords);
		eventArticleRepository.save(eventArticle);
		publishArticleCreatedEvent(eventArticle);
		return eventArticle;
	}

  public void deleteArticle(String articleId) {
    Article article =
        articleRepository
            .findById(articleId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));
		article.setStatus(Status.DELETED);
		articleRepository.save(article);
		log.info("Article deleted : {}", article.getId());
		
	}

  private void publishArticleCreatedEvent(Article article) {
    kafkaPublisher.articleUpdatedEvent(ArticleCreatedEvent.from(article));
  }
}

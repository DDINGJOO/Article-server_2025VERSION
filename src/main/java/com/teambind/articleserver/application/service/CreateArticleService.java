package com.teambind.articleserver.application.service;

import com.teambind.articleserver.application.port.in.CreateArticleUseCase;
import com.teambind.articleserver.application.port.out.LoadArticlePort;
import com.teambind.articleserver.application.port.out.PublishEventPort;
import com.teambind.articleserver.application.port.out.SaveArticlePort;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.articleType.EventArticle;
import com.teambind.articleserver.entity.articleType.NoticeArticle;
import com.teambind.articleserver.entity.articleType.RegularArticle;
import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.entity.keyword.Keyword;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.BoardRepository;
import com.teambind.articleserver.repository.KeywordRepository;
import com.teambind.articleserver.utils.generator.primay_key.PrimaryKetGenerator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 생성 애플리케이션 서비스
 *
 * Hexagonal Architecture의 Application Service입니다.
 * Use Case를 구현하고 Port들을 조합하여 비즈니스 로직을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreateArticleService implements CreateArticleUseCase {

    private final SaveArticlePort saveArticlePort;
    private final PublishEventPort publishEventPort;
    private final BoardRepository boardRepository;
    private final KeywordRepository keywordRepository;
    private final PrimaryKetGenerator primaryKetGenerator;

    @Override
    public ArticleInfo createArticle(CreateArticleCommand command) {
        log.info("Creating article with title: {}", command.title());

        // Board 조회
        Board board = boardRepository.findById(command.boardId())
            .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        // Keywords 조회
        List<Keyword> keywords = fetchKeywords(command.keywordIds());

        // Article 생성 (타입에 따라 분기)
        Article article = createArticleByType(command, board, keywords);

        // 저장
        Article savedArticle = saveArticlePort.saveArticle(article);

        // 이벤트 발행
        publishArticleCreatedEvent(savedArticle);

        // DTO 변환 및 반환
        return toArticleInfo(savedArticle);
    }

    private Article createArticleByType(CreateArticleCommand command, Board board, List<Keyword> keywords) {
        String articleId = primaryKetGenerator.generateKey();

        Article article;
        if (command.isEventArticle() || "이벤트".equals(board.getName())) {
            // Event Article
            article = EventArticle.builder()
                .id(articleId)
                .title(command.title())
                .content(command.content())
                .writerId(command.writerId())
                .board(board)
                .keywordMappings(new ArrayList<>())
                .status(Status.ACTIVE)
                .eventStartDate(command.eventStartDate())
                .eventEndDate(command.eventEndDate())
                .build();
        } else if ("공지사항".equals(board.getName())) {
            // Notice Article
            article = NoticeArticle.builder()
                .id(articleId)
                .title(command.title())
                .content(command.content())
                .writerId(command.writerId())
                .board(board)
                .keywordMappings(new ArrayList<>())
                .status(Status.ACTIVE)
                .build();
        } else {
            // Regular Article
            article = RegularArticle.builder()
                .id(articleId)
                .title(command.title())
                .content(command.content())
                .writerId(command.writerId())
                .board(board)
                .keywordMappings(new ArrayList<>())
                .status(Status.ACTIVE)
                .build();
        }

        // Keywords 추가
        if (keywords != null && !keywords.isEmpty()) {
            article.addKeywords(keywords);
        }

        return article;
    }

    private List<Keyword> fetchKeywords(List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return null;
        }
        return keywordRepository.findAllById(keywordIds);
    }

    private void publishArticleCreatedEvent(Article article) {
        PublishEventPort.ArticleCreatedEvent event = new PublishEventPort.ArticleCreatedEvent(
            article.getId(),
            article.getTitle(),
            article.getWriterId(),
            article.getBoard() != null ? article.getBoard().getId() : null,
            LocalDateTime.now()
        );
        publishEventPort.publishArticleCreatedEvent(event);
    }

    private ArticleInfo toArticleInfo(Article article) {
        return new ArticleInfo(
            article.getId(),
            article.getTitle(),
            article.getContent(),
            article.getWriterId(),
            article.getBoard() != null ? article.getBoard().getName() : null,
            article.getStatus().name(),
            article.getCreatedAt()
        );
    }
}
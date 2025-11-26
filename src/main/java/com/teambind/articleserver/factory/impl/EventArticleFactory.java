package com.teambind.articleserver.factory.impl;

import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.articleType.EventArticle;
import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import com.teambind.articleserver.adapter.out.persistence.repository.BoardRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.KeywordRepository;
import com.teambind.articleserver.common.exception.CustomException;
import com.teambind.articleserver.common.exception.ErrorCode;
import com.teambind.articleserver.common.util.generator.primay_key.PrimaryKetGenerator;
import com.teambind.articleserver.factory.ArticleFactory;
import com.teambind.articleserver.factory.ArticleType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 이벤트 게시글 생성 팩토리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventArticleFactory implements ArticleFactory {

    private final BoardRepository boardRepository;
    private final KeywordRepository keywordRepository;
    private final PrimaryKetGenerator primaryKetGenerator;

    private static final String EVENT_BOARD_NAME = "이벤트";

    @Override
    public Article create(ArticleCreateRequest request) {
        log.debug("Creating event article with title: {}", request.getTitle());

        // 이벤트 게시판 자동 설정
        Board eventBoard = boardRepository
            .findByName(EVENT_BOARD_NAME)
            .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND,
                "Event board '" + EVENT_BOARD_NAME + "' not found"));

        // Keywords 조회
        List<Keyword> keywords = fetchKeywords(request.getKeywordIds());

        // Event Article 생성
        EventArticle article = EventArticle.builder()
            .id(primaryKetGenerator.generateKey())
            .title(request.getTitle())
            .content(request.getContent())
            .writerId(request.getWriterId())
            .board(eventBoard)
            .keywordMappings(new ArrayList<>())
            .status(Status.ACTIVE)
            .eventStartDate(request.getEventStartDate())
            .eventEndDate(request.getEventEndDate())
            .build();

        // Keywords 추가
        if (keywords != null && !keywords.isEmpty()) {
            article.addKeywords(keywords);
        }

        log.info("Event article created with id: {}, period: [{} ~ {}]",
            article.getId(),
            request.getEventStartDate(),
            request.getEventEndDate());

        return article;
    }

    @Override
    public ArticleType getSupportedType() {
        return ArticleType.EVENT;
    }

    private List<Keyword> fetchKeywords(List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return null;
        }
        return keywordRepository.findAllById(keywordIds);
    }
}

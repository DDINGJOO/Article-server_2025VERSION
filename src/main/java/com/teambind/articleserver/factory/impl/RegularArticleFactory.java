package com.teambind.articleserver.factory.impl;

import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.articleType.RegularArticle;
import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.entity.keyword.Keyword;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.factory.ArticleFactory;
import com.teambind.articleserver.factory.ArticleType;
import com.teambind.articleserver.repository.BoardRepository;
import com.teambind.articleserver.repository.KeywordRepository;
import com.teambind.articleserver.utils.generator.primay_key.PrimaryKetGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 일반 게시글 생성 팩토리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegularArticleFactory implements ArticleFactory {

    private final BoardRepository boardRepository;
    private final KeywordRepository keywordRepository;
    private final PrimaryKetGenerator primaryKetGenerator;

    @Override
    public Article create(ArticleCreateRequest request) {
        log.debug("Creating regular article with title: {}", request.getTitle());

        // Board 조회
        Board board = boardRepository
            .findById(request.getBoardIds())
            .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        // Keywords 조회
        List<Keyword> keywords = fetchKeywords(request.getKeywordIds());

        // Regular Article 생성
        RegularArticle article = RegularArticle.builder()
            .id(primaryKetGenerator.generateKey())
            .title(request.getTitle())
            .content(request.getContent())
            .writerId(request.getWriterId())
            .board(board)
            .keywordMappings(new ArrayList<>())
            .status(Status.ACTIVE)
            .build();

        // Keywords 추가
        if (keywords != null && !keywords.isEmpty()) {
            article.addKeywords(keywords);
        }

        log.info("Regular article created with id: {}", article.getId());
        return article;
    }

    @Override
    public ArticleType getSupportedType() {
        return ArticleType.REGULAR;
    }

    private List<Keyword> fetchKeywords(List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return null;
        }
        return keywordRepository.findAllById(keywordIds);
    }
}
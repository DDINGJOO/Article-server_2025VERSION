package com.teambind.articleserver.factory.impl;

import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.articleType.NoticeArticle;
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
 * 공지사항 생성 팩토리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NoticeArticleFactory implements ArticleFactory {

    private final BoardRepository boardRepository;
    private final KeywordRepository keywordRepository;
    private final PrimaryKetGenerator primaryKetGenerator;

    private static final String NOTICE_BOARD_NAME = "공지사항";

    @Override
    public Article create(ArticleCreateRequest request) {
        log.debug("Creating notice article with title: {}", request.getTitle());

        // 공지사항 게시판 자동 설정
        Board noticeBoard = boardRepository
            .findByName(NOTICE_BOARD_NAME)
            .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND,
                "Notice board '" + NOTICE_BOARD_NAME + "' not found"));

        // Keywords 조회
        List<Keyword> keywords = fetchKeywords(request.getKeywordIds());

        // Notice Article 생성
        NoticeArticle article = NoticeArticle.builder()
            .id(primaryKetGenerator.generateKey())
            .title(request.getTitle())
            .content(request.getContent())
            .writerId(request.getWriterId())
            .board(noticeBoard)
            .keywordMappings(new ArrayList<>())
            .status(Status.ACTIVE)
            .build();

        // Keywords 추가
        if (keywords != null && !keywords.isEmpty()) {
            article.addKeywords(keywords);
        }

        log.info("Notice article created with id: {}", article.getId());
        return article;
    }

    @Override
    public ArticleType getSupportedType() {
        return ArticleType.NOTICE;
    }

    private List<Keyword> fetchKeywords(List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return null;
        }
        return keywordRepository.findAllById(keywordIds);
    }
}

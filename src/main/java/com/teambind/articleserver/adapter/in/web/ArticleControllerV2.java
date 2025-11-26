package com.teambind.articleserver.adapter.in.web;

import com.teambind.articleserver.application.port.in.CreateArticleUseCase;
import com.teambind.articleserver.application.port.in.CreateArticleUseCase.ArticleInfo;
import com.teambind.articleserver.application.port.in.CreateArticleUseCase.CreateArticleCommand;
import com.teambind.articleserver.application.port.in.ReadArticleUseCase;
import com.teambind.articleserver.application.port.in.ReadArticleUseCase.ArticleDetailInfo;
import com.teambind.articleserver.application.port.in.ReadArticleUseCase.ArticlePageInfo;
import com.teambind.articleserver.application.port.in.ReadArticleUseCase.SearchArticleQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 게시글 REST Controller V2
 *
 * Hexagonal Architecture의 Inbound Adapter입니다.
 * REST API를 통해 들어오는 요청을 Use Case로 전달합니다.
 */
@RestController
@RequestMapping("/api/v2/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleControllerV2 {

    private final CreateArticleUseCase createArticleUseCase;
    private final ReadArticleUseCase readArticleUseCase;

    /**
     * 게시글 생성
     */
    @PostMapping
    public ResponseEntity<ArticleInfo> createArticle(@Valid @RequestBody CreateArticleRequest request) {
        log.info("Creating article: title={}", request.title());

        CreateArticleCommand command = new CreateArticleCommand(
            request.title(),
            request.content(),
            request.writerId(),
            request.boardId(),
            request.keywordIds(),
            request.eventStartDate(),
            request.eventEndDate()
        );

        ArticleInfo articleInfo = createArticleUseCase.createArticle(command);
        return ResponseEntity.ok(articleInfo);
    }

    /**
     * 게시글 조회
     */
    @GetMapping("/{articleId}")
    public ResponseEntity<ArticleDetailInfo> getArticle(@PathVariable String articleId) {
        log.info("Getting article: id={}", articleId);

        return readArticleUseCase.getArticle(articleId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 게시글 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ArticlePageInfo> searchArticles(
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String writerId,
            @RequestParam(required = false) List<Long> keywordIds,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        SearchArticleQuery query = new SearchArticleQuery(
            boardId, title, content, writerId, keywordIds, status, page, size
        );

        ArticlePageInfo pageInfo = readArticleUseCase.searchArticles(query);
        return ResponseEntity.ok(pageInfo);
    }

    /**
     * 게시글 생성 요청 DTO
     */
    public record CreateArticleRequest(
        @NotBlank(message = "제목은 필수입니다")
        String title,

        @NotBlank(message = "내용은 필수입니다")
        String content,

        @NotBlank(message = "작성자 ID는 필수입니다")
        String writerId,

        @NotNull(message = "게시판 ID는 필수입니다")
        Long boardId,

        List<Long> keywordIds,
        LocalDateTime eventStartDate,
        LocalDateTime eventEndDate
    ) {}
}
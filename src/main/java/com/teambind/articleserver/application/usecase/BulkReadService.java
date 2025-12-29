package com.teambind.articleserver.application.usecase;

import com.teambind.articleserver.adapter.in.web.dto.response.ArticleBulkResponse;
import com.teambind.articleserver.adapter.out.persistence.projection.ArticleSimpleView;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.application.port.in.bulk.BulkReadUseCase;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 벌크 조회 서비스
 *
 * <p>Hexagonal Architecture의 Application Service입니다. 다중 게시글 일괄 조회 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class BulkReadService implements BulkReadUseCase {

  private final ArticleRepository articleRepository;

  @Override
  @Transactional(readOnly = true)
  public List<ArticleBulkResponse> fetchByIds(List<String> ids) {
    if (ids == null || ids.isEmpty()) return List.of();

    // Deduplicate while preserving first occurrence order
    LinkedHashSet<String> uniqueOrderedIds = new LinkedHashSet<>(ids);
    if (uniqueOrderedIds.size() > 30) {
      // limit to first 30 for performance as requested (20~30 typical)
      Iterator<String> it = uniqueOrderedIds.iterator();
      LinkedHashSet<String> limited = new LinkedHashSet<>();
      int count = 0;
      while (it.hasNext() && count < 30) {
        limited.add(it.next());
        count++;
      }
      uniqueOrderedIds = limited;
    }

    List<ArticleSimpleView> rows = articleRepository.findSimpleByIdIn(uniqueOrderedIds);

    // Map by id for O(1) reordering
    Map<String, ArticleSimpleView> byId =
        rows.stream().collect(Collectors.toMap(ArticleSimpleView::getId, v -> v, (a, b) -> a));

    List<ArticleBulkResponse> result = new ArrayList<>(uniqueOrderedIds.size());
    for (String id : uniqueOrderedIds) {
      ArticleSimpleView v = byId.get(id);
      if (v != null) {
        result.add(
            ArticleBulkResponse.builder()
                .articleId(v.getId())
                .title(v.getTitle())
                .content(v.getContent())
                .writerId(v.getWriterId())
                .boardId(v.getBoardId())
                .boardName(v.getBoardName())
                .articleType(v.getArticleType())
                .status(v.getStatus())
                .viewCount(v.getViewCount())
                .firstImageUrl(v.getFirstImageUrl())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build());
      }
    }
    return result;
  }
}

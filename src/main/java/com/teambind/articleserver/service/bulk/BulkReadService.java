package com.teambind.articleserver.service.bulk;

import com.teambind.articleserver.dto.response.ArticleSimpleResponse;
import com.teambind.articleserver.repository.ArticleRepository;
import com.teambind.articleserver.repository.projection.ArticleSimpleView;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BulkReadService {

  private final ArticleRepository articleRepository;

  @Transactional(readOnly = true)
  public List<ArticleSimpleResponse> fetchSimpleByIds(List<String> ids) {
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

    List<ArticleSimpleResponse> result = new ArrayList<>(uniqueOrderedIds.size());
    for (String id : uniqueOrderedIds) {
      ArticleSimpleView v = byId.get(id);
      if (v != null) {
        result.add(
            ArticleSimpleResponse.builder()
                .articleId(v.getId())
                .title(v.getTitle())
                .writerId(v.getWriterId())
                .version(v.getVersion())
                .createdAt(v.getCreatedAt())
                .build());
      }
    }
    return result;
  }
}

package com.teambind.articleserver.application.port.in.bulk;

import com.teambind.articleserver.adapter.in.web.dto.response.ArticleBulkResponse;
import java.util.List;

/**
 * 벌크 조회 UseCase (Inbound Port)
 *
 * <p>Hexagonal Architecture의 Inbound Port입니다. 다중 게시글 일괄 조회 작업을 정의합니다.
 */
public interface BulkReadUseCase {

  /**
   * ID 목록으로 게시글 일괄 조회 (content 미리보기 포함)
   *
   * @param ids 조회할 게시글 ID 목록
   * @return 게시글 정보 목록 (content 200자 미리보기 포함)
   */
  List<ArticleBulkResponse> fetchByIds(List<String> ids);
}

package com.teambind.articleserver.application.port.in.bulk;

import com.teambind.articleserver.adapter.in.web.dto.response.ArticleSimpleResponse;
import java.util.List;

/**
 * 벌크 조회 UseCase (Inbound Port)
 *
 * <p>Hexagonal Architecture의 Inbound Port입니다. 다중 게시글 일괄 조회 작업을 정의합니다.
 */
public interface BulkReadUseCase {

  /**
   * ID 목록으로 게시글 간략 정보 일괄 조회
   *
   * @param ids 조회할 게시글 ID 목록
   * @return 게시글 간략 정보 목록
   */
  List<ArticleSimpleResponse> fetchSimpleByIds(List<String> ids);
}

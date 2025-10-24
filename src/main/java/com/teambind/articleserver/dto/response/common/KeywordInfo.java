package com.teambind.articleserver.dto.response.common;

import com.teambind.articleserver.entity.keyword.Keyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 키워드 정보 DTO
 *
 * <p>게시글 응답에 포함되는 키워드 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordInfo {

  private Long keywordId;
  private String keywordName;
  private Boolean isCommon; // true: 공통 키워드, false: 보드 전용 키워드

  /**
   * Keyword 엔티티로부터 KeywordInfo 생성
   *
   * @param keyword 키워드 엔티티
   * @return KeywordInfo
   */
  public static KeywordInfo fromEntity(Keyword keyword) {
    if (keyword == null) {
      return null;
    }

    return KeywordInfo.builder()
        .keywordId(keyword.getId())
        .keywordName(keyword.getName())
        .isCommon(keyword.isCommonKeyword())
        .build();
  }
}

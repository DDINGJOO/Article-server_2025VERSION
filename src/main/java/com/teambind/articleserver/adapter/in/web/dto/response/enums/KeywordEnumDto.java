package com.teambind.articleserver.adapter.in.web.dto.response.enums;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * 키워드 Enum 응답 DTO
 *
 * <p>키워드 정보를 클라이언트에게 전달하기 위한 DTO입니다.
 * API Gateway와 통합을 위해 URL 정보를 포함합니다.
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeywordEnumDto {
    /** 키워드 ID */
    private Long keywordId;

    /** 키워드 이름 */
    private String keywordName;

    /** 공통 키워드 여부 */
    private Boolean isCommon;

    /** 보드 전용 키워드의 게시판 ID (공통 키워드는 null) */
    private Long boardId;

    /** 보드 전용 키워드의 게시판 이름 (공통 키워드는 null) */
    private String boardName;

    /** 키워드 URL */
    private String url;

    /** 키워드 활성화 여부 */
    private Boolean isActive;

    /** 키워드 사용 빈도 */
    private Integer usageCount;
}
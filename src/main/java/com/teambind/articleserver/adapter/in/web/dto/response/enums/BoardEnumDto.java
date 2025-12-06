package com.teambind.articleserver.adapter.in.web.dto.response.enums;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * 게시판 Enum 응답 DTO
 *
 * <p>게시판 정보를 클라이언트에게 전달하기 위한 DTO입니다.
 * API Gateway와 통합을 위해 URL 정보를 포함합니다.
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardEnumDto {
    /** 게시판 ID */
    private Long boardId;

    /** 게시판 이름 */
    private String boardName;

    /** 게시판 설명 */
    private String description;

    /** 게시판 URL */
    private String url;

    /** 게시판 활성화 여부 */
    private Boolean isActive;

    /** 게시판 표시 순서 */
    private Integer displayOrder;
}
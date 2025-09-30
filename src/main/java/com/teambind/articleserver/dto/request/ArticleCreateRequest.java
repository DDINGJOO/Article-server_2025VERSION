package com.teambind.articleserver.dto.request;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleCreateRequest {
	private String title;
	private String content;
	private Long boardId;
	private String writerId;
	private List<?> keywords;
	
}

package com.teambind.articleserver.dto.request;

import lombok.*;

import java.time.LocalDateTime;
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
	private String writerId;
	
	private LocalDateTime eventStartDate;
	private LocalDateTime eventEndDate;
	
	
	
	private List<?> keywords;
	private Object board;
	
}

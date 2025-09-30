package com.teambind.articleserver.dto.response;


import com.teambind.articleserver.entity.Board;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {
	private String articleId;
	private String title;
	private String content;
	private String writerId;
	
	private Board board;
	
	private LocalDateTime LastestUpdateId;
	private List<String> imageUrls;
	private List<Map<Integer, String>> keywords;
}

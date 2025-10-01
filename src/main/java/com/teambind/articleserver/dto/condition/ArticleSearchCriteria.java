package com.teambind.articleserver.dto.condition;

import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.entity.enums.Status;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ArticleSearchCriteria {

  // 키워드 아이디 -> 키워드 아이디를 갖고있는 게시물(by Keyword Mapping Table)
  private List<Keyword> keywords;
  private Board board;
  private String title;
  private String content;
  private List<String> writerId;
  private Status status;
}

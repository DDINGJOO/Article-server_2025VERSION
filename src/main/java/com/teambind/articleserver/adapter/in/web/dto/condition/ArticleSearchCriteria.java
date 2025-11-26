package com.teambind.articleserver.adapter.in.web.dto.condition;

import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
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
  private String writerId; // 단일 값으로 변경
  private Status status;
}

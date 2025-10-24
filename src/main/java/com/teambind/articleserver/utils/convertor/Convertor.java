package com.teambind.articleserver.utils.convertor;

import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.keyword.Keyword;
import java.util.List;

public interface Convertor {
  // New unified methods
  List<Keyword> convertKeywords(List<?> keywordList);
  Board convertBoard(Object board);
  
  
}

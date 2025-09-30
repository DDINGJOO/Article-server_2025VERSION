package com.teambind.articleserver.utils.convertor.impl;

import static com.teambind.articleserver.utils.DataInitializer.boardMap;
import static com.teambind.articleserver.utils.DataInitializer.boardMapReverse;
import static com.teambind.articleserver.utils.DataInitializer.keywordMap;
import static com.teambind.articleserver.utils.DataInitializer.keywordMapReverse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConvertorImplTest {

  private Convertor convertor;

  @BeforeEach
  void setUp() {
    convertor = new ConvertorImpl();
    // prepare in-memory maps (no Spring/Data needed)
    keywordMap.clear();
    keywordMapReverse.clear();
    boardMap.clear();
    boardMapReverse.clear();

    keywordMap.put(1L, "k1");
    keywordMap.put(2L, "k2");
    keywordMapReverse.put("k1", 1L);
    keywordMapReverse.put("k2", 2L);

    boardMap.put("b1", 10L);
    boardMapReverse.put(10L, "b1");
  }

  @Test
  @DisplayName("convertKeywords(List<?>): Long 리스트를 Keyword 리스트로 변환")
  void convertKeywords_withIds() {
    List<Keyword> result = convertor.convertKeywords(Arrays.asList(1L, 2L));
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo(1L);
    assertThat(result.get(0).getKeyword()).isEqualTo("k1");
    assertThat(result.get(1).getId()).isEqualTo(2L);
    assertThat(result.get(1).getKeyword()).isEqualTo("k2");
  }

  @Test
  @DisplayName("convertKeywords(List<?>): String 리스트를 Keyword 리스트로 변환")
  void convertKeywords_withNames() {
    List<Keyword> result = convertor.convertKeywords(Arrays.asList("k1", "k2"));
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo(1L);
    assertThat(result.get(0).getKeyword()).isEqualTo("k1");
    assertThat(result.get(1).getId()).isEqualTo(2L);
    assertThat(result.get(1).getKeyword()).isEqualTo("k2");
  }

  @Test
  @DisplayName("convertBoard(Object): Long 또는 String 값을 받아 Board 로 변환")
  void convertBoard_dynamic() {
    Board b1 = convertor.convertBoard(10L);
    assertThat(b1.getId()).isEqualTo(10L);
    assertThat(b1.getBoardName()).isEqualTo("b1");

    Board b2 = convertor.convertBoard("b1");
    assertThat(b2.getId()).isEqualTo(10L);
    assertThat(b2.getBoardName()).isEqualTo("b1");
  }

  @Test
  @DisplayName("convertKeywords: null 입력이면 CustomException")
  void convertKeywords_null() {
    assertThrows(CustomException.class, () -> convertor.convertKeywords(null));
  }

  @Test
  @DisplayName("convertBoard: null 입력이면 CustomException")
  void convertBoard_null() {
    assertThrows(CustomException.class, () -> convertor.convertBoard(null));
  }
}

package com.teambind.articleserver.utils.convertor.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.repository.BoardRepository;
import com.teambind.articleserver.repository.KeywordRepository;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConvertorImplTest {

  private Convertor convertor;
  private KeywordRepository keywordRepository;
  private BoardRepository boardRepository;

  @BeforeEach
  void setUp() {
    keywordRepository = Mockito.mock(KeywordRepository.class);
    boardRepository = Mockito.mock(BoardRepository.class);
    convertor = new ConvertorImpl(keywordRepository, boardRepository);

    // stubs for keywords by ids
    when(keywordRepository.findAllById(eq(Arrays.asList(1L, 2L))))
        .thenReturn(
            Arrays.asList(
                Keyword.builder().id(1L).keyword("k1").build(),
                Keyword.builder().id(2L).keyword("k2").build()));
    // stubs for keywords by names
    when(keywordRepository.findAllByKeywordIn(eq(Arrays.asList("k1", "k2"))))
        .thenReturn(
            Arrays.asList(
                Keyword.builder().id(1L).keyword("k1").build(),
                Keyword.builder().id(2L).keyword("k2").build()));

    // stubs for board by id and name
    when(boardRepository.findById(eq(10L)))
        .thenReturn(Optional.of(Board.builder().id(10L).boardName("b1").build()));
    when(boardRepository.findByBoardName(eq("b1")))
        .thenReturn(Optional.of(Board.builder().id(10L).boardName("b1").build()));
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
  @DisplayName("convertKeywords(List<?>): Int형 리스트를 Keyword 리스트로 변환")
  void convertKeywords_withIntValuesIds() {
    // convertor will convert ints to longs and call findAllById([1L,2L])
    List<Keyword> result = convertor.convertKeywords(Arrays.asList(1, 2));
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
  @DisplayName("convertBoard(Object): Long, Int 또는 String 값을 받아 Board 로 변환")
  void convertBoard_dynamic() {
    Board b1 = convertor.convertBoard(10L);
    assertThat(b1.getId()).isEqualTo(10L);
    assertThat(b1.getBoardName()).isEqualTo("b1");

    Board b3 = convertor.convertBoard(10);
    assertThat(b3.getId()).isEqualTo(10L);
    assertThat(b3.getBoardName()).isEqualTo("b1");

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

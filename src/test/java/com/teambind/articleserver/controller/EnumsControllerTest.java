package com.teambind.articleserver.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teambind.articleserver.utils.DataInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EnumsController.class)
class EnumsControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("GET /api/enums/boards 맵형태의 이넘리스트들을 응답 받는다.")
  void getBoards_returnsMap() throws Exception {
    // seed static map without loading DB
    DataInitializer.boardMapReverse.clear();
    DataInitializer.boardMapReverse.put(1L, "공지사항");

    mockMvc
        .perform(get("/api/enums/boards").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.['1']").value("공지사항"));
  }
}

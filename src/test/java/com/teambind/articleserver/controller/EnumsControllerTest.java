package com.teambind.articleserver.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EnumsControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("GET /api/enums/boards 맵형태의 이넘리스트들을 응답 받는다.")
  void getBoards_returnsMap() throws Exception {
    mockMvc
        .perform(get("/api/enums/boards").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        // expect at least id 1 -> '공지사항' from data.sql
        .andExpect(jsonPath("$.['1']").value("공지사항"));
  }
}

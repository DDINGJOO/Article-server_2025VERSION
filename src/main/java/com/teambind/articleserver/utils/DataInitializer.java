package com.teambind.articleserver.utils;

import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.keyword.Keyword;
import com.teambind.articleserver.repository.BoardRepository;
import com.teambind.articleserver.repository.KeywordRepository;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class DataInitializer {
  public static final Map<Long, Keyword> keywordMap = new ConcurrentHashMap<>() {};
  public static final Map<Long, Board> boardMap = new ConcurrentHashMap<>();

	private final BoardRepository boardRepository;
	private final KeywordRepository keywordRepository;
	
	
	@PostConstruct
	public void init() {
		log.info("DataInitializer init Started");
    getKeywordMap();
    getBoardMap();
    log.info("DataInitializer init keywordMap size: {}", keywordMap.size());
    log.info("DataInitializer init boardMap size: {}", boardMap.size());
		log.info("DataInitializer init Completed");
	}

  @Scheduled(cron = "0 0 0 * * *")
  public void refresh() {
    log.info("DataInitializer refresh Started");
    getKeywordMap();
    getBoardMap();
    log.info("DataInitializer refresh keywordMap size: {}", keywordMap.size());
    log.info("DataInitializer refresh boardMap size: {}", boardMap.size());
    log.info("DataInitializer refresh Completed");
  }

  public void getKeywordMap() {
    synchronized (keywordMap) {
      keywordMap.clear();
      keywordRepository
          .findAll()
          .forEach(
              keyword -> {
                keywordMap.put(keyword.getId(), keyword);
              });
    }
  }

  public void getBoardMap() {
    synchronized (boardMap) {
      boardMap.clear();
      boardRepository
          .findAll()
          .forEach(
              board -> {
                boardMap.put(board.getId(), board);
              });
    }
  }
}

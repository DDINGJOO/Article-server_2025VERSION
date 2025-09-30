package com.teambind.articleserver.utils;


import com.teambind.articleserver.repository.BoardRepository;
import com.teambind.articleserver.repository.KeywordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
	public static Map<Long, String> keywordMap = new HashMap<>();
	public static Map<String, Long> keywordMapReverse = new HashMap<>();
	
	
	private final BoardRepository boardRepository;
	private final KeywordRepository keywordRepository;
	
	@PostConstruct
	public void init() {
		log.info("DataInitializer init Started");
		
		keywordRepository.findAll().forEach(keyword -> {
			keywordMap.put(keyword.getId(), keyword.getKeyword());
			keywordMapReverse.put(keyword.getKeyword(), keyword.getId());
		});
		
		log.info("keywordMap size : {}", keywordMap.size());
		keywordMap.forEach((k, v) -> {
			log.info("keywordMap keyword : {}, id : {}", v, k);
		});
		
		log.info("keywordMapReverse size : {}", keywordMapReverse.size());
		keywordMapReverse.forEach((k, v) -> {
			log.info("keywordMapReverse keyword : {}, id : {}", k, v);
		});
		
		log.info("DataInitializer init Completed");
	}
}

package com.teambind.articleserver.utils;


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
	private final KeywordRepository keywordRepository;
	
	@PostConstruct
	public void init() {
		keywordRepository.findAll().forEach(keyword -> {
			keywordMap.put(keyword.getId(), keyword.getKeyword());
		});
		
		log.info("keywordMap size : {}", keywordMap.size());
		keywordMap.forEach((k, v) -> {
			log.info("keywordMap keyword : {}, id : {}", v, k);
		});
	}
}

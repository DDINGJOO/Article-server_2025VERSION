package com.teambind.articleserver.service.impl;


import com.teambind.articleserver.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleCreateService {
	
	private final ArticleRepository articleRepository;
	
	public void createArticle() {
	
	}
}

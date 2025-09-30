package com.teambind.articleserver.utils.validator.impl;

import com.teambind.articleserver.exceptions.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatorImplTest {
	
	@Test
	@DisplayName("파마미터로 받은 보드ID 가 null 이면 예외를 던진다.")
	void boardIdValidator1() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.boardIdValidator(null);
		});
	}
	
	@Test
	@DisplayName("파마미터로 받은 보드ID 가 제공하지 않는 ID 값을 갖는다")
	void boardIdValidator2() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.boardIdValidator(-9999L);
		});
	}
	
	@Test
	@DisplayName("파라미터로 받은 보드명이 null 이면 예외를 던진다.")
	void boardNameValidator1() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.boardNameValidator(null);
		});
	}
	
	@Test
	@DisplayName("파라미터로 받은 보드명이 제공하지 않는 보드명이면 예외를 던진다.")
	void boardNameValidator2() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.boardNameValidator("ㅁㄴㅇㅁ재엊ㅁ야ㅗㅓㅁ지ㅏㅓㅇ로ㅜ마ㅓㄹ");
		});
	}
	
	
	@Test
	@DisplayName("파라미터로 받은 키워드ID 가 null 이면 예외를 던진다.")
	void keywordIdValidator1() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.keywordIdValidator(null);
		});
	}
	
	@Test
	@DisplayName("파라미터로 받은 키워드ID 가 제공하지 않는 ID 값을 갖는다")
	void keywordIdValidator2() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.keywordIdValidator(-9999L);
		});
	}
	
	@Test
	@DisplayName("파라미터로 받은 키워드명이 null 이면 예외를 던진다.")
	void keywordNameValidator1() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.keywordNameValidator(null);
		});
	}
	
	@Test
	@DisplayName("파라미터로 받은 키워드명이 제공하지 않는 키워드명이면 예외를 던진다.")
	void keywordNameValidator2() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.keywordNameValidator("adhjdlkiawhjdlaiwjdlawij");
		});
	}
	
	@Test
	@DisplayName("파라미터의 타입에 따라 해당하는 메서드가 호출된다.")
	void boardValidator() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.boardValidator(null);
		});
		
		assertThrows(CustomException.class, () -> {
			validator.boardValidator("test");
		});
		
		assertThrows(CustomException.class, () -> {
			validator.boardValidator(999L);
		});
	}
	
	@Test
	void keywordValidator() {
		ValidatorImpl validator = new ValidatorImpl();
		
		assertThrows(CustomException.class, () -> {
			validator.keywordValidator(null);
		});
		
		assertThrows(CustomException.class, () -> {
			validator.keywordValidator("test");
		});
		
		assertThrows(CustomException.class, () -> {
			validator.keywordValidator(999L);
		});
	}
}

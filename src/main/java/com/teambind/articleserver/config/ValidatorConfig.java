package com.teambind.articleserver.config;


import com.teambind.articleserver.utils.validator.Validator;
import com.teambind.articleserver.utils.validator.impl.ValidatorImpl;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidatorConfig {
	public Validator validator() {
		return new ValidatorImpl();
	}
}

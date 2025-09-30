package com.teambind.articleserver.config;

import com.teambind.articleserver.utils.convertor.Convertor;
import com.teambind.articleserver.utils.convertor.impl.ConvertorImpl;
import com.teambind.articleserver.utils.validator.Validator;
import com.teambind.articleserver.utils.validator.impl.ValidatorImpl;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomConfig {
  public Validator validator() {
    return new ValidatorImpl();
  }

  public Convertor convertor() {
    return new ConvertorImpl();
  }
}

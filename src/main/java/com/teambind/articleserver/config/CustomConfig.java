package com.teambind.articleserver.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.teambind.articleserver.utils.generator.primay_key.PrimaryKetGenerator;
import com.teambind.articleserver.utils.generator.primay_key.Snowflake;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomConfig {
  // Explicit beans for validator/convertor are not annotated to avoid duplications
  // since their implementations are already annotated with @Component.

  @Bean
  public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
    return new JPAQueryFactory(entityManager);
  }

  @Bean
  public PrimaryKetGenerator keyGenerator() {
    return new Snowflake();
  }
}

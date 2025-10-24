package com.teambind.articleserver.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * ShedLock 설정
 *
 * <p>스케줄러 작업에 대한 분산 락 제공 여러 인스턴스가 동시에 실행되는 환경에서 하나의 인스턴스만 스케줄 작업을 실행하도록 보장
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

  /**
   * Redis 기반 Lock Provider 설정
   *
   * @param connectionFactory Redis 연결 팩토리
   * @return LockProvider 인스턴스
   */
  @Bean
  public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
    return new RedisLockProvider(connectionFactory, "article-server");
  }
}

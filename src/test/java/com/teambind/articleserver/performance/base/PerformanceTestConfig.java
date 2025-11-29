package com.teambind.articleserver.performance.base;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 성능 테스트를 위한 기본 설정 Docker Compose에 정의된 값들을 사용 */
@TestConfiguration
@EnableAsync
@Profile("performance-test")
public class PerformanceTestConfig {

  /** 병렬 데이터 생성을 위한 Thread Pool 설정 CPU 코어 수에 맞춰 자동 설정 */
  @Bean(name = "performanceTestExecutor")
  public ThreadPoolTaskExecutor performanceTestExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    int cores = Runtime.getRuntime().availableProcessors();

    executor.setCorePoolSize(cores);
    executor.setMaxPoolSize(cores * 2);
    executor.setQueueCapacity(1000);
    executor.setThreadNamePrefix("PerfTest-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();

    return executor;
  }

  /** 데이터 생성 배치 설정 */
  @Bean
  public DataGenerationConfig defaultDataGenerationConfig() {
    return DataGenerationConfig.builder()
        .batchSize(10_000)
        .parallelism(Runtime.getRuntime().availableProcessors())
        .flushInterval(1000)
        .build();
  }
}

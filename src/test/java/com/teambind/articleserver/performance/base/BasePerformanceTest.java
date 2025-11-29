package com.teambind.articleserver.performance.base;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** 성능 테스트를 위한 기본 클래스 Docker Compose 환경과 연동 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("performance-test")
@TestPropertySource(
    properties = {
      // Docker Compose에 정의된 MariaDB 설정
      "spring.datasource.url=jdbc:mariadb://localhost:13306/article_db",
      "spring.datasource.username=root",
      "spring.datasource.password=articlepass123",
      "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",

      // HikariCP 연결 풀 설정 (성능 테스트용 최적화)
      "spring.datasource.hikari.maximum-pool-size=50",
      "spring.datasource.hikari.minimum-idle=10",
      "spring.datasource.hikari.connection-timeout=30000",
      "spring.datasource.hikari.idle-timeout=600000",
      "spring.datasource.hikari.max-lifetime=1800000",
      "spring.datasource.hikari.leak-detection-threshold=60000",

      // JPA 설정 (성능 테스트용)
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect",
      "spring.jpa.properties.hibernate.show_sql=false",
      "spring.jpa.properties.hibernate.format_sql=false",
      "spring.jpa.properties.hibernate.use_sql_comments=false",

      // 배치 성능 최적화
      "spring.jpa.properties.hibernate.jdbc.batch_size=1000",
      "spring.jpa.properties.hibernate.order_inserts=true",
      "spring.jpa.properties.hibernate.order_updates=true",
      "spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true",

      // Fetch 최적화
      "spring.jpa.properties.hibernate.default_batch_fetch_size=100",

      // 통계 수집 (성능 분석용)
      "spring.jpa.properties.hibernate.generate_statistics=true",

      // 2차 캐시 비활성화 (순수 DB 성능 측정)
      "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
      "spring.jpa.properties.hibernate.cache.use_query_cache=false",

      // Docker Compose에 정의된 Redis 설정
      "spring.data.redis.host=localhost",
      "spring.data.redis.port=16379",
      "spring.cache.type=redis",

      // Docker Compose에 정의된 Kafka 설정
      "spring.kafka.bootstrap-servers=localhost:19092",
      "spring.kafka.consumer.group-id=performance-test-group",
      "spring.kafka.consumer.auto-offset-reset=earliest",

      // 로깅 레벨 (성능 테스트 시 최소화)
      "logging.level.org.hibernate.SQL=WARN",
      "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=WARN",
      "logging.level.org.springframework.web=WARN",
      "logging.level.com.teambind.articleserver=INFO"
    })
@Tag("performance")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BasePerformanceTest {

  /** 테스트 데이터 접두사 실제 데이터와 구분하기 위해 사용 */
  protected static final String TEST_DATA_PREFIX = "PERF_";

  /** 기본 타임아웃 설정 (밀리초) */
  protected static final int DEFAULT_TIMEOUT_MS = 60_000;

  /** 워밍업 반복 횟수 */
  protected static final int DEFAULT_WARMUP_ITERATIONS = 100;

  /** 측정 반복 횟수 */
  protected static final int DEFAULT_MEASUREMENT_ITERATIONS = 1000;
}

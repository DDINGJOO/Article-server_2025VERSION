package com.teambind.articleserver.config;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트 환경용 ID 생성기 설정
 *
 * <p>프로덕션에서는 Snowflake 분산 ID를 사용하지만, 테스트 환경에서는 간단한 순차 ID를 사용합니다.
 *
 * <p>사용법:
 *
 * <pre>
 * &#64;DataJpaTest
 * &#64;Import(TestIdGeneratorConfig.class)
 * class ArticleRepositoryTest {
 *     &#64;Autowired
 *     private TestIdGenerator idGenerator;
 *
 *     &#64;Test
 *     void test() {
 *         EventArticle article = EventArticle.builder()
 *             .title("테스트")
 *             .build();
 *         article.setId(idGenerator.generate());
 *     }
 * }
 * </pre>
 */
@TestConfiguration
public class TestIdGeneratorConfig {

  @Bean
  @Primary
  public TestIdGenerator testIdGenerator() {
    return new TestIdGenerator();
  }

  /**
   * 테스트용 ID 생성기
   *
   * <p>형식: TEST-{순번} (예: TEST-1000, TEST-1001, ...)
   */
  public static class TestIdGenerator {
    private final AtomicLong counter = new AtomicLong(1000);

    /**
     * 테스트용 ID 생성
     *
     * @return 생성된 ID (예: "TEST-1000")
     */
    public String generate() {
      return "TEST-" + counter.getAndIncrement();
    }

    /** 카운터 리셋 (테스트 간 격리가 필요한 경우) */
    public void reset() {
      counter.set(1000);
    }

    /**
     * 현재 카운터 값 조회
     *
     * @return 현재 카운터 값
     */
    public long getCurrentCount() {
      return counter.get();
    }
  }
}

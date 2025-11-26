package com.teambind.articleserver.performance.data;

import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import com.teambind.articleserver.adapter.out.persistence.repository.BoardRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.KeywordRepository;
import com.teambind.articleserver.common.util.generator.primay_key.Snowflake;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 성능 테스트용 대량 데이터 생성기
 *
 * <p>목표: - 게시글 60만 건 - 이미지 180만 개 (게시글당 3개) - 키워드 240만 개 매핑 (게시글당 4개)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceDataGenerator {

  // 미리 정의된 테스트 데이터 템플릿
  private static final String[] TITLE_TEMPLATES = {
    "성능 테스트 게시글 #%d",
    "Spring Boot 최적화 방법 #%d",
    "JPA N+1 문제 해결 사례 #%d",
    "MSA 아키텍처 설계 패턴 #%d",
    "도메인 주도 설계 실습 #%d"
  };
  private static final String[] CONTENT_TEMPLATES = {
    "이것은 성능 테스트를 위한 본문입니다. 게시글 번호: %d\n실제 운영 환경과 유사한 텍스트 길이를 만들기 위해 충분한 내용을 포함합니다.",
    "JPA와 Hibernate를 사용할 때 발생하는 성능 문제와 해결 방법에 대한 상세한 설명입니다. 인덱스 %d",
    "마이크로서비스 아키텍처에서 트랜잭션 관리와 분산 데이터 처리 방법론. 문서 번호 %d",
    "DDD를 적용한 실제 프로젝트 경험과 학습한 내용 정리. 게시글 ID: %d",
    "대용량 트래픽 처리를 위한 시스템 설계와 최적화 전략. 참조 번호: %d"
  };
  private static final String[] IMAGE_URL_TEMPLATES = {
    "https://cdn.example.com/images/article/%s/image_%d.jpg",
    "https://storage.example.com/photos/%s/pic_%d.png",
    "https://media.example.com/content/%s/img_%d.webp"
  };
  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;
  private final BoardRepository boardRepository;
  private final KeywordRepository keywordRepository;
  private final Snowflake snowflake;
  private final Random random = new Random();
  private final AtomicInteger progressCounter = new AtomicInteger(0);
  @Value("${performance.test.batch-size:1000}")
  private int batchSize;
  @Value("${performance.test.parallel-threads:8}")
  private int parallelThreads;

  /** 테스트 데이터 생성 메인 메서드 */
  @Transactional
  public void generateTestData(int articleCount) {
    log.info("=== 성능 테스트 데이터 생성 시작 ===");
    log.info("목표: 게시글 {}건, 이미지 {}개, 키워드 매핑 {}개", articleCount, articleCount * 3, articleCount * 4);

    try {
      // 1. Board 데이터 준비
      List<Board> boards = prepareBoards();

      // 2. Keyword 데이터 준비
      List<Keyword> keywords = prepareKeywords();

      // 3. Article 대량 생성 (병렬 처리)
      long startTime = System.currentTimeMillis();
      generateArticlesInBatch(articleCount, boards, keywords);

      long elapsed = System.currentTimeMillis() - startTime;
      log.info("=== 데이터 생성 완료 ===");
      log.info("총 소요 시간: {}초", elapsed / 1000);
      log.info("초당 생성 속도: {} articles/sec", articleCount / (elapsed / 1000.0));

    } catch (Exception e) {
      log.error("데이터 생성 중 오류 발생", e);
      throw new RuntimeException("Performance data generation failed", e);
    }
  }

  /** Board 데이터 준비 (없으면 생성) */
  private List<Board> prepareBoards() {
    List<Board> boards = boardRepository.findAll();

    if (boards.isEmpty()) {
      log.info("Board 데이터 생성 중...");
      String[] boardNames = {"공지사항", "자유게시판", "질문답변", "기술블로그", "이벤트"};

      for (String name : boardNames) {
        Board board =
            Board.builder()
                .name(name)
                .description(name + " 게시판입니다.")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        boards.add(boardRepository.save(board));
      }
      log.info("Board {} 개 생성 완료", boards.size());
    }

    return boards;
  }

  /** Keyword 데이터 준비 (없으면 생성) */
  private List<Keyword> prepareKeywords() {
    List<Keyword> keywords = keywordRepository.findAll();

    if (keywords.size() < 100) {
      log.info("Keyword 데이터 생성 중...");
      String[] keywordNames = {
        "Spring",
        "Java",
        "JPA",
        "Hibernate",
        "MySQL",
        "Redis",
        "Kafka",
        "Docker",
        "Kubernetes",
        "AWS",
        "MSA",
        "DDD",
        "Performance",
        "Optimization",
        "Testing",
        "Security",
        "API",
        "REST",
        "GraphQL",
        "WebSocket"
      };

      for (String name : keywordNames) {
        // 공통 키워드
        Keyword keyword =
            Keyword.builder()
                .name(name)
                .board(null) // null이면 공통 키워드
                .isActive(true)
                .usageCount(0)
                .build();
        keywords.add(keywordRepository.save(keyword));
      }
      log.info("Keyword {} 개 생성 완료", keywords.size());
    }

    return keywords;
  }

  /** Article 대량 생성 (JDBC Batch Insert 활용) */
  private void generateArticlesInBatch(int totalCount, List<Board> boards, List<Keyword> keywords) {
    ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    int recordsPerThread = totalCount / parallelThreads;

    for (int i = 0; i < parallelThreads; i++) {
      final int threadIndex = i;
      final int startIndex = i * recordsPerThread;
      final int endIndex = (i == parallelThreads - 1) ? totalCount : startIndex + recordsPerThread;

      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                try {
                  insertArticleBatch(startIndex, endIndex, boards, keywords, threadIndex);
                } catch (SQLException e) {
                  log.error("Thread {} 실행 중 오류", threadIndex, e);
                  throw new RuntimeException(e);
                }
              },
              executor);

      futures.add(future);
    }

    // 모든 스레드 완료 대기
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executor.shutdown();

    try {
      executor.awaitTermination(10, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Data generation interrupted", e);
    }
  }

  /** 배치 단위로 Article, Image, Keyword 매핑 삽입 */
  private void insertArticleBatch(
      int startIndex, int endIndex, List<Board> boards, List<Keyword> keywords, int threadIndex)
      throws SQLException {

    String articleSql =
        """
            INSERT INTO articles (
                article_id, article_type, title, contents, writer_id,
                board_id, status, first_image_url, view_count,
                created_at, updated_at, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    String imageSql =
        """
            INSERT INTO article_images (
                article_id, sequence_num, article_image_url, image_id
            ) VALUES (?, ?, ?, ?)
            """;

    String keywordMappingSql =
        """
            INSERT INTO keyword_mapping_table (
                article_id, keyword_id
            ) VALUES (?, ?)
            """;

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);

      try (PreparedStatement articlePs = conn.prepareStatement(articleSql);
          PreparedStatement imagePs = conn.prepareStatement(imageSql);
          PreparedStatement keywordPs = conn.prepareStatement(keywordMappingSql)) {

        int batchCount = 0;

        for (int i = startIndex; i < endIndex; i++) {
          String articleId = String.valueOf(snowflake.nextId());
          LocalDateTime now = LocalDateTime.now();
          Board board = boards.get(random.nextInt(boards.size()));

          // Article 데이터
          articlePs.setString(1, articleId);
          articlePs.setString(2, getArticleType(i));
          articlePs.setString(3, String.format(TITLE_TEMPLATES[i % TITLE_TEMPLATES.length], i));
          articlePs.setString(4, String.format(CONTENT_TEMPLATES[i % CONTENT_TEMPLATES.length], i));
          articlePs.setString(5, "user_" + (i % 1000)); // 1000명의 사용자 시뮬레이션
          articlePs.setLong(6, board.getId());
          articlePs.setString(7, Status.ACTIVE.name());
          articlePs.setString(8, String.format(IMAGE_URL_TEMPLATES[0], articleId, 1));
          articlePs.setLong(9, random.nextInt(10000)); // 조회수 0-9999
          articlePs.setTimestamp(10, Timestamp.valueOf(now.minusDays(random.nextInt(365))));
          articlePs.setTimestamp(11, Timestamp.valueOf(now));
          articlePs.setLong(12, 0L);
          articlePs.addBatch();

          // Image 데이터 (게시글당 3개)
          for (int j = 1; j <= 3; j++) {
            imagePs.setString(1, articleId);
            imagePs.setLong(2, j);
            imagePs.setString(3, String.format(IMAGE_URL_TEMPLATES[j % 3], articleId, j));
            imagePs.setString(4, "img_" + articleId + "_" + j);
            imagePs.addBatch();
          }

          // Keyword 매핑 (게시글당 4개)
          List<Long> selectedKeywordIds = selectRandomKeywords(keywords, 4);
          for (Long keywordId : selectedKeywordIds) {
            keywordPs.setString(1, articleId);
            keywordPs.setLong(2, keywordId);
            keywordPs.addBatch();
          }

          batchCount++;

          // 배치 실행
          if (batchCount >= batchSize) {
            articlePs.executeBatch();
            imagePs.executeBatch();
            keywordPs.executeBatch();
            conn.commit();

            int progress = progressCounter.addAndGet(batchCount);
            if (progress % 10000 == 0) {
              log.info(
                  "[Thread-{}] 진행률: {}/{} ({:.1f}%)",
                  threadIndex,
                  progress,
                  endIndex - startIndex,
                  (double) progress / (endIndex - startIndex) * 100);
            }

            batchCount = 0;
          }
        }

        // 남은 배치 처리
        if (batchCount > 0) {
          articlePs.executeBatch();
          imagePs.executeBatch();
          keywordPs.executeBatch();
          conn.commit();

          progressCounter.addAndGet(batchCount);
        }

      } catch (Exception e) {
        conn.rollback();
        throw e;
      }
    }
  }

  /** Article 타입 결정 (Regular 90%, Notice 5%, Event 5%) */
  private String getArticleType(int index) {
    int mod = index % 100;
    if (mod < 90) {
      return "RegularArticle";
    } else if (mod < 95) {
      return "NoticeArticle";
    } else {
      return "EventArticle";
    }
  }

  /** 랜덤하게 키워드 선택 */
  private List<Long> selectRandomKeywords(List<Keyword> keywords, int count) {
    List<Long> selected = new ArrayList<>();
    List<Keyword> shuffled = new ArrayList<>(keywords);

    for (int i = 0; i < Math.min(count, keywords.size()); i++) {
      int index = random.nextInt(shuffled.size());
      selected.add(shuffled.get(index).getId());
      shuffled.remove(index);
    }

    return selected;
  }

  /** 데이터 정리 (테스트 후 정리용) */
  @Transactional
  public void cleanupTestData() {
    log.info("테스트 데이터 정리 시작...");

    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
    jdbcTemplate.execute("TRUNCATE TABLE keyword_mapping_table");
    jdbcTemplate.execute("TRUNCATE TABLE article_images");
    jdbcTemplate.execute(
        "DELETE FROM articles WHERE title LIKE '성능 테스트%' OR title LIKE 'Spring Boot 최적화%'");
    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

    log.info("테스트 데이터 정리 완료");
  }

  /** 데이터 생성 상태 확인 */
  public void verifyDataGeneration() {
    Long articleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM articles", Long.class);
    Long imageCount =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM article_images", Long.class);
    Long keywordMappingCount =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM keyword_mapping_table", Long.class);

    log.info("=== 데이터 검증 결과 ===");
    log.info("Articles: {}", articleCount);
    log.info(
        "Images: {} (평균 {}/article)", imageCount, articleCount > 0 ? imageCount / articleCount : 0);
    log.info(
        "Keyword Mappings: {} (평균 {}/article)",
        keywordMappingCount,
        articleCount > 0 ? keywordMappingCount / articleCount : 0);
  }
}

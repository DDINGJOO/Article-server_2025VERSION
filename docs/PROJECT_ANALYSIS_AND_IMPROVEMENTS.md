# Article Server 프로젝트 분석 보고서

> 작성일: 2025-10-24
> 버전: 1.0.0
> 대상: Article Server (Spring Boot 3.5.6)

---

## 📋 프로젝트 개요

Spring Boot 3.5.6 기반의 게시글 관리 마이크로서비스로, Kafka 이벤트 기반 아키텍처를 활용하여 이미지 서비스와 통합되어 있습니다.

**기술 스택:** Java 17, Spring Boot 3.x, MariaDB, QueryDSL, Apache Kafka, Redis(비활성), Docker

---

## 🔴 보안 취약점 (Security Vulnerabilities)

> **참고:** 인증/인가는 별도의 인증 서버 및 API Gateway에서 처리되므로 이 서비스에서는 구현하지 않습니다.

### 1. **[높음] 테스트 환경 자격증명 하드코딩**

**파일:** `src/main/resources/application-test.yaml:8`

```yaml
spring:
  datasource:
    password: pass123#  # ← 버전 관리에 포함됨
```

**해결 방안:**

```yaml
# application-test.yaml
spring:
  datasource:
    password: ${TEST_DATABASE_PASSWORD:test_password}

# 또는 .env.test 파일 사용 (gitignore에 추가)
```

---

### 2. **[높음] SQL 쿼리 노출**

`application-dev.yaml`과 `application-test.yaml`에서 `show-sql: true` 설정으로 인해 로그에 모든 SQL 쿼리가 기록됩니다.

**해결 방안:**

```yaml
# application-prod.yaml
spring:
  jpa:
    show-sql: false  # 운영 환경에서는 반드시 false
    properties:
      hibernate:
        format_sql: false
```

---

### 3. **[중간] 입력 값 검증 부족**

**문제 코드:** `src/main/java/com/teambind/articleserver/dto/request/ArticleCreateRequestDto.java`

- DTOs에 `@NotBlank`, `@Length`, `@Pattern` 어노테이션 누락
- XSS, SQL Injection 방어 미흡

```java
public class ArticleCreateRequestDto {
	private String title;        // 길이 제한 없음
	private String content;      // HTML/Script 검증 없음
	private String writerId;     // 형식 검증 없음
}
```

**해결 방안:**

```java
public class ArticleCreateRequestDto {
	@NotBlank(message = "제목은 필수입니다")
	@Length(min = 1, max = 200, message = "제목은 1-200자 이내여야 합니다")
	private String title;
	
	@NotBlank(message = "내용은 필수입니다")
	@Length(min = 1, max = 10000, message = "내용은 10000자 이내여야 합니다")
	private String content;
	
	@NotBlank
	@Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "유효하지 않은 작성자 ID")
	private String writerId;
}

// 컨트롤러에 @Validated 추가
@PostMapping
public ResponseEntity<ArticleResponseDto> createArticle(
		@Validated @RequestBody ArticleCreateRequestDto request) { ...}
```

---

### 4. **[중간] HTTPS/SSL 미적용**

`application-test.yaml`에서 `useSSL=false` 설정 발견

**해결 방안:**

```yaml
# application-prod.yaml
spring:
  datasource:
    url: jdbc:mariadb://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_NAME}?useSSL=true&requireSSL=true

server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

---

## ⚠️ 코드 품질 이슈 (Code Quality)

### 1. **[높음] 중복 코드 - 검색 로직**

**파일:**

- `src/main/java/com/teambind/articleserver/controller/ArticleController.java:70-130`
- `src/main/java/com/teambind/articleserver/controller/RegularArticleController.java:60-120`

두 컨트롤러에서 거의 동일한 60여 줄의 검색 파라미터 처리 로직 중복

**해결 방안:**

```java
// 새로운 클래스: SearchParamsExtractor.java
@Component
public class SearchParamsExtractor {
	public ArticleSearchParams extract(
			String board, String title, String content,
			String keywords, String writers, String cursor, Integer size
	) {
		return ArticleSearchParams.builder()
				.board(decodeParam(board))
				.title(decodeParam(title))
				.content(decodeParam(content))
				.keywords(parseKeywords(keywords))
				.writers(parseWriters(writers))
				.cursor(decodeCursor(cursor))
				.size(Optional.ofNullable(size).orElse(10))
				.build();
	}
}

// 컨트롤러에서 사용
@GetMapping("/search")
public ResponseEntity<ArticleSearchResponseDto> searchArticles(
		@RequestParam Map<String, String> params) {
	
	var searchParams = searchParamsExtractor.extract(params);
	var result = articleReadService.searchArticles(searchParams);
	return ResponseEntity.ok(result);
}
```

---

### 2. **[중간] 예외 응답 정보 부족**

**파일:** `src/main/java/com/teambind/articleserver/exceptions/GlobalExceptionHandler.java`

**현재 응답 형식:**

```java
return ResponseEntity.status(ex.getStatus()).

body(ex.getMessage());
```

**문제점:**

- 에러 코드 없음
- 타임스탬프 없음
- 요청 경로 정보 없음
- 디버깅 어려움

**해결 방안:**

```java

@Data
@Builder
public class ErrorResponse {
	private String errorCode;
	private String message;
	private LocalDateTime timestamp;
	private String path;
	private Map<String, String> validationErrors;
}

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(ArticleNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleArticleNotFound(
			ArticleNotFoundException ex,
			HttpServletRequest request
	) {
		ErrorResponse response = ErrorResponse.builder()
				.errorCode("ARTICLE_NOT_FOUND")
				.message(ex.getMessage())
				.timestamp(LocalDateTime.now())
				.path(request.getRequestURI())
				.build();
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationErrors(
			MethodArgumentNotValidException ex,
			HttpServletRequest request
	) {
		Map<String, String> errors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						FieldError::getDefaultMessage
				));
		
		ErrorResponse response = ErrorResponse.builder()
				.errorCode("VALIDATION_ERROR")
				.message("입력 값 검증 실패")
				.timestamp(LocalDateTime.now())
				.path(request.getRequestURI())
				.validationErrors(errors)
				.build();
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
}
```

---

### 3. **[중간] Type Safety 부족 - DTO에서 와일드카드 사용**

**파일:** `src/main/java/com/teambind/articleserver/dto/request/ArticleCreateRequestDto.java`

```java
private List<?> keywords;      // 타입 불명확
private Object board;           // Object 사용
```

이로 인해 `ConvertorImpl.java`에서 런타임 타입 체크 필요:

```java
if(keywords instanceof
List<?> list){
		if(!list.

isEmpty()){
Object first = list.get(0);
        if(first instanceof Number){...}
		else if(first instanceof String){...}
		}
		}
```

**해결 방안:**

```java
// 명확한 타입 정의
public class ArticleCreateRequestDto {
	private List<Long> keywordIds;     // 또는 List<String> keywordNames
	private Long boardId;               // 또는 String boardName
	
	// 또는 Union 타입이 필요하면
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
	@JsonSubTypes({
			@JsonSubTypes.Type(value = KeywordIdList.class, name = "ids"),
			@JsonSubTypes.Type(value = KeywordNameList.class, name = "names")
	})
	private KeywordInput keywords;
}
```

---

### 4. **[낮음] TODO 주석 미완성**

**파일:** `src/main/java/com/teambind/articleserver/entity/Article.java:275`

```java
//TODO : // TEST PLZ!!
public void addKeywords(List<Keyword> keywords) { ...}
```

**액션:** 테스트 코드 작성 또는 TODO 제거

---

### 5. **[낮음] Null Safety 이슈**

**파일:** `src/main/java/com/teambind/articleserver/event/consume/KafkaConsumer.java:43`

```java
if(request.get(0).

getImageId() ==null||request.

get(0).

getImageId().

isEmpty())
```

`request` 자체가 비어있을 경우 `IndexOutOfBoundsException` 발생 가능

**해결 방안:**

```java
if(request ==null||request.

isEmpty()){
		log.

warn("Empty image change request received");
    return;
		    }

ImageChangeEvent firstImage = request.get(0);
if(firstImage.

getImageId() ==null||firstImage.

getImageId().

isEmpty()){
		article.

clearImages();
    return;
		    }
```

---

## 🚀 성능 개선 방안 (Performance Optimization)

### 1. **N+1 쿼리 문제**

**문제:**

- `Article` 엔티티의 `keywords`와 `images`가 `LAZY` 로딩
- 커서 검색 시 각 게시글마다 추가 쿼리 발생 가능

**해결 방안:**

```java
// ArticleRepositoryCustomImpl.java
@Override
public List<Article> searchArticles(ArticleSearchParams params) {
	QArticle article = QArticle.article;
	
	// EntityGraph 대신 fetch join 사용
	JPAQuery<Article> query = queryFactory
			.selectFrom(article)
			.distinct()
			.leftJoin(article.keywords, QKeywordMappingTable.keywordMappingTable).fetchJoin()
			.leftJoin(article.images, QArticleImage.articleImage).fetchJoin()
			.leftJoin(article.board, QBoard.board).fetchJoin()
			.where(buildPredicates(params))
			.orderBy(article.createdAt.desc(), article.id.desc())
			.limit(params.getSize() + 1);
	
	return query.fetch();
}
```

**또는 BatchSize 설정:**

```java

@Entity
public class Article {
	@OneToMany(mappedBy = "article")
	@BatchSize(size = 10)  // 10개 게시글 단위로 일괄 로드
	private List<KeywordMappingTable> keywords;
}
```

---

### 2. **정적 맵 초기화 성능**

**파일:** `src/main/java/com/teambind/articleserver/config/DataInitializer.java`

```java
public static Map<Long, String> keywordMap = new HashMap<>();
public static Map<Long, String> boardMap = new HashMap<>();
```

**문제:**

- 애플리케이션 시작 시 모든 데이터를 메모리에 로드
- 선형 검색으로 인한 O(n) 조회 시간
- 데이터가 많아질 경우 메모리 낭비

**해결 방안:**

```java
// Redis 캐싱 활용
@Service
public class KeywordCacheService {
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Autowired
	private KeywordRepository keywordRepository;
	
	@Cacheable(value = "keywords", key = "#id")
	public String getKeywordName(Long id) {
		return keywordRepository.findById(id)
				.map(Keyword::getKeyword)
				.orElseThrow(() -> new KeywordNotFoundException(id));
	}
	
	@CacheEvict(value = "keywords", key = "#id")
	public void evictKeyword(Long id) {
		// 캐시 무효화
	}
}

// application.yaml에서 Redis 활성화
spring:
data:
redis:
repositories:
enabled:true  #
현재 false로
설정됨
```

---

### 3. **Kafka 동기 전송**

**파일:** `src/main/java/com/teambind/articleserver/event/publish/KafkaPublisher.java`

```java
kafkaTemplate.send(TOPIC, articleUpdatedEvent);  // 동기 블로킹
```

**문제:** 전송 실패 시 예외 처리 없음, 블로킹으로 인한 성능 저하

**해결 방안:**

```java

@Service
public class KafkaPublisher {
	
	public void articleUpdatedEvent(ArticleCreatedEvent event) {
		try {
			String json = objectMapper.writeValueAsString(event);
			
			// 비동기 전송 with callback
			CompletableFuture<SendResult<String, String>> future =
					kafkaTemplate.send(TOPIC, json);
			
			future.whenComplete((result, ex) -> {
				if (ex != null) {
					log.error("Failed to send article event: articleId={}, error={}",
							event.getArticleId(), ex.getMessage());
					// 재시도 큐에 추가 또는 DLQ로 전송
				} else {
					log.debug("Article event sent successfully: articleId={}, offset={}",
							event.getArticleId(),
							result.getRecordMetadata().offset());
				}
			});
		
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize article event", e);
			throw new EventPublishException("Event serialization failed", e);
		}
	}
}
```

---

### 4. **DB 커넥션 풀 최적화**

현재 기본 설정 사용 중 - HikariCP 튜닝 필요

**해결 방안:**

```yaml
# application-prod.yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

---

## 🏗️ 아키텍처 개선 제안

### 1. **API Gateway 통합 권장**

현재 Nginx 로드밸런서만 사용 중 → API Gateway 추가 권장

**추천 솔루션:**

- Spring Cloud Gateway
- Kong
- AWS API Gateway

**이점:**

- 중앙 집중식 인증/인가
- Rate Limiting
- Request/Response 변환
- 라우팅 규칙 관리

---

### 2. **이벤트 소싱 패턴 도입 고려**

현재: 게시글 생성/수정 시 단순 이벤트 발행

**개선안:**

```java
// 모든 게시글 변경을 이벤트로 저장
@Entity
public class ArticleEvent {
	@Id
	private String eventId;
	private String articleId;
	private String eventType;  // CREATED, UPDATED, DELETED, IMAGE_CHANGED
	private String payload;
	private LocalDateTime occurredAt;
}

// 이벤트 스트림에서 현재 상태 재구성 가능
// 변경 이력 추적 가능
// 감사(Audit) 기능 구현 용이
```

---

### 3. **CQRS (Command Query Responsibility Segregation) 적용**

읽기와 쓰기 모델 분리

```java
// Command Model (쓰기)
@Service
public class ArticleCommandService {
	public String createArticle(CreateArticleCommand cmd) { ...}
	
	public void updateArticle(UpdateArticleCommand cmd) { ...}
}

// Query Model (읽기) - 별도 DB 또는 캐시
@Service
public class ArticleQueryService {
	public ArticleView getArticleById(String id) { ...}
	
	public Page<ArticleListView> searchArticles(SearchQuery query) { ...}
}
```

---

### 4. **멀티 테넌시(Multi-Tenancy) 강화**

현재 `Board` 기반 분리 → 조직/팀 단위 격리 추가

```java

@Entity
public class Article {
	@Column(nullable = false)
	private String tenantId;  // 조직 ID
	
	@Column(nullable = false)
	private String workspaceId;  // 워크스페이스 ID
}

// Hibernate Filter로 자동 필터링
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Article { ...
}
```

---

## 📊 모니터링 및 관찰성 (Observability)

### 1. **로깅 개선**

**현재 상태:**

- 기본 로깅만 사용
- 구조화된 로그 없음
- 추적 ID 없음

**해결 방안:**

```java
// 1. Logback JSON 인코더 추가
// build.gradle
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'

// 2. logback-spring.xml 설정
<appender name="JSON"class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
        <includeMdcKeyName>articleId</includeMdcKeyName>
    </encoder>
</appender>

// 3. MDC를 이용한 추적 ID 추가
@Component
public class TraceIdFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, ...) {
		String traceId = UUID.randomUUID().toString();
		MDC.put("traceId", traceId);
		response.setHeader("X-Trace-Id", traceId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
```

---

### 2. **메트릭 수집 - Micrometer + Prometheus**

```java
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'

// application.yaml
management:
endpoints:
web:
exposure:
include:health,info,prometheus,metrics
metrics:
tags:
application:article-server
instance:

$ {
	HOSTNAME
}

export:
prometheus:
enabled:true

// 커스텀 메트릭
@Service
public class ArticleCreateService {
	
	@Autowired
	private MeterRegistry meterRegistry;
	
	public Article createArticle(...) {
		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			Article article = // 생성 로직
					meterRegistry.counter("articles.created",
							"board", article.getBoard().getBoardName()).increment();
			return article;
		} finally {
			sample.stop(Timer.builder("article.create.duration")
					.register(meterRegistry));
		}
	}
}
```

---

### 3. **분산 추적 - Spring Cloud Sleuth + Zipkin**

```java
// build.gradle
implementation 'org.springframework.cloud:spring-cloud-starter-sleuth'
implementation 'org.springframework.cloud:spring-cloud-sleuth-zipkin'

// application.yaml
spring:
sleuth:
sampler:
probability:1.0  #개발:100%,운영:0.1(10%)
zipkin:
base-url:http://zipkin:9411
enabled:true
```

---

### 4. **Kafka Consumer Lag 모니터링**

```java

@Component
public class KafkaConsumerMetrics {
	
	@Autowired
	private MeterRegistry registry;
	
	@Scheduled(fixedRate = 60000)
	public void recordConsumerLag() {
		// Kafka Admin API로 컨슈머 그룹의 lag 확인
		AdminClient admin = AdminClient.create(kafkaProperties);
		Map<TopicPartition, Long> lag = // lag 계산 로직
				
				lag.forEach((tp, lagValue) -> {
					registry.gauge("kafka.consumer.lag",
							Tags.of("topic", tp.topic(), "partition", String.valueOf(tp.partition())),
							lagValue);
				});
	}
}
```

---

## 🧪 테스트 개선 방안

### 1. **Kafka 통합 테스트 추가**

**현재 상태:** Kafka Producer/Consumer 테스트 없음

**해결 방안:**

```java

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"article-created", "article-image-changed"})
class KafkaIntegrationTest {
	
	@Autowired
	private KafkaPublisher publisher;
	
	@Autowired
	private KafkaConsumer consumer;
	
	@Autowired
	private EmbeddedKafkaBroker embeddedKafka;
	
	@Test
	@DisplayName("게시글 생성 이벤트 발행 및 소비 테스트")
	void publishAndConsumeArticleCreatedEvent() {
		// given
		ArticleCreatedEvent event = ArticleCreatedEvent.builder()
				.articleId("test-123")
				.writerId("writer-1")
				.build();
		
		// when
		publisher.articleUpdatedEvent(event);
		
		// then
		await().atMost(5, TimeUnit.SECONDS)
				.untilAsserted(() -> {
					// 이벤트 소비 확인
					verify(someDependency).onArticleCreated(any());
				});
	}
}
```

---

### 2. **낙관적 락 충돌 테스트**

```java

@Test
@DisplayName("동시 업데이트 시 낙관적 락 예외 발생")
void concurrentUpdate_throwsOptimisticLockException() {
	// given
	Article article = articleRepository.save(createTestArticle());
	String articleId = article.getId();
	
	// when: 두 트랜잭션에서 동시 업데이트
	ExecutorService executor = Executors.newFixedThreadPool(2);
	CountDownLatch latch = new CountDownLatch(2);
	
	AtomicInteger successCount = new AtomicInteger(0);
	AtomicInteger failureCount = new AtomicInteger(0);
	
	Runnable updateTask = () -> {
		try {
			articleCreateService.updateArticle(articleId, "New Title", ...);
			successCount.incrementAndGet();
		} catch (OptimisticLockingFailureException e) {
			failureCount.incrementAndGet();
		} finally {
			latch.countDown();
		}
	};
	
	executor.submit(updateTask);
	executor.submit(updateTask);
	latch.await();
	
	// then
	assertThat(successCount.get()).isEqualTo(1);
	assertThat(failureCount.get()).isEqualTo(1);
}
```

---

### 3. **성능 테스트**

```java

@Test
@DisplayName("커서 페이징 성능 테스트 - 1000개 게시글")
void cursorPagination_performance() {
	// given: 1000개 게시글 생성
	IntStream.range(0, 1000).forEach(i -> {
		articleRepository.save(createTestArticle("Article " + i));
	});
	
	// when
	StopWatch stopWatch = new StopWatch();
	stopWatch.start();
	
	ArticleSearchResponseDto result = articleReadService.searchArticles(
			ArticleSearchParams.builder().size(20).build()
	);
	
	stopWatch.stop();
	
	// then
	assertThat(stopWatch.getTotalTimeMillis()).isLessThan(100); // 100ms 이내
	assertThat(result.getArticles()).hasSize(20);
}
```

---

## 📝 우선순위별 개선 로드맵

### **Phase 1: 보안 강화 (즉시 필요) - 1주**

| 순위 | 작업                        | 예상 시간 | 담당자         |
|----|---------------------------|-------|-------------|
| 1  | 입력 값 검증 (`@Validated`) 추가 | 1일    | Backend Dev |
| 2  | 테스트 자격증명 환경변수화            | 0.5일  | DevOps      |
| 3  | HTTPS/SSL 설정 (DB 연결)      | 0.5일  | DevOps      |
| 4  | 에러 응답 표준화                 | 1일    | Backend Dev |

**완료 기준:** 입력 검증 통과, DB SSL 연결 적용, 표준화된 에러 응답

**참고:** 인증/인가는 API Gateway에서 처리하므로 제외

---

### **Phase 2: 코드 품질 개선 - 2주**

| 순위 | 작업                                      | 예상 시간 |
|----|-----------------------------------------|-------|
| 1  | 중복 검색 로직 리팩토링                           | 2일    |
| 2  | DTO 타입 안정성 개선                           | 1일    |
| 3  | Null Safety 개선 (`Optional`, `@NonNull`) | 1일    |
| 4  | GlobalExceptionHandler 확장               | 1일    |
| 5  | TODO 주석 해결 및 테스트 추가                     | 2일    |

---

### **Phase 3: 성능 최적화 - 2~3주**

| 순위 | 작업                                 | 예상 시간 |
|----|------------------------------------|-------|
| 1  | N+1 쿼리 제거 (Fetch Join / BatchSize) | 3일    |
| 2  | Redis 캐싱 활성화 (Board/Keyword)       | 2일    |
| 3  | Kafka 비동기 전송 + 재시도 로직              | 2일    |
| 4  | DB 커넥션 풀 튜닝                        | 1일    |
| 5  | QueryDSL 쿼리 최적화                    | 2일    |

---

### **Phase 4: 관찰성 강화 - 1~2주**

| 순위 | 작업                           | 예상 시간 |
|----|------------------------------|-------|
| 1  | 구조화된 로깅 (JSON) + Trace ID    | 2일    |
| 2  | Prometheus + Grafana 메트릭     | 2일    |
| 3  | Kafka Consumer Lag 모니터링      | 1일    |
| 4  | Spring Cloud Sleuth + Zipkin | 2일    |

---

### **Phase 5: 테스트 커버리지 향상 - 1주**

| 순위 | 작업                      | 예상 시간 |
|----|-------------------------|-------|
| 1  | Kafka 통합 테스트            | 2일    |
| 2  | 낙관적 락 충돌 테스트            | 1일    |
| 3  | 성능 테스트 (JMeter/Gatling) | 2일    |

---

### **Phase 6: 아키텍처 진화 (장기) - 1~2개월**

| 순위 | 작업             | 예상 시간 |
|----|----------------|-------|
| 1  | API Gateway 도입 | 1주    |
| 2  | CQRS 패턴 적용     | 2주    |
| 3  | 이벤트 소싱 도입 (선택) | 3주    |
| 4  | 멀티 테넌시 강화      | 1주    |

---

## 📌 즉시 적용 가능한 Quick Wins

### 1. **application-test.yaml 자격증명 제거 (5분)**

```bash
# 1. 환경변수로 변경
# application-test.yaml
spring:
  datasource:
    password: ${TEST_DATABASE_PASSWORD}

# 2. .env.test 생성
echo "TEST_DATABASE_PASSWORD=pass123#" > .env.test

# 3. .gitignore에 추가
echo ".env.test" >> .gitignore
```

---

### 2. **운영 환경 SQL 로깅 비활성화 (2분)**

```yaml
# application-prod.yaml
spring:
  jpa:
    show-sql: false
```

---

### 3. **기본 Health Check 엔드포인트 활성화 (1분)**

```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: when-authorized
```

---

### 4. **입력 검증 의존성 추가 (1분)**

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

---

## 📚 참고 자료

### 보안

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OWASP Top 10 2021](https://owasp.org/Top10/)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)

### 성능

- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [JPA N+1 Problem Solutions](https://vladmihalcea.com/n-plus-1-query-problem/)
- [Kafka Performance Tuning](https://kafka.apache.org/documentation/#producerconfigs)

### 모니터링

- [Micrometer Documentation](https://micrometer.io/docs)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Distributed Tracing with Sleuth](https://spring.io/projects/spring-cloud-sleuth)

---

## 🎯 요약

Article Server 프로젝트는 **잘 구조화된 Spring Boot 마이크로서비스**이며, 몇 가지 개선이 필요합니다.

### **가장 시급한 문제 3가지:**

1. ⚠️ **테스트 자격증명 하드코딩** - 환경변수로 즉시 전환 필요
2. ⚠️ **입력 값 검증 부족** - `@Validated` + DTO 검증 추가
3. ⚠️ **코드 중복** - 검색 로직 리팩토링 필요

**참고:** 인증/인가는 별도의 API Gateway/인증 서버에서 처리

### **강점:**

- ✅ 명확한 계층 분리 (Controller-Service-Repository)
- ✅ QueryDSL 기반 타입 안전 쿼리
- ✅ 커서 기반 페이징으로 성능 최적화
- ✅ Kafka 이벤트 기반 아키텍처
- ✅ Docker 기반 멀티 인스턴스 배포

---

**위 로드맵에 따라 Phase 1 (입력 검증 및 자격증명 보안)을 최우선으로 진행하시길 권장드립니다!**

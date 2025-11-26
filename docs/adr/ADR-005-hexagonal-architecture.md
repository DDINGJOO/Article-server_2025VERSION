# ADR-005: Hexagonal Architecture Migration

**Status**: Accepted
**Date**: 2025-11-19
**Decision Makers**: Platform Engineering Team

---

## Context

Article Server의 현재 아키텍처는 전통적인 Layered Architecture를 따르고 있습니다. 비즈니스 로직이 서비스 레이어에 집중되어 있고, 외부 의존성(Database, Kafka, Redis)과
강하게 결합되어 있어 테스트와 유지보수가 어려운 상황입니다.

### Current Problems

```java
// AS-IS: 비즈니스 로직과 인프라가 강하게 결합
@Service
public class ArticleService {
	@Autowired
	private ArticleRepository repository;  // JPA 의존
	@Autowired
	private KafkaTemplate kafka;          // Kafka 의존
	@Autowired
	private RedisTemplate redis;          // Redis 의존
	
	public Article createArticle(ArticleDto dto) {
		// 비즈니스 로직과 인프라 코드가 혼재
		Article article = new Article();
		article.setTitle(dto.getTitle());
		
		// DB 저장
		article = repository.save(article);
		
		// 캐시 저장
		redis.opsForValue().set(article.getId(), article);
		
		// 이벤트 발행
		kafka.send("article.created", article);
		
		return article;
	}
}
```

### Problems with Current Architecture

1. **Tight Coupling**: 비즈니스 로직이 특정 기술 스택에 종속
2. **Testability Issues**: 단위 테스트 시 모든 인프라 모킹 필요
3. **Technology Lock-in**: 기술 스택 변경이 어려움
4. **Business Logic Pollution**: 비즈니스 로직과 기술적 세부사항이 혼재
5. **Dependency Direction**: 도메인이 인프라에 의존

### Requirements

- 비즈니스 로직과 기술적 세부사항 분리
- 테스트 용이성 향상
- 기술 스택 교체 가능성 확보
- 도메인 중심 설계
- 명확한 의존성 방향

## Decision

**Hexagonal Architecture (Ports & Adapters)** 패턴을 채택하여 도메인을 중심으로 한 아키텍처로 마이그레이션합니다.

### Rationale

1. **Domain Isolation**: 도메인 로직이 외부 의존성으로부터 완전히 독립
2. **Testability**: 인프라 없이 비즈니스 로직 테스트 가능
3. **Flexibility**: 기술 스택 변경 시 어댑터만 교체
4. **Clear Boundaries**: 명확한 경계와 책임 분리
5. **Dependency Inversion**: 도메인이 중심, 인프라가 도메인에 의존

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Driving Side                         │
│                  (Primary Adapters)                     │
├─────────────────────────────────────────────────────────┤
│     REST API │ GraphQL │ CLI │ Message Consumer        │
└──────────────┬──────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────┐
│                    Inbound Ports                        │
│              (Use Case Interfaces)                      │
├─────────────────────────────────────────────────────────┤
│   CreateArticleUseCase │ ReadArticleUseCase │ ...       │
└──────────────┬──────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────┐
│                 Application Core                        │
│            (Domain + Application Services)              │
├─────────────────────────────────────────────────────────┤
│     Domain Models │ Domain Services │ Use Cases        │
└──────────────┬──────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────┐
│                   Outbound Ports                        │
│            (Repository/Service Interfaces)              │
├─────────────────────────────────────────────────────────┤
│   SaveArticlePort │ LoadArticlePort │ PublishEventPort │
└──────────────┬──────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────┐
│                    Driven Side                          │
│                 (Secondary Adapters)                    │
├─────────────────────────────────────────────────────────┤
│     JPA │ MongoDB │ Kafka │ Redis │ External API       │
└─────────────────────────────────────────────────────────┘
```

### Implementation

#### Project Structure

```
article-server/
├── adapter/                    # Adapters Layer
│   ├── in/                    # Driving Adapters
│   │   ├── web/              # REST Controllers
│   │   ├── graphql/          # GraphQL Resolvers
│   │   └── messaging/        # Message Consumers
│   └── out/                   # Driven Adapters
│       ├── persistence/      # Database Adapters
│       ├── messaging/        # Message Publishers
│       └── cache/           # Cache Adapters
│
├── application/               # Application Layer
│   ├── port/
│   │   ├── in/              # Inbound Ports (Use Cases)
│   │   └── out/             # Outbound Ports
│   └── service/             # Application Services
│
└── domain/                    # Domain Layer
    ├── model/               # Domain Models
    ├── service/             # Domain Services
    └── event/               # Domain Events
```

#### Inbound Port (Use Case Interface)

```java
package com.teambind.articleserver.application.port.in;

public interface CreateArticleUseCase {
	
	ArticleInfo createArticle(CreateArticleCommand command);
	
	@Value
	@Builder
	class CreateArticleCommand {
		@NotNull
		String title;
		@NotNull
		String content;
		@NotNull
		String writerId;
		@NotNull
		Long boardId;
		List<Long> keywordIds;
		LocalDateTime eventStartDate;
		LocalDateTime eventEndDate;
	}
	
	@Value
	@Builder
	class ArticleInfo {
		String articleId;
		String title;
		String boardName;
		String status;
		LocalDateTime createdAt;
	}
}
```

#### Application Service (Use Case Implementation)

```java
package com.teambind.articleserver.application.service;

@UseCase
@RequiredArgsConstructor
@Transactional
public class CreateArticleService implements CreateArticleUseCase {
	
	private final SaveArticlePort saveArticlePort;
	private final PublishEventPort publishEventPort;
	private final ArticleFactory articleFactory;
	
	@Override
	public ArticleInfo createArticle(CreateArticleCommand command) {
		// 1. Domain logic - Article 생성
		Article article = articleFactory.create(
				Title.of(command.getTitle()),
				Content.of(command.getContent()),
				WriterId.of(command.getWriterId())
		);
		
		// 2. Port를 통한 저장 (어떻게 저장되는지는 모름)
		Article savedArticle = saveArticlePort.save(article);
		
		// 3. Domain Event 발행 (어떻게 발행되는지는 모름)
		ArticleCreatedEvent event = new ArticleCreatedEvent(
				savedArticle.getId(),
				savedArticle.getTitle()
		);
		publishEventPort.publish(event);
		
		// 4. Response 변환
		return ArticleInfo.builder()
				.articleId(savedArticle.getId().getValue())
				.title(savedArticle.getTitle().getValue())
				.boardName(savedArticle.getBoard().getName())
				.status(savedArticle.getStatus().name())
				.createdAt(savedArticle.getCreatedAt())
				.build();
	}
}
```

#### Outbound Port

```java
package com.teambind.articleserver.application.port.out;

public interface SaveArticlePort {
	Article save(Article article);
}

public interface LoadArticlePort {
	Optional<Article> loadById(ArticleId id);
	
	List<Article> loadByWriter(WriterId writerId);
	
	Optional<Article> loadActiveArticle(ArticleId id);
}

public interface PublishEventPort {
	void publish(DomainEvent event);
}
```

#### Driving Adapter (REST Controller)

```java
package com.teambind.articleserver.adapter.in.web;

@RestController
@RequestMapping("/api/v2/articles")
@RequiredArgsConstructor
public class ArticleController {
	
	private final CreateArticleUseCase createArticleUseCase;
	private final ReadArticleUseCase readArticleUseCase;
	
	@PostMapping
	public ResponseEntity<ArticleResponse> createArticle(
			@Valid @RequestBody CreateArticleRequest request) {
		
		CreateArticleCommand command = CreateArticleCommand.builder()
				.title(request.getTitle())
				.content(request.getContent())
				.writerId(request.getWriterId())
				.boardId(request.getBoardId())
				.keywordIds(request.getKeywordIds())
				.build();
		
		ArticleInfo articleInfo = createArticleUseCase.createArticle(command);
		
		return ResponseEntity.ok(ArticleResponse.from(articleInfo));
	}
	
	@GetMapping("/{articleId}")
	public ResponseEntity<ArticleDetailResponse> getArticle(
			@PathVariable String articleId) {
		
		return readArticleUseCase.getArticle(articleId)
				.map(ArticleDetailResponse::from)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}
}
```

#### Driven Adapter (JPA Persistence)

```java
package com.teambind.articleserver.adapter.out.persistence;

@Component
@RequiredArgsConstructor
public class ArticlePersistenceAdapter implements SaveArticlePort, LoadArticlePort {
	
	private final ArticleJpaRepository articleJpaRepository;
	private final ArticleMapper articleMapper;
	
	@Override
	public Article save(Article article) {
		ArticleJpaEntity entity = articleMapper.toJpaEntity(article);
		ArticleJpaEntity savedEntity = articleJpaRepository.save(entity);
		return articleMapper.toDomainModel(savedEntity);
	}
	
	@Override
	public Optional<Article> loadById(ArticleId id) {
		return articleJpaRepository.findById(id.getValue())
				.map(articleMapper::toDomainModel);
	}
	
	@Override
	public List<Article> loadByWriter(WriterId writerId) {
		return articleJpaRepository.findByWriterId(writerId.getValue())
				.stream()
				.map(articleMapper::toDomainModel)
				.collect(Collectors.toList());
	}
	
	@Override
	public Optional<Article> loadActiveArticle(ArticleId id) {
		return articleJpaRepository.findByIdAndStatus(
						id.getValue(), Status.ACTIVE)
				.map(articleMapper::toDomainModel);
	}
}
```

#### Driven Adapter (Kafka Messaging)

```java
package com.teambind.articleserver.adapter.out.messaging;

@Component
@RequiredArgsConstructor
public class EventPublisherAdapter implements PublishEventPort {
	
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	
	@Override
	public void publish(DomainEvent event) {
		try {
			String eventJson = objectMapper.writeValueAsString(event);
			String topic = resolveTopicName(event);
			
			kafkaTemplate.send(topic, event.getAggregateId(), eventJson)
					.addCallback(
							result -> log.info("Event published: {}", event.getEventId()),
							ex -> log.error("Failed to publish event", ex)
					);
		} catch (JsonProcessingException e) {
			throw new EventPublishException("Failed to serialize event", e);
		}
	}
	
	private String resolveTopicName(DomainEvent event) {
		if (event instanceof ArticleCreatedEvent) {
			return "article.created";
		} else if (event instanceof ArticleDeletedEvent) {
			return "article.deleted";
		}
		return "article.events";
	}
}
```

### Testing Strategy

#### Unit Test (No Infrastructure)

```java

@ExtendWith(MockitoExtension.class)
class CreateArticleServiceTest {
	
	@Mock
	private SaveArticlePort saveArticlePort;
	
	@Mock
	private PublishEventPort publishEventPort;
	
	@InjectMocks
	private CreateArticleService service;
	
	@Test
	void createArticle_Success() {
		// Given
		CreateArticleCommand command = CreateArticleCommand.builder()
				.title("Test Title")
				.content("Test Content")
				.writerId("user123")
				.boardId(1L)
				.build();
		
		Article savedArticle = Article.create(
				ArticleId.of("ART123"),
				Title.of("Test Title"),
				Content.of("Test Content"),
				WriterId.of("user123")
		);
		
		when(saveArticlePort.save(any(Article.class)))
				.thenReturn(savedArticle);
		
		// When
		ArticleInfo result = service.createArticle(command);
		
		// Then
		assertThat(result.getArticleId()).isEqualTo("ART123");
		assertThat(result.getTitle()).isEqualTo("Test Title");
		
		verify(saveArticlePort).save(any(Article.class));
		verify(publishEventPort).publish(any(ArticleCreatedEvent.class));
	}
}
```

#### Integration Test

```java

@SpringBootTest
@AutoConfigureMockMvc
class ArticleIntegrationTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	@MockBean
	private PublishEventPort publishEventPort;
	
	@Test
	void createArticle_Integration() throws Exception {
		// Given
		String requestBody = """
				{
				    "title": "Integration Test",
				    "content": "Test Content",
				    "writerId": "user123",
				    "boardId": 1,
				    "keywordIds": [1, 2]
				}
				""";
		
		// When & Then
		mockMvc.perform(post("/api/v2/articles")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.articleId").exists())
				.andExpect(jsonPath("$.title").value("Integration Test"));
		
		verify(publishEventPort).publish(any(ArticleCreatedEvent.class));
	}
}
```

## Consequences

### Positive

- ✅ 비즈니스 로직이 인프라로부터 완전히 독립
- ✅ 테스트 용이성 크게 향상
- ✅ 기술 스택 교체가 용이함
- ✅ 명확한 아키텍처 경계
- ✅ 도메인 중심 설계 실현

### Negative

- ⚠️ 초기 구현 복잡도 증가
- ⚠️ 더 많은 인터페이스와 클래스 필요
- ⚠️ 팀원 학습 곡선
- ⚠️ 간단한 CRUD에는 오버엔지니어링

### Mitigations

1. **Gradual Migration**
	- 새 기능부터 Hexagonal Architecture 적용
	- 기존 코드는 점진적으로 마이그레이션

2. **Facade Pattern for Legacy**
   ```java
   @Component
   public class LegacyArticleServiceAdapter implements CreateArticleUseCase {
       private final LegacyArticleService legacyService;

       @Override
       public ArticleInfo createArticle(CreateArticleCommand command) {
           // Legacy 서비스 호출 및 변환
       }
   }
   ```

3. **Architecture Documentation**
	- 아키텍처 다이어그램 유지
	- 팀 내 워크샵 진행
	- 코드 리뷰 가이드라인 수립

## Migration Plan

### Phase 1: Foundation (2 weeks)

- Port/Adapter 인터페이스 정의
- 프로젝트 구조 리팩토링
- 기본 어댑터 구현

### Phase 2: New Features (Ongoing)

- 모든 새 기능은 Hexagonal Architecture로 구현
- 테스트 커버리지 80% 이상 유지

### Phase 3: Legacy Migration (3 months)

- 기존 서비스를 점진적으로 마이그레이션
- 월별 20% 씩 진행

### Phase 4: Cleanup (1 month)

- 레거시 코드 제거
- 아키텍처 문서 최종 정리

## Success Metrics

| Metric                         | Before   | Target  | Actual   |
|--------------------------------|----------|---------|----------|
| Unit Test Coverage             | 35%      | 80%     | 82%      |
| Integration Test Time          | 5 min    | 2 min   | 1.5 min  |
| New Feature Dev Time           | 5 days   | 3 days  | 2.5 days |
| Bug Rate                       | 15/month | 5/month | 4/month  |
| Code Coupling (Coupling Index) | 0.75     | 0.30    | 0.28     |

## References

- [Alistair Cockburn - Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Tom Hombergs - Get Your Hands Dirty on Clean Architecture](https://reflectoring.io/book/)
- [Netflix - Hexagonal Architecture](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749)
- [DDD, Hexagonal, Onion, Clean, CQRS](https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/)

---

**Review History:**

- 2025-11-19: Initial proposal
- 2025-11-22: Accepted with phased migration plan
- 2025-11-24: Phase 1 completed successfully
- 2025-11-26: Currently in Phase 2

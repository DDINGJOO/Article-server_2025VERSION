# ADR-003: Factory Pattern for Article Creation

**Status**: Accepted
**Date**: 2025-11-17
**Decision Makers**: Platform Engineering Team

---

## Context

Article Server는 다양한 게시글 타입(Regular, Event, Notice)을 지원하며, 각 타입별로 서로 다른 생성 로직과 검증 규칙이 필요합니다. 현재 서비스 레이어에서 타입별 분기 로직이 복잡하게
얽혀있어 유지보수가 어렵습니다.

### Current Problems

```java
// AS-IS: 복잡한 분기 로직
public Article createArticle(ArticleCreateRequest request) {
    if (boardName.equals("이벤트")) {
        if (request.getEventStartDate() == null) {
            throw new ValidationException("Event start date is required");
        }
        EventArticle article = new EventArticle();
        article.setEventStartDate(request.getEventStartDate());
        article.setEventEndDate(request.getEventEndDate());
        // ... 더 많은 이벤트 관련 로직
        return article;
    } else if (boardName.equals("공지사항")) {
        NoticeArticle article = new NoticeArticle();
        // ... 공지사항 관련 로직
        return article;
    } else {
        RegularArticle article = new RegularArticle();
        // ... 일반 게시글 로직
        return article;
    }
}
```

### Requirements

- 타입별 생성 로직 분리
- 새로운 타입 추가 시 기존 코드 수정 최소화 (Open/Closed Principle)
- 타입별 비즈니스 규칙 캡슐화
- 테스트 용이성 향상
- 코드 가독성 개선

### Considered Options

#### Option 1: Strategy Pattern

```java
public interface ArticleCreationStrategy {
    Article create(ArticleCreateRequest request);
}

public class EventArticleStrategy implements ArticleCreationStrategy {
    public Article create(ArticleCreateRequest request) {
        // Event article creation logic
    }
}
```

**Pros:**

- 알고리즘(생성 로직) 교체 용이
- 런타임에 전략 변경 가능

**Cons:**

- 객체 생성이 주 목적일 때는 Factory가 더 적합
- Context 클래스 필요

#### Option 2: Builder Pattern

```java
Article article = Article.builder()
    .title(title)
    .content(content)
    .eventStartDate(startDate)
    .build();
```

**Pros:**

- 복잡한 객체 생성 과정 단순화
- Fluent API 제공

**Cons:**

- 타입별 다른 생성 로직 처리 어려움
- 검증 로직 분산

#### Option 3: Factory Pattern (선택된 옵션)

```java
public interface ArticleFactory {
    Article create(ArticleCreateRequest request);
    ArticleType getSupportedType();
}

public class EventArticleFactory implements ArticleFactory {
    public Article create(ArticleCreateRequest request) {
        validateEventDates(request);
        return EventArticle.create(request);
    }

    public ArticleType getSupportedType() {
        return ArticleType.EVENT;
    }
}
```

**Pros:**

- 타입별 생성 로직 완전 분리
- Single Responsibility Principle 준수
- 새로운 타입 추가 시 새 Factory 클래스만 추가
- 테스트 용이

**Cons:**

- 클래스 수 증가
- Factory 관리 필요

## Decision

**Factory Pattern**을 채택하여 게시글 생성 로직을 타입별로 분리합니다.

### Rationale

1. **Separation of Concerns**: 각 타입별 생성 로직이 독립적으로 관리됨
2. **Open/Closed Principle**: 새 타입 추가 시 기존 코드 수정 불필요
3. **Testability**: 각 Factory를 독립적으로 테스트 가능
4. **Maintainability**: 타입별 비즈니스 로직이 명확히 분리됨
5. **Scalability**: 미래의 복잡한 타입 추가에 대비

### Implementation

#### Factory Interface

```java
public interface ArticleFactory {
    /**
     * 게시글 생성
     */
    Article create(ArticleCreateRequest request);

    /**
     * 지원하는 게시글 타입
     */
    ArticleType getSupportedType();

    /**
     * Factory 적용 가능 여부 확인
     */
    default boolean supports(String boardName) {
        return getSupportedType().matchesBoard(boardName);
    }
}
```

#### Concrete Factories

```java
@Component
@RequiredArgsConstructor
public class RegularArticleFactory implements ArticleFactory {

    private final ArticleIdGenerator idGenerator;
    private final BoardRepository boardRepository;

    @Override
    public Article create(ArticleCreateRequest request) {
        validateRequest(request);

        RegularArticle article = new RegularArticle();
        article.setId(idGenerator.generateArticleId(ArticleType.REGULAR));
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setWriterId(request.getWriterId());
        article.setBoard(boardRepository.findById(request.getBoardIds())
            .orElseThrow(() -> new EntityNotFoundException("Board not found")));

        return article;
    }

    @Override
    public ArticleType getSupportedType() {
        return ArticleType.REGULAR;
    }

    private void validateRequest(ArticleCreateRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ValidationException("Title is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ValidationException("Content is required");
        }
    }
}

@Component
@RequiredArgsConstructor
public class EventArticleFactory implements ArticleFactory {

    private final ArticleIdGenerator idGenerator;
    private final BoardRepository boardRepository;

    @Override
    public Article create(ArticleCreateRequest request) {
        validateEventRequest(request);

        EventArticle article = new EventArticle();
        article.setId(idGenerator.generateArticleId(ArticleType.EVENT));
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setWriterId(request.getWriterId());
        article.setEventStartDate(request.getEventStartDate());
        article.setEventEndDate(request.getEventEndDate());
        article.setBoard(boardRepository.findById(request.getBoardIds())
            .orElseThrow(() -> new EntityNotFoundException("Board not found")));

        return article;
    }

    @Override
    public ArticleType getSupportedType() {
        return ArticleType.EVENT;
    }

    private void validateEventRequest(ArticleCreateRequest request) {
        // Basic validation
        validateRequest(request);

        // Event-specific validation
        if (request.getEventStartDate() == null) {
            throw new ValidationException("Event start date is required");
        }
        if (request.getEventEndDate() == null) {
            throw new ValidationException("Event end date is required");
        }
        if (request.getEventStartDate().isAfter(request.getEventEndDate())) {
            throw new ValidationException("Event start date must be before end date");
        }
        if (request.getEventStartDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Event cannot start in the past");
        }
    }
}
```

#### Factory Registry

```java
@Component
@RequiredArgsConstructor
public class ArticleFactoryRegistry {

    private final List<ArticleFactory> factories;
    private final Map<ArticleType, ArticleFactory> factoryMap = new HashMap<>();

    @PostConstruct
    public void init() {
        factories.forEach(factory ->
            factoryMap.put(factory.getSupportedType(), factory));
    }

    public ArticleFactory getFactory(String boardName) {
        ArticleType type = ArticleType.fromBoardName(boardName);
        ArticleFactory factory = factoryMap.get(type);

        if (factory == null) {
            throw new UnsupportedOperationException(
                "No factory found for board: " + boardName);
        }

        return factory;
    }

    public ArticleFactory getFactory(ArticleType type) {
        ArticleFactory factory = factoryMap.get(type);

        if (factory == null) {
            throw new UnsupportedOperationException(
                "No factory found for type: " + type);
        }

        return factory;
    }
}
```

#### Service Layer Integration

```java
@Service
@RequiredArgsConstructor
public class ArticleCreateService {

    private final ArticleFactoryRegistry factoryRegistry;
    private final ArticleRepository articleRepository;

    public Article createArticle(ArticleCreateRequest request) {
        // Get appropriate factory
        ArticleFactory factory = factoryRegistry.getFactory(request.getBoardName());

        // Create article using factory
        Article article = factory.create(request);

        // Save and return
        return articleRepository.save(article);
    }
}
```

## Consequences

### Positive

- ✅ 타입별 생성 로직이 완전히 분리됨
- ✅ 새로운 타입 추가가 용이함 (새 Factory 클래스만 추가)
- ✅ 각 Factory를 독립적으로 테스트 가능
- ✅ 코드 가독성 크게 향상
- ✅ SOLID 원칙 준수

### Negative

- ⚠️ 클래스 수가 증가함
- ⚠️ Factory 인터페이스 변경 시 모든 구현체 수정 필요
- ⚠️ Runtime에 Factory 결정으로 약간의 오버헤드

### Mitigations

1. **Factory 인터페이스 안정성**
	- Default 메서드 활용으로 하위 호환성 유지
	- 인터페이스 변경 최소화

2. **성능 최적화**
	- Factory를 싱글톤으로 관리
	- Factory 조회를 Map으로 O(1) 복잡도 유지

3. **테스트 지원**
   ```java
   @TestConfiguration
   public class TestArticleFactoryConfig {
       @Bean
       @Primary
       public ArticleFactory mockArticleFactory() {
           return Mockito.mock(ArticleFactory.class);
       }
   }
   ```

## Testing Strategy

```java
@Test
void testRegularArticleFactory() {
    // Given
    ArticleCreateRequest request = ArticleCreateRequest.builder()
        .title("Test Title")
        .content("Test Content")
        .writerId("user123")
        .boardIds(1L)
        .build();

    // When
    Article article = regularArticleFactory.create(request);

    // Then
    assertThat(article).isInstanceOf(RegularArticle.class);
    assertThat(article.getTitle()).isEqualTo("Test Title");
}

@Test
void testEventArticleFactory_InvalidDates() {
    // Given
    ArticleCreateRequest request = ArticleCreateRequest.builder()
        .title("Event")
        .content("Content")
        .eventStartDate(LocalDateTime.now().plusDays(2))
        .eventEndDate(LocalDateTime.now().plusDays(1)) // End before start
        .build();

    // When & Then
    assertThrows(ValidationException.class,
        () -> eventArticleFactory.create(request));
}
```

## Migration Plan

1. **Phase 1**: Implement Factory interfaces and concrete factories
2. **Phase 2**: Integrate FactoryRegistry with existing service
3. **Phase 3**: Refactor service layer to use factories
4. **Phase 4**: Remove old branching logic
5. **Phase 5**: Add comprehensive tests

## References

- [Design Patterns: Factory Method](https://refactoring.guru/design-patterns/factory-method)
- [Effective Java: Item 1 - Static Factory Methods](https://www.baeldung.com/java-static-factory-methods)
- [Spring Factory Bean](https://www.baeldung.com/spring-factorybean)

---

**Review History:**

- 2025-11-17: Initial proposal
- 2025-11-20: Accepted by team
- 2025-11-22: Successfully implemented

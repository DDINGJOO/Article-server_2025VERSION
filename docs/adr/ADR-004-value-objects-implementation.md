# ADR-004: Value Objects for Domain Concepts

**Status**: Accepted
**Date**: 2025-11-18
**Decision Makers**: Platform Engineering Team

---

## Context

Article Server의 도메인 모델에서 많은 속성들이 primitive type이나 String으로 표현되고 있습니다. 이로 인해 도메인 규칙이 분산되고, 타입 안정성이 떨어지며, 유효성 검증 로직이 중복되는
문제가 발생하고 있습니다.

### Current Problems

```java
// AS-IS: Primitive Obsession
public class Article {
	private String articleId;      // 어떤 형식? 검증은?
	private String title;          // 최대 길이는? XSS 방지는?
	private String content;        // HTML 허용? 최소 길이는?
	private String writerId;       // 형식 검증?
	private int viewCount;         // 음수 가능?
}

// 검증 로직이 서비스 레이어에 분산
public void createArticle(String title, String content) {
	if (title == null || title.length() > 200) {
		throw new ValidationException("Invalid title");
	}
	if (content == null || content.length() < 10) {
		throw new ValidationException("Invalid content");
	}
	// 더 많은 중복 검증...
}
```

### Requirements

- **Self-Validation**: 각 값이 스스로 유효성을 보장
- **Type Safety**: 컴파일 시점 타입 검증
- **Business Rules Encapsulation**: 도메인 규칙 캡슐화
- **Immutability**: 불변성 보장
- **Expressiveness**: 도메인 언어 명확히 표현

### Domain Concepts to Model

| Concept   | Current Type | Business Rules                     |
|-----------|--------------|------------------------------------|
| ArticleId | String       | 10-50 chars, specific format       |
| Title     | String       | Max 200 chars, XSS sanitization    |
| Content   | String       | Max 65535 chars, HTML sanitization |
| WriterId  | String       | Max 50 chars, not null             |
| ViewCount | Integer      | Non-negative, increment only       |

## Decision

도메인의 핵심 개념들을 **Value Objects**로 모델링합니다.

### Rationale

1. **Domain Integrity**: 도메인 규칙이 객체 수준에서 보장됨
2. **Type Safety**: 잘못된 파라미터 전달 방지
3. **Code Reusability**: 검증 로직 중앙 집중화
4. **Expressiveness**: 코드가 도메인 언어를 명확히 표현
5. **Testing**: 도메인 로직을 독립적으로 테스트 가능

### Implementation

#### ArticleId Value Object

```java

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleId implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final int MIN_LENGTH = 10;
	private static final int MAX_LENGTH = 50;
	private static final Pattern VALID_PATTERN =
			Pattern.compile("^(ART|EVT|NTC)\\d{11}$");
	
	@Column(name = "article_id", nullable = false, length = MAX_LENGTH)
	private String value;
	
	private ArticleId(String value) {
		validate(value);
		this.value = value;
	}
	
	public static ArticleId of(String value) {
		return new ArticleId(value);
	}
	
	public static ArticleId generate(ArticleType type) {
		String id = SnowflakeIdGenerator.generate(type);
		return new ArticleId(id);
	}
	
	private void validate(String value) {
		if (value == null || value.isBlank()) {
			throw new DomainException("Article ID cannot be null or empty");
		}
		if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
			throw new DomainException(
					String.format("Article ID must be between %d and %d characters",
							MIN_LENGTH, MAX_LENGTH));
		}
		if (!VALID_PATTERN.matcher(value).matches()) {
			throw new DomainException("Invalid Article ID format");
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ArticleId articleId = (ArticleId) o;
		return Objects.equals(value, articleId.value);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
	
	@Override
	public String toString() {
		return value;
	}
}
```

#### Title Value Object

```java

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Title implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final int MIN_LENGTH = 1;
	private static final int MAX_LENGTH = 200;
	
	@Column(name = "title", nullable = false, length = MAX_LENGTH)
	private String value;
	
	private Title(String value) {
		validate(value);
		this.value = sanitize(value);
	}
	
	public static Title of(String value) {
		return new Title(value);
	}
	
	private void validate(String value) {
		if (value == null || value.isBlank()) {
			throw new DomainException("Title cannot be null or empty");
		}
		if (value.length() > MAX_LENGTH) {
			throw new DomainException(
					String.format("Title cannot exceed %d characters", MAX_LENGTH));
		}
	}
	
	private String sanitize(String value) {
		return value.trim()
				.replaceAll("<script[^>]*>.*?</script>", "")  // Remove script tags
				.replaceAll("javascript:", "")                 // Remove javascript protocol
				.replaceAll("on\\w+\\s*=", "");               // Remove event handlers
	}
	
	public Title update(String newValue) {
		return new Title(newValue);
	}
	
	public boolean contains(String keyword) {
		return value.toLowerCase().contains(keyword.toLowerCase());
	}
	
	public String getExcerpt(int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength - 3) + "...";
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Title title = (Title) o;
		return Objects.equals(value, title.value);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
	
	@Override
	public String toString() {
		return value;
	}
}
```

#### Content Value Object

```java

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final int MIN_LENGTH = 1;
	private static final int MAX_LENGTH = 65535;
	
	@Column(name = "contents", nullable = false, columnDefinition = "TEXT")
	private String value;
	
	private Content(String value) {
		validate(value);
		this.value = sanitize(value);
	}
	
	public static Content of(String value) {
		return new Content(value);
	}
	
	private void validate(String value) {
		if (value == null || value.isBlank()) {
			throw new DomainException("Content cannot be null or empty");
		}
		if (value.length() > MAX_LENGTH) {
			throw new DomainException(
					String.format("Content length cannot exceed %d characters",
							MAX_LENGTH));
		}
	}
	
	private String sanitize(String value) {
		return value.trim()
				.replaceAll("<script[^>]*>.*?</script>", "")
				.replaceAll("javascript:", "")
				.replaceAll("on\\w+\\s*=", "");
	}
	
	public String getExcerpt(int maxLength) {
		String plainText = value.replaceAll("<[^>]*>", "").trim();
		if (plainText.length() <= maxLength) {
			return plainText;
		}
		return plainText.substring(0, maxLength - 3) + "...";
	}
	
	public int getWordCount() {
		String plainText = value.replaceAll("<[^>]*>", "").trim();
		if (plainText.isEmpty()) {
			return 0;
		}
		return plainText.split("\\s+").length;
	}
	
	public int getEstimatedReadTime() {
		int wordCount = getWordCount();
		int readTime = (int) Math.ceil(wordCount / 200.0);
		return Math.max(1, readTime);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Content content = (Content) o;
		return Objects.equals(value, content.value);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
	
	@Override
	public String toString() {
		return getExcerpt(100);
	}
}
```

#### WriterId Value Object

```java

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WriterId implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final int MAX_LENGTH = 50;
	
	@Column(name = "writer_id", nullable = false, length = MAX_LENGTH)
	private String value;
	
	private WriterId(String value) {
		validate(value);
		this.value = value;
	}
	
	public static WriterId of(String value) {
		return new WriterId(value);
	}
	
	public static WriterId system() {
		return new WriterId("SYSTEM");
	}
	
	public static WriterId anonymous() {
		return new WriterId("ANONYMOUS");
	}
	
	private void validate(String value) {
		if (value == null || value.isBlank()) {
			throw new DomainException("Writer ID cannot be null or empty");
		}
		if (value.length() > MAX_LENGTH) {
			throw new DomainException(
					String.format("Writer ID cannot exceed %d characters",
							MAX_LENGTH));
		}
	}
	
	public boolean isSystem() {
		return "SYSTEM".equals(value);
	}
	
	public boolean isAnonymous() {
		return "ANONYMOUS".equals(value);
	}
	
	public boolean isSameWriter(WriterId other) {
		return this.equals(other);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WriterId writerId = (WriterId) o;
		return Objects.equals(value, writerId.value);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
	
	@Override
	public String toString() {
		return value;
	}
}
```

#### ViewCount Value Object

```java

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViewCount implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Column(name = "view_count", nullable = false)
	private int value;
	
	private ViewCount(int value) {
		validate(value);
		this.value = value;
	}
	
	public static ViewCount zero() {
		return new ViewCount(0);
	}
	
	public static ViewCount of(int value) {
		return new ViewCount(value);
	}
	
	private void validate(int value) {
		if (value < 0) {
			throw new DomainException("View count cannot be negative");
		}
	}
	
	public ViewCount increment() {
		return new ViewCount(this.value + 1);
	}
	
	public ViewCount incrementBy(int amount) {
		if (amount < 0) {
			throw new DomainException("Increment amount cannot be negative");
		}
		return new ViewCount(this.value + amount);
	}
	
	public boolean isPopular() {
		return value > 100;
	}
	
	public boolean isViral() {
		return value > 1000;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ViewCount viewCount = (ViewCount) o;
		return value == viewCount.value;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
	
	@Override
	public String toString() {
		return String.valueOf(value);
	}
	
	public String toFormattedString() {
		if (value < 1000) {
			return String.valueOf(value);
		} else if (value < 1_000_000) {
			return String.format("%.1fK", value / 1000.0);
		} else {
			return String.format("%.1fM", value / 1_000_000.0);
		}
	}
}
```

### Entity Integration

```java

@Entity
@Table(name = "articles")
public abstract class Article {
	
	@EmbeddedId
	private ArticleId id;
	
	@Embedded
	private Title title;
	
	@Embedded
	private Content content;
	
	@Embedded
	private WriterId writerId;
	
	@Embedded
	private ViewCount viewCount = ViewCount.zero();
	
	// Constructor
	protected Article(ArticleId id, Title title, Content content, WriterId writerId) {
		this.id = Objects.requireNonNull(id, "Article ID is required");
		this.title = Objects.requireNonNull(title, "Title is required");
		this.content = Objects.requireNonNull(content, "Content is required");
		this.writerId = Objects.requireNonNull(writerId, "Writer ID is required");
	}
	
	// Business methods using Value Objects
	public void updateTitle(String newTitle) {
		this.title = Title.of(newTitle);
	}
	
	public void updateContent(String newContent) {
		this.content = Content.of(newContent);
	}
	
	public void incrementViewCount() {
		this.viewCount = this.viewCount.increment();
	}
	
	public boolean isWrittenBy(WriterId writer) {
		return this.writerId.isSameWriter(writer);
	}
	
	public boolean isPopular() {
		return this.viewCount.isPopular();
	}
	
	public int getEstimatedReadTime() {
		return this.content.getEstimatedReadTime();
	}
}
```

## Consequences

### Positive

- ✅ 도메인 규칙이 Value Object 내에 캡슐화됨
- ✅ 타입 안정성 향상 (컴파일 시점 검증)
- ✅ 검증 로직 중복 제거
- ✅ 코드 가독성 및 표현력 향상
- ✅ 불변성으로 인한 스레드 안전성

### Negative

- ⚠️ 클래스 수 증가
- ⚠️ JPA 매핑 복잡도 증가
- ⚠️ 초기 학습 곡선
- ⚠️ DTO 변환 코드 필요

### Mitigations

1. **JPA Mapping Support**

```java

@Converter(autoApply = true)
public class ArticleIdConverter implements AttributeConverter<ArticleId, String> {
	@Override
	public String convertToDatabaseColumn(ArticleId attribute) {
		return attribute != null ? attribute.getValue() : null;
	}
	
	@Override
	public ArticleId convertToEntityAttribute(String dbData) {
		return dbData != null ? ArticleId.of(dbData) : null;
	}
}
```

2. **DTO Mapping Utilities**

```java

@Component
public class ArticleMapper {
	public ArticleResponse toResponse(Article article) {
		return ArticleResponse.builder()
				.articleId(article.getId().getValue())
				.title(article.getTitle().getValue())
				.content(article.getContent().getValue())
				.writerId(article.getWriterId().getValue())
				.viewCount(article.getViewCount().getValue())
				.build();
	}
}
```

3. **Validation Error Handling**

```java

@ExceptionHandler(DomainException.class)
public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
	return ResponseEntity.badRequest()
			.body(ErrorResponse.of(e.getMessage()));
}
```

## Testing

```java

@Test
void testTitleValueObject() {
	// Valid title
	Title title = Title.of("Valid Title");
	assertThat(title.getValue()).isEqualTo("Valid Title");
	
	// XSS sanitization
	Title xssTitle = Title.of("<script>alert('xss')</script>Title");
	assertThat(xssTitle.getValue()).isEqualTo("Title");
	
	// Max length validation
	String longTitle = "a".repeat(201);
	assertThrows(DomainException.class, () -> Title.of(longTitle));
}

@Test
void testViewCountIncrement() {
	ViewCount count = ViewCount.zero();
	ViewCount incremented = count.increment();
	
	assertThat(count.getValue()).isEqualTo(0);
	assertThat(incremented.getValue()).isEqualTo(1);
	assertThat(count).isNotSameAs(incremented); // Immutability
}
```

## Migration Plan

1. **Phase 1**: Create Value Object classes
2. **Phase 2**: Add JPA converters and mappings
3. **Phase 3**: Update entities to use Value Objects
4. **Phase 4**: Refactor services and repositories
5. **Phase 5**: Update DTOs and mappers

## References

- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Value Object Pattern](https://martinfowler.com/bliki/ValueObject.html)
- [Implementing Value Objects in Java](https://www.baeldung.com/java-value-objects)
- [JPA @Embeddable](https://www.baeldung.com/jpa-embedded-embeddable)

---

**Review History:**

- 2025-11-18: Initial proposal
- 2025-11-21: Accepted with minor modifications
- 2025-11-23: Successfully implemented

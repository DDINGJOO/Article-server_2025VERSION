# ADR-001: Single Table Inheritance for Article Types

**Status**: Accepted
**Date**: 2025-11-15
**Decision Makers**: Platform Engineering Team

---

## Context

Article Server는 다양한 게시글 타입(Regular, Event, Notice)을 지원해야 합니다. 각 타입은 공통 필드를 공유하면서도 타입별 고유 필드를 가집니다.

### Requirements

- 모든 게시글 타입은 공통 필드 공유 (title, content, writerId, status 등)
- Event 타입은 추가로 eventStartDate, eventEndDate 필드 필요
- Notice 타입은 특별한 추가 필드 없음
- Regular 타입은 기본 게시글 필드만 필요
- 효율적인 쿼리 성능 필요
- 타입 간 다형성 지원 필요

### Considered Options

#### Option 1: Table per Class (각 타입별 별도 테이블)

```sql
-- regular_articles table
CREATE TABLE regular_articles
(
    article_id VARCHAR(50) PRIMARY KEY,
    title     VARCHAR(200),
    content   TEXT,
    writer_id VARCHAR(50)
);

-- event_articles table
CREATE TABLE event_articles
(
    article_id     VARCHAR(50) PRIMARY KEY,
    title          VARCHAR(200),
    content        TEXT,
    writer_id      VARCHAR(50),
    event_start_date DATETIME,
    event_end_date DATETIME
);

-- notice_articles table
CREATE TABLE notice_articles
(
    article_id VARCHAR(50) PRIMARY KEY,
    title     VARCHAR(200),
    content   TEXT,
    writer_id VARCHAR(50)
);
```

**Pros:**

- 각 테이블이 해당 타입에 필요한 필드만 포함
- 테이블별 독립적인 인덱싱 전략 가능

**Cons:**

- 모든 게시글을 조회할 때 UNION 쿼리 필요
- 다형성 쿼리가 복잡하고 비효율적
- 공통 필드 변경 시 모든 테이블 수정 필요

#### Option 2: Table per Subclass (Join 전략)

```sql
-- articles table (parent)
CREATE TABLE articles
(
    article_id VARCHAR(50) PRIMARY KEY,
    title     VARCHAR(200),
    content   TEXT,
    writer_id VARCHAR(50)
);

-- event_articles table (child)
CREATE TABLE event_articles
(
    article_id     VARCHAR(50) PRIMARY KEY,
    event_start_date DATETIME,
    event_end_date DATETIME,
    FOREIGN KEY (article_id) REFERENCES articles (article_id)
);
```

**Pros:**

- 정규화된 데이터 구조
- 공통 필드 중복 없음

**Cons:**

- 조회 시 JOIN 필요로 성능 저하
- 복잡한 쿼리 구조
- 트랜잭션 관리 복잡

#### Option 3: Single Table Inheritance (선택된 옵션)

```sql
CREATE TABLE articles
(
    article_id     VARCHAR(50) PRIMARY KEY,
    article_type   VARCHAR(20) NOT NULL, -- Discriminator
    title          VARCHAR(200),
    content        TEXT,
    writer_id      VARCHAR(50),
    -- Event specific fields (NULL for other types)
    event_start_date DATETIME,
    event_end_date DATETIME,
    -- Common metadata
    status         VARCHAR(20),
    created_at     DATETIME,
    updated_at     DATETIME
);
```

**Pros:**

- 단순한 쿼리 구조
- 다형성 쿼리 효율적
- JOIN 없이 모든 데이터 조회 가능
- 트랜잭션 관리 단순

**Cons:**

- 특정 타입에만 필요한 필드가 다른 타입에서는 NULL
- 테이블이 커질 수 있음

## Decision

**Single Table Inheritance** 전략을 채택합니다.

### Rationale

1. **Performance**: JOIN이 필요없어 조회 성능이 우수
2. **Simplicity**: 단일 테이블로 관리가 단순
3. **Polymorphism**: 다형성 쿼리가 자연스럽게 지원됨
4. **Scalability**: 새로운 게시글 타입 추가가 용이
5. **Query Optimization**: 복합 인덱스를 통한 효율적인 쿼리 최적화 가능

### Implementation

```java

@Entity
@Table(name = "articles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "article_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Article {
	@Id
	@Column(name = "article_id")
	private String id;
	
	@Column(name = "title")
	private String title;
	
	@Column(name = "content", columnDefinition = "TEXT")
	private String content;
	
	@Column(name = "writer_id")
	private String writerId;
}

@Entity
@DiscriminatorValue("EVENT")
public class EventArticle extends Article {
	@Column(name = "event_start_date")
	private LocalDateTime eventStartDate;
	
	@Column(name = "event_end_date")
	private LocalDateTime eventEndDate;
}

@Entity
@DiscriminatorValue("REGULAR")
public class RegularArticle extends Article {
	// No additional fields
}

@Entity
@DiscriminatorValue("NOTICE")
public class NoticeArticle extends Article {
	// No additional fields
}
```

### Index Strategy

```sql
-- 타입별 조회 최적화
CREATE INDEX idx_article_type_status ON articles (article_type, status);

-- 이벤트 날짜 조회 최적화
CREATE INDEX idx_event_dates ON articles (article_type, status, event_start_date, event_end_date) WHERE article_type = 'EVENT';

-- 일반 조회 최적화
CREATE INDEX idx_status_created ON articles (status, created_at DESC);
```

## Consequences

### Positive

- ✅ 조회 성능 최적화됨
- ✅ 코드 구조가 단순해짐
- ✅ 다형성이 자연스럽게 지원됨
- ✅ 새로운 게시글 타입 추가가 용이함

### Negative

- ⚠️ NULL 필드로 인한 저장 공간 오버헤드
- ⚠️ 타입별 제약조건 적용이 애플리케이션 레벨에서 필요
- ⚠️ 데이터베이스 레벨에서 타입별 필드 무결성 보장 어려움

### Mitigations

- CHECK 제약조건으로 타입별 필드 유효성 검증
- 애플리케이션 레벨에서 엄격한 유효성 검사
- 주기적인 데이터 무결성 검증 스크립트 실행

## References

- [JPA Inheritance Strategies](https://www.baeldung.com/hibernate-inheritance)
- [Martin Fowler - Single Table Inheritance](https://martinfowler.com/eaaCatalog/singleTableInheritance.html)
- [Hibernate Performance Tuning](https://vladmihalcea.com/hibernate-performance-tuning-tips/)

---

**Review History:**

- 2025-11-15: Initial proposal
- 2025-11-18: Accepted by team
- 2025-11-20: Implemented in production

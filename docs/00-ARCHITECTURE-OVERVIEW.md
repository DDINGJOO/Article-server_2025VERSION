# Article Server Architecture Overview

**Version**: 1.0.0
**Last Updated**: 2025-11-26
**Status**: Production
**Maintainer**: Team Bind Platform Engineering

---

## Table of Contents

1. [Introduction](#introduction)
2. [Architecture Styles](#architecture-styles)
3. [Layered Structure](#layered-structure)
4. [Core Domain Models](#core-domain-models)
5. [Technology Stack](#technology-stack)
6. [Key Design Decisions](#key-design-decisions)
7. [Deployment Architecture](#deployment-architecture)
8. [Evolution Roadmap](#evolution-roadmap)
9. [Reference Materials](#reference-materials)

---

## Introduction

Article Server는 **MSA(Microservice Architecture)** 환경에서 게시글 관리를 담당하는 독립적인 서비스입니다.

### Core Responsibilities

- **게시글 생명주기 관리**: 생성, 수정, 삭제 (Soft Delete), 상태 관리
- **다중 게시글 타입 지원**: Regular, Event, Notice 타입 별 특화 로직
- **키워드 시스템**: 게시글 분류 및 검색 최적화
- **이미지 관리**: 게시글 첨부 이미지 처리 및 이벤트 기반 동기화
- **이벤트 스트리밍**: Kafka를 통한 도메인 이벤트 발행/구독

---

## Architecture Styles

### 적용된 아키텍처 패턴

#### 1. Domain-Driven Design (DDD)

```java
// Aggregate Root
public abstract class ArticleAggregate extends AggregateRoot {
    @EmbeddedId
    private ArticleId id;           // Value Object
    @Embedded
    private Title title;            // Value Object
    @Embedded
    private Content content;        // Value Object
    @Embedded
    private WriterId writerId;      // Value Object
}
```

#### 2. Hexagonal Architecture (Ports & Adapters)

```
┌─────────────────────────────────────────┐
│         Inbound Ports (Use Cases)       │
├─────────────────────────────────────────┤
│        Application Services              │
├─────────────────────────────────────────┤
│         Outbound Ports                  │
├─────────────────────────────────────────┤
│  Adapters (Persistence, Messaging)      │
└─────────────────────────────────────────┘
```

#### 3. Factory Pattern

```java
public interface ArticleFactory {
    Article create(ArticleCreateRequest request);
    ArticleType getSupportedType();
}
```

---

## Layered Structure

```
article-server/
├── adapter/                    # Infrastructure Layer
│   ├── in/
│   │   └── web/               # REST Controllers (Driving Adapters)
│   └── out/
│       ├── persistence/       # JPA Repositories (Driven Adapters)
│       └── messaging/         # Kafka Publishers (Driven Adapters)
│
├── application/               # Application Layer
│   ├── port/
│   │   ├── in/               # Inbound Ports (Use Cases)
│   │   └── out/              # Outbound Ports
│   └── service/              # Application Services
│
├── domain/                    # Domain Layer
│   ├── aggregate/            # Aggregate Roots
│   ├── vo/                   # Value Objects
│   ├── event/                # Domain Events
│   └── repository/           # Repository Interfaces
│
├── factory/                   # Factory Pattern Implementation
│   ├── impl/                 # Concrete Factories
│   └── ArticleFactoryRegistry.java
│
└── entity/                    # JPA Entities (Legacy)
    ├── article/
    ├── articleType/
    ├── board/
    └── keyword/
```

---

## Core Domain Models

### Entity Hierarchy

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "article_type")
public abstract class Article {
    // Core fields with optimized indexes
}

@Entity
@DiscriminatorValue("REGULAR")
public class RegularArticle extends Article { }

@Entity
@DiscriminatorValue("EVENT")
public class EventArticle extends Article {
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
}

@Entity
@DiscriminatorValue("NOTICE")
public class NoticeArticle extends Article { }
```

### Value Objects

| Value Object | Purpose | Validation Rules                   |
|--------------|---------|------------------------------------|
| `ArticleId`  | 게시글 식별자 | Length: 10-50 chars                |
| `Title`      | 게시글 제목  | Max 200 chars, XSS sanitization    |
| `Content`    | 게시글 내용  | Max 65535 chars, HTML sanitization |
| `WriterId`   | 작성자 식별자 | Max 50 chars, Not null             |

### Domain Events

```java
public interface DomainEvent {
    String getEventId();
    LocalDateTime getOccurredAt();
    String getEventType();
    String getAggregateId();
}
```

---

## Technology Stack

### Core Frameworks

- **Spring Boot 3.5.6**: Application framework
- **Spring Data JPA**: ORM abstraction
- **QueryDSL 5.1.0**: Type-safe dynamic queries
- **Spring Kafka**: Event streaming

### Data Stores

- **MariaDB**: Primary database
- **Redis**: Caching & distributed locks (ShedLock)
- **H2**: In-memory database for testing

### Messaging

- **Apache Kafka**: Event bus for inter-service communication
	- Topics: `article.created`, `article.deleted`, `article-image-changed`

### Build & Test

- **Gradle**: Build automation
- **JUnit 5**: Unit testing
- **Mockito**: Test doubles
- **AssertJ**: Fluent assertions

---

## Key Design Decisions

### ADRs (Architecture Decision Records)

| ADR                                                    | Title                                      | Status   | Category       |
|--------------------------------------------------------|--------------------------------------------|----------|----------------|
| [ADR-001](adr/ADR-001-single-table-inheritance.md)     | Single Table Inheritance for Article Types | Accepted | Persistence    |
| [ADR-002](adr/ADR-002-snowflake-id-generation.md)      | Snowflake ID Generation Strategy           | Accepted | Identification |
| [ADR-003](adr/ADR-003-factory-pattern-adoption.md)     | Factory Pattern for Article Creation       | Accepted | Design Pattern |
| [ADR-004](adr/ADR-004-value-objects-implementation.md) | Value Objects for Domain Concepts          | Accepted | DDD            |
| [ADR-005](adr/ADR-005-hexagonal-architecture.md)       | Hexagonal Architecture Migration           | Accepted | Architecture   |

### Performance Optimizations

1. **Composite Indexes**
   ```sql
   idx_status_created_id (status, created_at, article_id)
   idx_board_status_created (board_id, status, created_at)
   idx_event_status_dates (article_type, status, event_start_date, event_end_date)
   ```

2. **Batch Fetching**
   ```java
   @BatchSize(size = 100)
   private List<KeywordMappingTable> keywordMappings;
   ```

3. **QueryDSL Dynamic Queries**
	- Cursor-based pagination
	- Subquery optimization for keyword filtering

---

## Deployment Architecture

### Container Strategy

```yaml
# Docker Compose Configuration
services:
  article-server:
    image: article-server:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_HOST=mariadb
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

### Database Indexing

```sql
-- Performance-critical indexes
CREATE INDEX idx_article_board ON articles(board_id);
CREATE INDEX idx_status_created_id ON articles(status, created_at, article_id);
CREATE INDEX idx_event_status_dates ON articles(article_type, status, event_start_date, event_end_date);
```

### Health Checks

- `/actuator/health`: Application health status
- `/actuator/metrics`: Performance metrics
- `/actuator/info`: Build information

---

## Evolution Roadmap

### Phase 1: Foundation (Completed)

- ✅ Basic CRUD operations
- ✅ Multi-type article support
- ✅ Keyword system
- ✅ Kafka integration

### Phase 2: Architecture Enhancement (Current)

- ✅ Factory Pattern implementation
- ✅ DDD tactical patterns
- ✅ Hexagonal Architecture
- 🔄 Comprehensive testing

### Phase 3: Advanced Features (Q1 2025)

- ⏳ CQRS implementation
- ⏳ Event Sourcing for audit trail
- ⏳ GraphQL API support
- ⏳ Full-text search with Elasticsearch

### Phase 4: Scalability (Q2 2025)

- ⏳ Read/write database separation
- ⏳ Caching strategy enhancement
- ⏳ Rate limiting
- ⏳ API versioning strategy

---

## Reference Materials

### Internal Documentation

- [Development Setup Guide](01-DEVELOPMENT-SETUP.md)
- [API Specification](API-SPECIFICATION.md)
- [Testing Guide](guides/TESTING-GUIDE.md)
- [Migration Guide](guides/MIGRATION-GUIDE.md)

### External Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.5.6/reference/)
- [Domain-Driven Design Reference](https://www.domainlanguage.com/ddd/reference/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

---

**Document Maintained by**: Platform Engineering Team
**Review Cycle**: Quarterly
**Next Review**: 2025-02-26

---

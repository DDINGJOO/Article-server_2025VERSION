# Article Server

게시글 생명주기를 전담 관리하는 Spring Boot 마이크로서비스입니다.

## 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [주요 기능](#주요-기능)
3. [아키텍처](#아키텍처)
4. [데이터베이스 스키마](#데이터베이스-스키마)
5. [API 엔드포인트](#api-엔드포인트)
6. [기술 스택](#기술-스택)
7. [설정 및 실행](#설정-및-실행)

---

## 프로젝트 개요

### 기본 정보

- **프로젝트명**: Article Server
- **타입**: Spring Boot REST API 마이크로서비스
- **Java**: 17
- **빌드**: Gradle 8.x
- **버전**: 0.0.1-SNAPSHOT

### 핵심 목적

마이크로서비스 아키텍처 환경에서 게시글 라이프사이클을 전담 관리하는 서버입니다.

- 게시글 CRUD (생성, 조회, 수정, 삭제)
- 다중 게시글 타입 지원 (일반, 이벤트, 공지사항)
- 이미지 연동 및 순서 관리
- 키워드 태깅 및 검색
- 커서 기반 페이지네이션
- Kafka 이벤트 기반 통합

---

## 주요 기능

### 1. 게시글 타입별 관리

#### Single Table Inheritance 패턴

- **RegularArticle**: 일반 게시글
- **EventArticle**: 이벤트 게시글 (기간 정보 포함)
- **NoticeArticle**: 공지사항

각 타입별 전용 엔드포인트와 비즈니스 로직 제공

### 2. 고급 검색 기능

#### QueryDSL 기반 동적 쿼리

- 게시판별 필터링
- 키워드 태깅 검색
- 제목/내용 전문 검색
- 작성자 검색
- 상태별 필터링 (ACTIVE, DELETED, BLOCKED)

#### 커서 기반 페이지네이션

- 무한 스크롤 지원
- updatedAt + articleId 복합 커서
- N+1 쿼리 문제 해결 (Fetch Join)
- 서브쿼리 최적화

### 3. 이미지 통합

- 외부 Image Server 연동
- 이미지 순서 관리 (ArticleImage 엔티티)
- 첫 이미지 URL 캐싱
- 이미지 변경 이벤트 구독 (Kafka)
- 자동 이미지 URL 동기화

### 4. 이벤트 기반 통합

#### Kafka Consumer

- 토픽: `ARTICLE-image-changed`
- 이미지 변경 이벤트 수신
- ArticleImage 자동 업데이트
- 재시도 로직 (ErrorHandlingDeserializer)

#### Kafka Producer

- 게시글 생성/수정/삭제 이벤트 발행
- 하위 서비스로 변경사항 전파
- 트랜잭션 안전성 보장

### 5. 스케줄링

#### 삭제 게시글 정리 스케줄러

- 매일 새벽 4시 15분 실행
- ShedLock 분산 락 (다중 인스턴스 환경)
- DELETED 상태 게시글 물리 삭제
- lockAtMostFor(9m), lockAtLeastFor(1m) 설정

### 6. 성능 최적화

#### 인메모리 캐싱

- DataInitializer를 통한 Board/Keyword 캐싱
- 애플리케이션 시작 시 로드
- 매일 자정 자동 갱신
- Validator에서 DB 쿼리 없이 검증 (1000배 이상 성능 향상)

#### 데이터베이스 인덱싱

- 복합 인덱스 전략
	- `idx_status_created_id`: 상태별 최신순 조회
	- `idx_status_updated_id`: 커서 페이징용
	- `idx_board_status_created`: 게시판별 조회
	- `idx_writer_status_created`: 작성자별 조회
	- `idx_type_status_created`: 타입별 조회

#### BatchSize 최적화

- @BatchSize(size = 10): 연관 엔티티 일괄 조회
- N+1 문제 해결

### 7. AOP 기반 로깅

#### @LogTrace 어노테이션

- 메서드 실행 시간 측정
- 파라미터/결과 로깅
- TraceId 생성으로 요청 추적
- 성능 임계값 경고 (1초, 3초)
- 예외 로깅

### 8. 계층 분리 및 검증

#### 어노테이션 기반 Validation

- `@ValidBoardId`: Board 존재 여부 검증
- `@ValidKeywordIds`: Keyword 리스트 검증
- `@ValidEventPeriod`: 이벤트 기간 검증
- DataInitializer 캐시 활용으로 DB 쿼리 제거

#### 계층 분리 원칙

- Controller: 요청/응답 처리
- Service: 비즈니스 로직
- Repository: 데이터 접근
- Controller에서 Repository 직접 참조 금지

---

## 아키텍처

### 계층 구조

\`\`\`
┌─────────────────────────────────────────┐
│ Controller Layer │
│  (Article, Event, Notice, Bulk)         │
└──────────────┬──────────────────────────┘
│
┌──────────────▼──────────────────────────┐
│ AOP Layer │
│  (@LogTrace - 성능 모니터링)              │
└──────────────┬──────────────────────────┘
│
┌──────────────▼──────────────────────────┐
│ Service Layer │
│  (ArticleCreateService)                  │
│  (ArticleReadService)                    │
└──────────────┬──────────────────────────┘
│
┌──────────────▼──────────────────────────┐
│ Event Layer │
│  (Kafka Consumer - 이미지 동기화)        │
│  (Kafka Producer - 변경 이벤트)         │
└──────────────┬──────────────────────────┘
│
┌──────────────▼──────────────────────────┐
│ Repository Layer │
│  (JPA + QueryDSL)                       │
│  (ArticleRepositoryCustom)               │
└──────────────┬──────────────────────────┘
│
┌──────────────▼──────────────────────────┐
│ Entity Layer │
│  (Article - Single Table Inheritance)    │
│  (Board, Keyword, ArticleImage)         │
└─────────────────────────────────────────┘
\`\`\`

---

## 기술 스택

### Core

- **Spring Boot**: 3.5.6
- **Java**: 17
- **Gradle**: 8.x

### Database

- **Production**: MariaDB
- **ORM**: Spring Data JPA (Hibernate)
- **Query**: QueryDSL 5.1.0

### Messaging

- **Kafka**: spring-kafka
- **ShedLock**: 5.10.2 (분산 락, Redis 기반)

### Validation

- **Jakarta Validation**: Bean Validation
- **Custom Validators**: @ValidBoardId, @ValidKeywordIds, @ValidEventPeriod

### Cache

- **Redis**: spring-data-redis
- **In-Memory**: DataInitializer (Board/Keyword 캐싱)

### Development

- **Lombok**: 코드 간소화
- **Slf4j**: 로깅

---

## 문서

### 관련 문서

- [API 명세서](docs/API_SPECIFICATION.md)
- [AOP 로그 트레이서 가이드](docs/guides/AOP_LOG_TRACER_GUIDE.md)
- [Validation 가이드](docs/guides/VALIDATION_GUIDE.md)
- [Validation 성능 최적화](docs/VALIDATION_PERFORMANCE_OPTIMIZATION.md)

### 메타 정보

- **작성일**: 2025-10-25
- **최종 업데이트**: 2025-10-25
- **버전**: 0.0.1-SNAPSHOT

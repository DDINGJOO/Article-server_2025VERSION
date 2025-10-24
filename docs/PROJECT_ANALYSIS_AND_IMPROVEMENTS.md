# Article Server 프로젝트 분석 보고서

> 작성일: 2025-10-24
> 버전: 2.0.0
> 대상: Article Server (Spring Boot 3.5.6)

---

## 📋 프로젝트 개요

Spring Boot 3.5.6 기반의 게시글 관리 마이크로서비스로, Kafka 이벤트 기반 아키텍처를 활용하여 이미지 서비스와 통합되어 있습니다.

**기술 스택:** Java 17, Spring Boot 3.x, MariaDB, QueryDSL, Apache Kafka, Redis(비활성), Docker

---

## 🔴 보안 취약점

> **참고:** 인증/인가는 별도의 인증 서버에서 처리되므로 이 서비스에서는 구현하지 않습니다.

### 1. **[높음] 테스트 환경 자격증명 하드코딩**

**파일:** `src/main/resources/application-test.yaml:8`

**문제점:**

- 데이터베이스 비밀번호가 코드 저장소에 노출됨
- 보안 정책 위반

**개선 방안:**

- 환경변수 또는 `.env.test` 파일 사용
- `.gitignore`에 환경설정 파일 추가

---

### 2. **[중간] SQL 쿼리 노출**

**파일:** `application-dev.yaml`, `application-test.yaml`

**문제점:**

- `show-sql: true` 설정으로 로그에 모든 SQL 쿼리 기록
- 민감한 데이터 노출 가능성

**개선 방안:**

- 운영 환경에서 `show-sql: false` 설정
- 필요시 로그 레벨로 제어

---

### 3. **[중간] 입력 값 검증 부족**

**파일:** `src/main/java/com/teambind/articleserver/dto/request/*`

**문제점:**

- DTO에 검증 어노테이션 누락 (`@NotBlank`, `@Length`, `@Pattern`)
- XSS, SQL Injection 방어 미흡
- 제목/내용 길이 제한 없음

**개선 방안:**

- Jakarta Validation 어노테이션 추가
- 컨트롤러에 `@Validated` 적용
- 입력 값 길이/형식 제한

---

## ⚠️ 코드 품질 이슈

### 1. **[높음] 중복 코드 - 검색 로직**

**파일:**

- `ArticleController.java:70-130`
- `RegularArticleController.java:60-120`

**문제점:**

- 동일한 검색 파라미터 처리 로직 중복 (약 60줄)
- URL 디코딩 로직 중복

**개선 방안:**

- `SearchParamsExtractor` 컴포넌트 생성
- 공통 로직 추출

---

### 2. **[중간] 예외 응답 정보 부족**

**파일:** `GlobalExceptionHandler.java`

**문제점:**

- 에러 응답에 에러 코드 없음
- 타임스탬프, 요청 경로 정보 없음
- 클라이언트 디버깅 어려움

**개선 방안:**

- `ErrorResponse` DTO 생성 (errorCode, message, timestamp, path, validationErrors)
- 표준화된 에러 응답 구조 적용

---

### 3. **[중간] Type Safety 부족**

**파일:** `ArticleCreateRequestDto.java`

**문제점:**

- `List<?>`, `Object` 타입 사용으로 타입 안정성 부족
- `ConvertorImpl`에서 런타임 타입 체크 필요

**개선 방안:**

- 명확한 타입 정의 (`List<Long>` 또는 `List<String>`)
- 또는 `@JsonTypeInfo`를 사용한 다형성 처리

---

### 4. **[낮음] TODO 주석 미완성**

**파일:** `Article.java:75`

**문제점:**

- `//TODO : // TEST PLZ!!` 주석 방치
- `addKeywords()` 메서드 테스트 미작성

**개선 방안:**

- 테스트 코드 작성 또는 TODO 제거

---

### 5. **[완료] Null Safety 이슈**

**파일:** `KafkaConsumer.java`

**상태:** ✅ 리팩토링 완료

- Wrapper 및 리스트 Null 체크 추가
- 이미지 ID/URL Null 검증 추가

---

## 🚀 성능 개선 방안

### 1. **N+1 쿼리 문제**

**문제점:**

- `Article` 엔티티의 `keywords`, `images`가 LAZY 로딩
- 커서 검색 시 각 게시글마다 추가 쿼리 발생 가능

**개선 방안:**

- QueryDSL에서 `fetchJoin()` 사용
- 또는 `@BatchSize(size = 10)` 어노테이션 적용

---

### 2. **정적 맵 초기화 성능**

**파일:** `DataInitializer.java`

**문제점:**

- 애플리케이션 시작 시 모든 키워드/게시판을 메모리에 로드
- 데이터가 많아질 경우 메모리 낭비
- Thread-unsafe한 static 맵 사용

**개선 방안:**

- Redis 캐싱 활용 (`@Cacheable`)
- 또는 Lazy 로딩 방식으로 변경

---

### 3. **Kafka 동기 전송**

**파일:** `KafkaPublisher.java`

**문제점:**

- 동기 방식으로 메시지 전송 (블로킹)
- 전송 실패 시 에러 처리 없음

**개선 방안:**

- `CompletableFuture`를 사용한 비동기 전송
- 콜백으로 성공/실패 처리
- 재시도 로직 또는 DLQ 전송

---

### 4. **DB 커넥션 풀 최적화**

**문제점:**

- HikariCP 기본 설정 사용 중
- 커넥션 풀 튜닝 미적용

**개선 방안:**

- `maximum-pool-size`, `minimum-idle` 설정
- `leak-detection-threshold` 설정으로 커넥션 누수 감지

---

## 🏗️ 아키텍처 개선 제안

### 1. **이벤트 소싱 패턴 도입**

**현재 상태:**

- 게시글 생성/수정 시 단순 이벤트 발행

**개선 방안:**

- 모든 게시글 변경을 이벤트로 저장
- 변경 이력 추적 가능
- 감사(Audit) 기능 구현 용이

---

### 2. **CQRS 패턴 적용**

**개선 방안:**

- Command Model (쓰기)와 Query Model (읽기) 분리
- 읽기 성능 최적화를 위한 별도 DB 또는 캐시 활용

---

### 3. **멀티 테넌시 강화**

**현재 상태:**

- `Board` 기반 분리

**개선 방안:**

- `tenantId`, `workspaceId` 추가
- Hibernate Filter로 자동 필터링

---

## 📊 모니터링 및 관찰성

### 1. **로깅 개선**

**현재 상태:**
- 기본 로깅만 사용
- 구조화된 로그 없음
- 추적 ID 없음

**개선 방안:**

- Logback JSON 인코더 추가 (`logstash-logback-encoder`)
- MDC를 이용한 Trace ID 추가
- 요청별 추적 가능하도록 개선

---

### 2. **메트릭 수집**

**개선 방안:**

- Micrometer + Prometheus 연동
- 커스텀 메트릭 추가 (게시글 생성 수, 처리 시간 등)
- Grafana 대시보드 구축

---

### 3. **분산 추적**

**개선 방안:**

- Spring Cloud Sleuth + Zipkin 도입
- 마이크로서비스 간 요청 추적

---

### 4. **Kafka Consumer Lag 모니터링**

**개선 방안:**

- Kafka Admin API로 Consumer Group Lag 확인
- Prometheus로 메트릭 수집
- 임계값 초과 시 알림 설정

---

## 🧪 테스트 개선 방안

### 1. **Kafka 통합 테스트 추가**

**현재 상태:**

- Kafka Producer/Consumer 테스트 없음

**개선 방안:**

- `@EmbeddedKafka`를 사용한 통합 테스트 작성
- 이벤트 발행 및 소비 테스트

---

### 2. **낙관적 락 충돌 테스트**

**개선 방안:**

- 동시 업데이트 시나리오 테스트
- `OptimisticLockingFailureException` 처리 검증

---

### 3. **성능 테스트**

**개선 방안:**

- JMeter 또는 Gatling을 사용한 부하 테스트
- 커서 페이징 성능 측정 (1000개 이상 데이터)

---

## 📝 우선순위별 개선 로드맵

### **Phase 1: 보안 및 입력 검증 (1주)**

| 순위 | 작업                  | 예상 시간 |
|----|---------------------|-------|
| 1  | 입력 값 검증 어노테이션 추가    | 1일    |
| 2  | 테스트 자격증명 환경변수화      | 0.5일  |
| 3  | SQL 로깅 비활성화 (운영 환경) | 0.5일  |
| 4  | 에러 응답 표준화           | 1일    |

---

### **Phase 2: 코드 품질 개선 (2주)**

| 순위 | 작업                        | 예상 시간 |
|----|---------------------------|-------|
| 1  | 중복 검색 로직 리팩토링             | 2일    |
| 2  | DTO 타입 안정성 개선             | 1일    |
| 3  | GlobalExceptionHandler 확장 | 1일    |
| 4  | TODO 주석 해결 및 테스트 추가       | 2일    |

---

### **Phase 3: 성능 최적화 (2~3주)**

| 순위 | 작업                    | 예상 시간 |
|----|-----------------------|-------|
| 1  | N+1 쿼리 제거             | 3일    |
| 2  | Redis 캐싱 활성화          | 2일    |
| 3  | Kafka 비동기 전송 + 재시도 로직 | 2일    |
| 4  | DB 커넥션 풀 튜닝           | 1일    |
| 5  | QueryDSL 쿼리 최적화       | 2일    |

---

### **Phase 4: 관찰성 강화 (1~2주)**

| 순위 | 작업                           | 예상 시간 |
|----|------------------------------|-------|
| 1  | 구조화된 로깅 (JSON) + Trace ID    | 2일    |
| 2  | Prometheus + Grafana 메트릭     | 2일    |
| 3  | Kafka Consumer Lag 모니터링      | 1일    |
| 4  | Spring Cloud Sleuth + Zipkin | 2일    |

---

### **Phase 5: 테스트 커버리지 향상 (1주)**

| 순위 | 작업                      | 예상 시간 |
|----|-------------------------|-------|
| 1  | Kafka 통합 테스트            | 2일    |
| 2  | 낙관적 락 충돌 테스트            | 1일    |
| 3  | 성능 테스트 (JMeter/Gatling) | 2일    |

---

### **Phase 6: 아키텍처 진화 (1~2개월, 선택)**

| 순위 | 작업         | 예상 시간 |
|----|------------|-------|
| 1  | CQRS 패턴 적용 | 2주    |
| 2  | 이벤트 소싱 도입  | 3주    |
| 3  | 멀티 테넌시 강화  | 1주    |

---

## 📌 즉시 적용 가능한 Quick Wins

### 1. 테스트 자격증명 환경변수화 (5분)

- `application-test.yaml`에서 하드코딩된 비밀번호 제거
- 환경변수로 대체

### 2. 운영 환경 SQL 로깅 비활성화 (2분)

- `application-prod.yaml`에서 `show-sql: false` 설정

### 3. 입력 검증 의존성 추가 (1분)

- `build.gradle`에 `spring-boot-starter-validation` 추가

### 4. Health Check 엔드포인트 활성화 (1분)

- Spring Boot Actuator health 엔드포인트 활성화

---

## 🎯 요약

### **현재 상태**

Article Server는 잘 구조화된 Spring Boot 마이크로서비스이며, 몇 가지 개선이 필요합니다.

### **가장 시급한 문제 3가지**

1. ⚠️ **테스트 자격증명 하드코딩** - 환경변수로 즉시 전환 필요
2. ⚠️ **입력 값 검증 부족** - Jakarta Validation 적용 필요
3. ⚠️ **코드 중복** - 검색 로직 리팩토링 필요

### **강점**

- ✅ 명확한 계층 분리 (Controller-Service-Repository)
- ✅ QueryDSL 기반 타입 안전 쿼리
- ✅ 커서 기반 페이징으로 성능 최적화
- ✅ Kafka 이벤트 기반 아키텍처
- ✅ Docker 기반 멀티 인스턴스 배포
- ✅ Kafka Consumer 리팩토링 완료 (이미지 서버 가이드 준수)

---

## 📚 참고 자료

### 성능

- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP)
- [JPA N+1 Problem Solutions](https://vladmihalcea.com/n-plus-1-query-problem/)
- [Kafka Performance Tuning](https://kafka.apache.org/documentation/#producerconfigs)

### 모니터링

- [Micrometer Documentation](https://micrometer.io/docs)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Distributed Tracing with Sleuth](https://spring.io/projects/spring-cloud-sleuth)

### 보안

- [OWASP Top 10 2021](https://owasp.org/Top10/)
- [Input Validation Best Practices](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)

---

**권장 조치:** Phase 1 (입력 검증 및 자격증명 보안)을 최우선으로 진행하시길 권장드립니다.

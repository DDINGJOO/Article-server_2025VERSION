# 성능 테스트 가이드

## 목차

1. [개요](#개요)
2. [초기 설정](#초기-설정)
3. [테스트 데이터 준비](#테스트-데이터-준비)
4. [성능 테스트 실행](#성능-테스트-실행)
5. [코드 수정 후 재테스트](#코드-수정-후-재테스트)
6. [결과 비교 분석](#결과-비교-분석)
7. [실전 시나리오](#실전-시나리오)

---

## 개요

이 가이드는 Article Server의 조회 성능을 지속적으로 측정하고 개선하는 방법을 설명합니다.
**메서드 이름이나 구현이 변경되어도** 동일한 테스트 시나리오로 성능을 측정할 수 있습니다.

### 핵심 원리

- **테스트 시나리오는 변경하지 않음** (일관된 비교 기준)
- **실제 서비스 메서드를 호출** (ArticleReadService 등)
- **60만 건의 실제 데이터로 테스트**
- **자동으로 성능 지표 수집** (p50, p95, p99, 쿼리 수)

---

## 초기 설정

### 1. Docker 환경 시작

```bash
# Docker Compose로 DB, Redis 시작
docker compose up -d article-mariadb article-redis

# DB 상태 확인
docker compose ps
```

### 2. 테스트 데이터 생성 (최초 1회)

```bash
# 60만 건 데이터 생성 (약 5-10분 소요)
./scripts/performance/setup-test-data.sh -c 600000 -v

# 또는 Gradle로 직접 실행
./gradlew test --tests PerformanceDataGeneratorTest.generatePerformanceTestData
```

### 3. 데이터 생성 확인

```sql
# Docker로 DB 접속
docker
compose exec article-mariadb mysql -u root -particlepass123 article_db

# 데이터 확인
SELECT COUNT(*)
FROM articles; -- 600,000
SELECT COUNT(*)
FROM article_images; -- 1,800,000
SELECT COUNT(*)
FROM keyword_mapping_table; -- 2,400,000
```

---

## 성능 테스트 실행

### 방법 1: Shell 스크립트 사용 (권장)

```bash
# 단건 조회 성능 테스트
./scripts/performance/run-performance-test.sh -s single

# 페이지네이션 테스트
./scripts/performance/run-performance-test.sh -s pagination

# 복잡한 검색 테스트
./scripts/performance/run-performance-test.sh -s complex

# 동시 사용자 부하 테스트
./scripts/performance/run-performance-test.sh -s concurrent

# 모든 테스트 실행
./scripts/performance/run-performance-test.sh -s all
```

### 방법 2: Gradle 직접 실행

```bash
# 특정 테스트만 실행
./gradlew test \
  --tests "QueryPerformanceTest.testSingleArticleReadPerformance" \
  -Dspring.profiles.active=performance-test

# 전체 성능 테스트 실행
./gradlew test \
  --tests "QueryPerformanceTest" \
  -Dspring.profiles.active=performance-test
```

### 방법 3: IDE에서 실행

1. IntelliJ IDEA에서 `QueryPerformanceTest` 클래스 열기
2. 테스트 메서드 옆의 실행 버튼 클릭
3. Run Configuration에서 Active profiles: `performance-test` 설정

---

## 코드 수정 후 재테스트

### 시나리오: 쿼리 최적화 진행

#### Step 1: 현재 성능 측정 (베이스라인)

```bash
# 최적화 전 성능 측정
./scripts/performance/run-performance-test.sh -s all

# 결과가 performance-results/ 디렉토리에 저장됨
ls -la performance-results/
# performance-metrics-single-20241126-143022.json
# performance-metrics-pagination-20241126-143022.json
# ...
```

#### Step 2: 코드 수정

```java
// 예: ArticleRepositoryCustomImpl.java 수정

// AS-IS (최적화 전)
public List<Article> searchByCursor(...) {
	// N+1 문제가 있는 코드
	return queryFactory
			.selectFrom(ARTICLE)
			.where(conditions)
			.fetch();
}

// TO-BE (최적화 후)
public List<Article> searchByCursor(...) {
	// Fetch Join 추가
	return queryFactory
			.selectFrom(ARTICLE)
			.leftJoin(ARTICLE.board, BOARD).fetchJoin()
			.leftJoin(ARTICLE.images).fetchJoin()  // N+1 해결
			.where(conditions)
			.distinct()
			.fetch();
}
```

#### Step 3: 동일한 테스트 재실행

```bash
# 코드 수정 후 재빌드
./gradlew clean build

# 동일한 테스트 재실행
./scripts/performance/run-performance-test.sh -s all
```

#### Step 4: 결과 비교

```bash
# 자동 비교 (최신 2개 결과)
./scripts/performance/compare-results.sh

# 수동 비교 (특정 파일)
./scripts/performance/run-performance-test.sh \
  -s single \
  -c performance-results/performance-metrics-single-20241126-143022.json
```

---

## 결과 비교 분석

### 성능 지표 해석

#### 1. 응답 시간 (Response Time)

```
P50: 15ms   → 50%의 요청이 15ms 이내 처리
P95: 45ms   → 95%의 요청이 45ms 이내 처리  ← 주요 지표
P99: 120ms  → 99%의 요청이 120ms 이내 처리
```

#### 2. 쿼리 수 (Query Count)

```
평균 쿼리 수: 3.2  → 요청당 평균 3.2개 SQL 실행
최대 쿼리 수: 5    → 최악의 경우 5개 SQL 실행
```

### 개선 판단 기준

| 지표                | 목표      | 양호        | 개선 필요   |
|-------------------|---------|-----------|---------|
| P95 Response Time | < 50ms  | 50-100ms  | > 100ms |
| P99 Response Time | < 150ms | 150-300ms | > 300ms |
| 쿼리 수/요청           | < 3     | 3-5       | > 5     |
| 에러율               | 0%      | < 0.1%    | > 0.1%  |

### 비교 결과 예시

```
=== 성능 최적화 비교 결과 ===

📊 시나리오: 단건 조회
🕐 측정 시간: 2024-11-26 14:30:22

📈 개선 효과:
  • 응답 시간: 65.3% 개선
  • 쿼리 수: 60.0% 감소
  • 처리량: 180.5% 증가

📋 상세 비교:
┌─────────────┬──────────┬──────────┬──────────┐
│   Metric    │  Before  │  After   │  Change  │
├─────────────┼──────────┼──────────┼──────────┤
│ P95 (ms)    │   120.50 │    42.30 │  -64.9%  │
│ Queries/req │     5.00 │     2.00 │  -60.0%  │
└─────────────┴──────────┴──────────┴──────────┘

✅ 결론: 성능 개선 확인 ✨
```

---

## 실전 시나리오

### 시나리오 1: 인덱스 추가 효과 측정

```bash
# 1. 현재 성능 측정
./scripts/performance/run-performance-test.sh -s complex

# 2. 인덱스 추가
docker compose exec article-mariadb mysql -u root -particlepass123 article_db -e "
CREATE INDEX idx_board_status_created
ON articles(board_id, status, created_at);
"

# 3. 재측정
./scripts/performance/run-performance-test.sh -s complex

# 4. 결과 확인
# P95: 200ms → 30ms (85% 개선)
# 쿼리 실행 계획도 함께 확인
```

### 시나리오 2: N+1 문제 해결

```java
// QueryPerformanceTest.java는 수정하지 않음!
// ArticleReadService.java만 수정

// AS-IS
public Article fetchArticleById(String articleId) {
	return articleRepository.findById(articleId)
			.orElseThrow(...);
	// Lazy Loading으로 images, keywords 조회시 추가 쿼리
}

// TO-BE
public Article fetchArticleById(String articleId) {
	return articleRepository.findByIdWithAssociations(articleId)
			.orElseThrow(...);
	// Fetch Join으로 한 번에 조회
}
```

```bash
# 동일한 테스트로 개선 확인
./scripts/performance/run-performance-test.sh -s single

# 결과: 쿼리 수 5 → 1 (80% 감소)
```

### 시나리오 3: 캐시 적용 효과

```java
// Redis 캐시 적용
@Cacheable(value = "articles", key = "#articleId")
public Article fetchArticleById(String articleId) {
	// 기존 로직 그대로
}
```

```bash
# 캐시 효과 측정 전용 테스트
./gradlew test --tests "QueryPerformanceTest.testCacheEffectiveness"

# 결과:
# Cold Cache: P95 = 50ms
# Warm Cache: P95 = 5ms (90% 개선)
```

### 시나리오 4: 동시성 처리 개선

```bash
# 동시 사용자 부하 테스트
./scripts/performance/run-performance-test.sh -s concurrent

# Connection Pool 튜닝 후
# application-performance-test.yml 수정
# hikari.maximum-pool-size: 30 → 50

# 재측정
./scripts/performance/run-performance-test.sh -s concurrent

# 결과: 200명 동시 접속시 에러율 5% → 0%
```

---

## 자주 묻는 질문 (FAQ)

### Q1: 메서드 이름을 변경했는데 테스트가 깨집니다

**A:** `QueryPerformanceTest`는 `ArticleReadService`의 공개 메서드만 호출합니다.
서비스 레이어의 public 메서드 시그니처만 유지하면 내부 구현은 자유롭게 변경 가능합니다.

### Q2: 새로운 최적화 기법을 테스트하고 싶습니다

**A:** `OptimizationComparisonTest`에 새 테스트 메서드를 추가하세요:

```java

@Test
public void testMyOptimization() {
	// 최적화 전 코드
	Runnable before = () -> { /* ... */ };
	
	// 최적화 후 코드
	Runnable after = () -> { /* ... */ };
	
	PerformanceComparator.compareOptimization("my_optimization", before, after);
}
```

### Q3: CI/CD에 통합하고 싶습니다

**A:** GitHub Actions 예시:

```yaml
- name: Run Performance Test
  run: |
    ./scripts/performance/setup-test-data.sh -c 100000
    ./scripts/performance/run-performance-test.sh -s all

- name: Check Performance Regression
  run: |
    # P95가 100ms 넘으면 실패
    ./gradlew test --tests "QueryPerformanceTest" \
      -DmaxP95=100 -DmaxQueries=5
```

### Q4: 프로덕션 데이터와 유사하게 테스트하고 싶습니다

**A:** `PerformanceDataGenerator`를 수정하여 실제 데이터 분포를 반영:

```java
// 실제 데이터 분포 반영
private String getArticleType(int index) {
	int mod = index % 100;
	if (mod < 70) return "RegularArticle";     // 70%
	if (mod < 95) return "NoticeArticle";      // 25%
	return "EventArticle";                      // 5%
}
```

### Q5: 결과를 시각화하고 싶습니다

**A:** CSV 파일을 Excel이나 Grafana로 import:

```bash
# CSV 생성
cat performance-results/*.json | jq -r '
  [.metadata.timestamp, .response_time_ms.p95, .query_metrics.averageCount]
  | @csv' > performance-trend.csv

# Grafana 대시보드 또는 Excel 차트로 시각화
```

---

## 트러블슈팅

### 문제: OutOfMemoryError

```bash
# JVM 힙 크기 증가
export GRADLE_OPTS="-Xmx4g -Xms2g"
./gradlew test --tests QueryPerformanceTest
```

### 문제: Connection Pool 부족

```yaml
# application-performance-test.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # 증가
```

### 문제: 테스트 데이터 불일치

```bash
# 데이터 검증
docker compose exec article-mariadb mysql -u root -particlepass123 article_db -e "
  SELECT COUNT(*) as count, article_type
  FROM articles
  GROUP BY article_type;
"

# 필요시 재생성
./scripts/performance/setup-test-data.sh -r -c 600000
```

---

## 다음 단계

1. **성능 목표 설정**: SLA에 따른 목표 지표 정의
2. **자동화**: CI/CD 파이프라인에 성능 테스트 통합
3. **모니터링**: 프로덕션 메트릭과 테스트 결과 비교
4. **최적화**: 병목 지점 식별 및 개선

---

## 참고 자료

- [ADR-006: Performance Test Framework](./docs/adr/ADR-006-performance-test-framework.md)
- [Performance Test Architecture](./docs/performance-test-architecture.md)
- [Spring Boot Performance Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html)

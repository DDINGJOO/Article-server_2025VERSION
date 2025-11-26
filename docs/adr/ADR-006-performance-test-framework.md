# 아키텍처 결정 기록 (ADR): 성능 테스트 프레임워크 선택

## 상태

승인됨 (Accepted)

## 컨텍스트

Article Server는 대규모 트래픽을 처리해야 하는 MSA 환경의 핵심 서비스입니다.
현재 시스템은 다음과 같은 규모를 처리해야 합니다:

- 게시글: 60만 건 이상
- 이미지: 게시글당 3개 (총 180만 개)
- 키워드: 게시글당 4개 (총 240만 개)
- 동시 사용자: 최대 1,000명

성능 최적화의 효과를 정량적으로 측정하고, 지속적으로 모니터링할 수 있는
체계적인 성능 테스트 프레임워크가 필요합니다.

## 고려된 대안들

### 대안 1: JMH (Java Microbenchmark Harness)

**특징**: JVM 최적화에 특화된 마이크로 벤치마크 도구

**선택하지 않은 이유**:

- 단위 메서드 수준의 성능 측정에 적합하지만, 실제 운영 환경 재현 어려움
- DB, Redis, Kafka 등 외부 의존성과의 통합 테스트 제한적
- HTTP 요청/응답 전체 플로우 측정 불가
- 동시성 테스트 시나리오 구현 복잡

### 대안 2: Spring Boot Test + TestContainers

**특징**: 컨테이너 기반 통합 테스트 프레임워크

**선택하지 않은 이유**:

- TestContainers 시작/종료 오버헤드로 대규모 데이터 테스트 비효율적
- 60만 건 데이터 초기화 시 매번 컨테이너 재시작 필요
- 실제 Docker Compose 환경과 차이 존재
- 성능 메트릭 수집 기능 제한적 (별도 구현 필요)

### 대안 3: Gatling/JMeter

**특징**: 부하 테스트 전문 도구

**선택하지 않은 이유**:

- 외부 HTTP 요청 기반으로 내부 쿼리 카운트 측정 어려움
- Spring 애플리케이션과의 통합 복잡
- Hibernate Statistics 접근 불가
- 별도 스크립트 언어 학습 필요 (Scala/XML)
- 데이터 생성과 테스트 실행의 분리로 관리 복잡도 증가

### 대안 4: Custom Performance Framework (선택)

**특징**: Spring Boot 기반 맞춤형 성능 테스트 프레임워크

## 결정

**Custom Performance Framework (하이브리드 접근법)를 선택합니다.**

## 선택 근거

### 1. 기술적 적합성

#### 1.1 실제 환경과의 일치성

```yaml
실제 운영 환경:
  - MariaDB 10.11
  - Redis 7.2
  - Kafka with Zookeeper

테스트 환경 (Docker Compose):
  - 동일한 버전의 MariaDB, Redis, Kafka
  - 실제 네트워크 레이턴시 포함
  - 실제 트랜잭션 격리 수준 적용
```

#### 1.2 정밀한 메트릭 수집

```java
// Hibernate Statistics로 쿼리 카운트 정확히 추적
@PerformanceTest
public ArticleResponse getArticle(String id) {
	// 자동으로 다음 메트릭 수집:
	// - SQL 실행 횟수
	// - N+1 쿼리 발생 여부
	// - Lazy Loading 패턴
	// - Cache Hit/Miss
}
```

#### 1.3 Spring 생태계 완벽 통합

- 기존 Repository, Service 레이어 그대로 활용
- Spring AOP로 비침투적 성능 측정
- Spring Profile로 환경별 설정 관리
- Spring Boot Actuator와 연동 가능

### 2. 비즈니스 가치

#### 2.1 빠른 피드백 사이클

```bash
# 개발자가 코드 수정 후 즉시 성능 영향 확인
./run-performance-test.sh --scenario quick-check

# 결과 (30초 내):
Query Count: 5 → 2 (60% 감소)
Response Time p95: 100ms → 25ms (75% 개선)
```

#### 2.2 지속적 성능 모니터링

- CI/CD 파이프라인에 통합하여 성능 저하 조기 감지
- PR 단위로 성능 영향 자동 리포트
- 릴리즈 전 성능 검증 자동화

#### 2.3 최적화 효과 정량화

```json
{
  "optimization": "Added composite index",
  "impact": {
    "query_time": {
      "before": "450ms",
      "after": "15ms",
      "improvement": "96.7%"
    },
    "queries_executed": {
      "before": 12,
      "after": 3,
      "reduction": "75%"
    }
  }
}
```

### 3. 아키텍처 원칙 준수

#### 3.1 SOLID 원칙 적용

```java
// Single Responsibility
class QueryCountCollector { /* 쿼리 카운트만 */
}

class ResponseTimeCollector { /* 응답 시간만 */
}

// Open-Closed: 새 메트릭 추가 시
class CacheHitRatioCollector implements MetricCollector {
	// 기존 코드 수정 없이 추가
}

// Dependency Inversion
@Component
class PerformanceTestRunner {
	// 구체 클래스가 아닌 인터페이스 의존
	private final List<MetricCollector> collectors;
}
```

#### 3.2 DDD 관점

- **도메인 언어 사용**: ArticleReadPerformance, QueryOptimization 등
- **Bounded Context 분리**: 성능 테스트는 독립적 모듈
- **도메인 이벤트**: 성능 임계값 초과 시 이벤트 발행

#### 3.3 MSA 패턴 적용

- **Circuit Breaker 패턴**: 성능 저하 시 자동 차단
- **Bulkhead 패턴**: 테스트별 리소스 격리
- **Service Mesh 호환**: Istio/Linkerd 메트릭과 통합 가능

### 4. 실용적 이점

#### 4.1 개발자 경험 (DX)

```java
// 간단한 어노테이션으로 성능 테스트
@Test
@PerformanceTest(
		warmup = 10,
		iterations = 100,
		expectedP95 = "50ms",
		maxQueries = 5
)
void testArticleSearch() {
	// 일반 테스트 코드와 동일
	var result = articleService.search(criteria);
	assertThat(result).isNotEmpty();
}
```

#### 4.2 비용 효율성

- 외부 도구 라이선스 불필요
- 기존 인프라 활용 (Docker, Spring Boot)
- 팀 내 Java/Spring 전문성 활용

#### 4.3 유지보수성

- 프로덕션 코드와 동일한 언어/프레임워크
- IDE 지원 (디버깅, 리팩토링)
- 기존 코드 리뷰 프로세스 적용 가능

### 5. 리스크 완화

#### 5.1 초기 구현 비용

**리스크**: 커스텀 프레임워크 개발 시간
**완화 방안**:

- Phase별 점진적 구현
- 핵심 기능부터 우선 개발
- 오픈소스 라이브러리 적극 활용 (Micrometer, HdrHistogram)

#### 5.2 정확성 검증

**리스크**: 자체 구현 메트릭의 신뢰성
**완화 방안**:

- JMH와 교차 검증
- Production 메트릭과 비교
- 오픈소스 구현 참조 (Spring Boot Actuator)

#### 5.3 확장성

**리스크**: 새로운 요구사항 대응
**완화 방안**:

- 플러그인 아키텍처 설계
- 인터페이스 기반 확장 포인트
- 설정 파일로 동작 커스터마이징

## 예상 결과

### 단기 (1-2주)

- 기본 성능 메트릭 수집 가능
- 쿼리 최적화 효과 즉시 확인
- 개발 중 성능 저하 조기 발견

### 중기 (1-2개월)

- CI/CD 파이프라인 통합 완료
- 성능 트렌드 추적 시작
- 병목 지점 자동 식별

### 장기 (6개월)

- 성능 기준선(Baseline) 확립
- 예측적 성능 분석
- 자동 성능 튜닝 제안

## 의사결정 참여자

- **제안**: 시니어 백엔드 개발자
- **검토**: 개발팀 전체
- **승인**: 기술 리드
- **날짜**: 2024년 11월 26일

## 추가 고려사항

### 향후 통합 가능성

- **APM 도구**: Datadog, New Relic과 연동
- **클라우드 네이티브**: K8s HPA 메트릭 제공
- **AI/ML**: 성능 이상 탐지 모델 학습 데이터

### 대안 재검토 시점

- 테스트 규모가 1000만 건 초과 시
- 분산 환경 테스트 필요 시
- 실시간 스트리밍 데이터 테스트 필요 시

## 결론

Custom Performance Framework는 다음과 같은 명확한 이점을 제공합니다:

1. **정확성**: 실제 환경과 동일한 조건에서 측정
2. **통합성**: Spring 생태계와 완벽한 통합
3. **유연성**: 다양한 시나리오와 메트릭 지원
4. **실용성**: 즉각적인 피드백과 지속적 모니터링
5. **확장성**: SOLID 원칙 기반 확장 가능한 설계

이는 단순한 테스트 도구를 넘어, Article Server의 지속적인 성능 개선을 위한
핵심 인프라가 될 것입니다.

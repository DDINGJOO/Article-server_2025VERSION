# Article Server 성능 테스트 Quick Start

## 5분 안에 시작하기

### 1 환경 준비 (1분)

```bash
# DB 시작
docker compose up -d article-mariadb article-redis
```

### 2 테스트 데이터 생성 (4분)

```bash
# 60만 건 데이터 생성
./scripts/performance/setup-test-data.sh -c 600000 -v
```

### 3 성능 테스트 실행

```bash
# 모든 시나리오 테스트
./scripts/performance/run-performance-test.sh -s all
```

---

## 테스트 시나리오

| 시나리오   | 명령어             | 측정 항목                       |
|--------|-----------------|-----------------------------|
| 단건 조회  | `-s single`     | 개별 게시글 조회 성능                |
| 페이지네이션 | `-s pagination` | 목록 조회 성능 (10, 20, 50, 100건) |
| 복잡한 검색 | `-s complex`    | 키워드 + 게시판 필터링               |
| 동시 사용자 | `-s concurrent` | 10, 50, 100, 200명 동시 접속     |
| 캐시 효과  | `-s cache`      | Cold vs Warm Cache 비교       |

---

## 코드 수정 → 재테스트 워크플로우

### 예시: 쿼리 최적화

```bash
# 1. 현재 성능 측정 (베이스라인)
./scripts/performance/run-performance-test.sh -s single
# 결과: P95 = 100ms, 쿼리 5개

# 2. 코드 수정
# ArticleRepositoryCustomImpl.java 수정
# - Fetch Join 추가
# - 인덱스 최적화

# 3. 재테스트 (동일한 명령어!)
./scripts/performance/run-performance-test.sh -s single
# 결과: P95 = 30ms, 쿼리 2개

# 4. 개선 확인
#  70% 응답시간 개선
#  60% 쿼리 감소
```

---

## 📈 성능 지표

### 목표 기준

| 지표       | 목표      | 현재    | 상태 |
|----------|---------|-------|----|
| P95 응답시간 | < 50ms  | 45ms  |    |
| P99 응답시간 | < 150ms | 120ms |    |
| 쿼리/요청    | < 3     | 2.5   |    |
| 동시사용자    | > 200   | 200   |    |

### 결과 예시

```
=== 단건 조회 성능 측정 결과 ===
응답 시간:
  - p50: 15.32ms
  - p95: 45.67ms  ← 주요 지표
  - p99: 120.45ms
쿼리 실행:
  - 평균 쿼리 수: 2.50
  - 최대 쿼리 수: 5
```

---

## 주요 명령어

```bash
# 데이터 생성
./scripts/performance/setup-test-data.sh -c 600000

# 개별 시나리오 테스트
./scripts/performance/run-performance-test.sh -s single
./scripts/performance/run-performance-test.sh -s pagination
./scripts/performance/run-performance-test.sh -s complex

# 모든 테스트 실행
./scripts/performance/run-performance-test.sh -s all

# 이전 결과와 비교
./scripts/performance/run-performance-test.sh -s single -c baseline.json

# 데이터 정리
./gradlew test --tests PerformanceDataGeneratorTest.cleanupPerformanceTestData
```

---

## 프로젝트 구조

```
Article-server/
├── scripts/performance/
│   ├── setup-test-data.sh      # 테스트 데이터 생성
│   └── run-performance-test.sh  # 성능 테스트 실행
├── src/test/java/.../performance/
│   ├── data/
│   │   └── PerformanceDataGenerator.java    # 60만건 데이터 생성
│   ├── measurement/
│   │   └── QueryPerformanceTest.java        # 조회 성능 테스트
│   ├── metrics/
│   │   ├── QueryMetricsCollector.java       # 메트릭 수집
│   │   └── PerformanceMetrics.java          # p95, p99 계산
│   └── comparison/
│       └── PerformanceComparator.java       # 전후 비교
└── performance-results/
    ├── performance-metrics-*.json           # 테스트 결과
    └── performance-report-*.md              # 리포트
```

---

## FAQ

### Q: 메서드를 변경했는데 테스트가 계속 작동하나요?

**A:** 네! `ArticleReadService`의 public 메서드만 호출하므로, 내부 구현은 자유롭게 변경 가능합니다.

### Q: 테스트 데이터를 매번 생성해야 하나요?

**A:** 아니요. 한 번 생성하면 계속 사용 가능합니다.

### Q: 실제 프로덕션과 동일한가요?

**A:** 데이터 규모(60만 건)와 쿼리 패턴은 유사하지만, 네트워크 지연이나 실제 부하는 다를 수 있습니다.

---

## 상세 문서

- [성능 테스트 가이드](./docs/PERFORMANCE-TEST-GUIDE.md) - 상세 사용법
- [아키텍처 결정 기록](./docs/adr/ADR-006-performance-test-framework.md) - 설계 결정 근거
- [성능 테스트 설계](./docs/performance-test-architecture.md) - 기술 구조

---

## 다음 단계

1. **베이스라인 설정**: 현재 성능 측정 후 목표 설정
2. **최적화 진행**: 병목 지점 개선
3. **CI/CD 통합**: PR마다 자동 성능 테스트
4. **모니터링**: 프로덕션 메트릭과 비교

---

## Tips

- 테스트 전 항상 `./gradlew clean build`
- 캐시 초기화가 필요하면 Docker 재시작
- 결과는 `performance-results/` 디렉토리에 자동 저장
- JSON 결과를 Excel로 import하여 차트 생성 가능

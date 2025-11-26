# Performance Testing Suite

## 디렉토리 구조

```
performance-testing/
├── scripts/           # 데이터 생성 및 테스트 실행 스크립트
│   ├── generate-600k-data.sql
│   ├── generate-complete-600k-data.sql
│   ├── quick-100k-insert.sql
│   ├── setup-test-data.sh
│   └── run-performance-test.sh
├── tests/            # 성능 테스트 Java 코드
│   └── (테스트 클래스 링크)
├── results/          # 테스트 결과 및 로그
│   └── performance-test.log
├── configs/          # 테스트 설정 파일
│   └── application-performance-test.yml
└── docs/             # 문서 및 ADR
    └── ADR-006-performance-test-framework.md
```

## 실행 방법

### 1. 데이터 생성 (60만건)

```bash
cd performance-testing/scripts
./setup-test-data.sh -c 600000
```

### 2. 성능 테스트 실행

```bash
cd performance-testing/scripts
./run-performance-test.sh
```

### 3. 결과 확인

```bash
cat performance-testing/results/performance-test.log
```

## 테스트 시나리오

1. **필터 조합 테스트**: 모든 가능한 필터 조합 성능 측정
2. **텍스트 검색 테스트**: 제목/내용 검색 성능 측정
3. **배치 조회 테스트**: 대량 데이터 조회 성능 측정
4. **인덱스 위치별 테스트**: 데이터 위치에 따른 성능 차이 분석

## 주요 메트릭

- **P50, P95, P99 Latency**: 응답 시간 백분위수
- **Query Count**: 실행된 쿼리 수
- **Cache Hit Ratio**: 캐시 적중률
- **N+1 Detection**: N+1 쿼리 문제 탐지

## 참고

- Java 테스트 클래스는 `src/test/java/com/teambind/articleserver/performance/` 디렉토리 참조
- 상세 설정은 `configs/application-performance-test.yml` 참조

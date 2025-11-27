#!/bin/bash

# 프로덕션 성능 테스트 실행 스크립트
# 600K 데이터 및 다양한 동시성 시나리오 지원

set -e

echo "========================================"
echo "Production Performance Test Runner"
echo "========================================"
echo ""

# 기본값 설정
DATA_SIZE=${1:-100000}
CONCURRENT_USERS=${2:-100}
WARMUP_SIZE=${3:-100}
ITERATIONS=${4:-1000}

echo "Configuration:"
echo "  Data Size: $DATA_SIZE articles"
echo "  Concurrent Users: $CONCURRENT_USERS"
echo "  Warmup Size: $WARMUP_SIZE"
echo "  Iterations: $ITERATIONS"
echo ""

# 결과 디렉터리 생성
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_DIR="results/production_${DATA_SIZE}_${TIMESTAMP}"
mkdir -p "$RESULT_DIR"

echo "Results will be saved to: $RESULT_DIR"
echo ""

# 데이터베이스 준비 확인
echo "Checking database status..."
DB_COUNT=$(mysql -h localhost -P 23306 -u root -ppassword article_test -e "SELECT COUNT(*) FROM articles;" -s -N 2>/dev/null || echo "0")
echo "  Current article count: $DB_COUNT"

if [ "$DB_COUNT" -lt "$DATA_SIZE" ]; then
    echo ""
    echo "WARNING: Insufficient data! Need $DATA_SIZE articles but only have $DB_COUNT"
    echo "   Generating additional test data..."

    # 데이터 생성 스크립트 실행
    ./generate-data.sh "$DATA_SIZE"
fi

echo ""
echo "Running Production Performance Test..."
echo "========================================"

# JVM 옵션 설정 (600K 처리를 위한 메모리 설정)
export JAVA_OPTS="-Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 테스트 실행
cd ..
./gradlew test \
    --tests "ProductionPerformanceTest" \
    -Dtest.data.size="$DATA_SIZE" \
    -Dtest.concurrent.users="$CONCURRENT_USERS" \
    -Dtest.warmup.size="$WARMUP_SIZE" \
    -Dtest.measure.iterations="$ITERATIONS" \
    -Dspring.profiles.active=performance-test \
    --info 2>&1 | tee "performance-testing/$RESULT_DIR/test_output.log"

TEST_RESULT=$?

echo ""
echo "========================================"

if [ $TEST_RESULT -eq 0 ]; then
    echo "Test completed successfully!"
    echo ""
    echo "Key Metrics Summary:"
    grep -E "P50:|P95:|P99:|TPS:|N\+1 Problem:" "performance-testing/$RESULT_DIR/test_output.log" | tail -20
else
    echo "Test failed with exit code: $TEST_RESULT"
    echo "   Check the log file for details: $RESULT_DIR/test_output.log"
fi

echo ""
echo "Full results available in: performance-testing/$RESULT_DIR"
echo ""

# 여러 시나리오 실행 옵션
if [ "$5" == "all" ]; then
    echo "Running additional scenarios..."
    echo ""

    # 시나리오 1: 10만건 기본
    echo "Scenario 1: 100K articles, 100 users"
    ./run-production-test.sh 100000 100

    # 시나리오 2: 60만건 기본
    echo "Scenario 2: 600K articles, 100 users"
    ./run-production-test.sh 600000 100

    # 시나리오 3: 60만건 고부하
    echo "Scenario 3: 600K articles, 500 users"
    ./run-production-test.sh 600000 500

    echo ""
    echo "All scenarios completed!"
fi

# N+1 문제 개선 전후 비교
if [ "$5" == "compare" ]; then
    echo "Running before/after comparison..."
    echo ""

    # 개선 전 테스트 (lazy loading)
    echo "Testing BEFORE optimization (Lazy Loading)..."
    export SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_BATCH_FETCH_SIZE=1
    ./run-production-test.sh "$DATA_SIZE" "$CONCURRENT_USERS" "$WARMUP_SIZE" "$ITERATIONS"
    mv "$RESULT_DIR" "${RESULT_DIR}_before"

    # 개선 후 테스트 (batch fetch + entity graph)
    echo "Testing AFTER optimization (Batch Fetch + Entity Graph)..."
    export SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_BATCH_FETCH_SIZE=100
    ./run-production-test.sh "$DATA_SIZE" "$CONCURRENT_USERS" "$WARMUP_SIZE" "$ITERATIONS"
    mv "$RESULT_DIR" "${RESULT_DIR}_after"

    echo ""
    echo "Comparison Results:"
    echo "  Before: ${RESULT_DIR}_before"
    echo "  After: ${RESULT_DIR}_after"

    # 간단한 비교 출력
    echo ""
    echo "P95 Latency Comparison:"
    echo -n "  Before: "
    grep "P95:" "${RESULT_DIR}_before/test_output.log" | head -1
    echo -n "  After:  "
    grep "P95:" "${RESULT_DIR}_after/test_output.log" | head -1
fi

exit $TEST_RESULT
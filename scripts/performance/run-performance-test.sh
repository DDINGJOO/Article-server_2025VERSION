#!/bin/bash

#######################################
# 성능 테스트 실행 스크립트
#
# 목적: 조회 성능 측정 및 리포트 생성
# 사용법: ./run-performance-test.sh [options]
#######################################

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 기본 설정
PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
RESULTS_DIR="$PROJECT_DIR/performance-results"
LOG_FILE="$RESULTS_DIR/performance-test-$(date +%Y%m%d-%H%M%S).log"
DOCKER_COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"

# 테스트 시나리오 선택
SCENARIO=""
ALL_SCENARIOS=false
COMPARE_MODE=false
BASELINE_FILE=""

# 함수: 사용법
usage() {
    cat << EOF
사용법: $0 [OPTIONS]

OPTIONS:
    -s, --scenario <name>    실행할 테스트 시나리오
                            single: 단건 조회 성능
                            pagination: 페이지네이션 성능
                            complex: 복잡한 검색 성능
                            concurrent: 동시 사용자 부하
                            cache: 캐시 효과 측정
                            all: 모든 시나리오 실행

    -c, --compare <file>    이전 결과와 비교
    -o, --output <format>   출력 형식 (json, csv, markdown, all)
    -v, --verbose          상세 로그 출력
    -h, --help             도움말

예제:
    $0 -s single                          # 단건 조회 테스트
    $0 -s all -o json                    # 모든 테스트 실행, JSON 출력
    $0 -s single -c baseline.json        # 기준선과 비교
EOF
}

# 함수: 로그
log() {
    local level=$1
    shift
    local message="$@"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} ${message}"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} ${message}"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} ${message}"
            ;;
        SUCCESS)
            echo -e "${CYAN}[SUCCESS]${NC} ${message}"
            ;;
        HEADER)
            echo -e "${PURPLE}=================${NC}"
            echo -e "${PURPLE}${message}${NC}"
            echo -e "${PURPLE}=================${NC}"
            ;;
    esac

    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
}

# 함수: 환경 체크
check_environment() {
    log HEADER "환경 체크"

    # Java 체크
    if ! command -v java &> /dev/null; then
        log ERROR "Java가 설치되지 않았습니다."
        exit 1
    fi

    local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    log INFO "Java 버전: $java_version"

    # Gradle 체크
    if [ ! -f "$PROJECT_DIR/gradlew" ]; then
        log ERROR "Gradle wrapper를 찾을 수 없습니다."
        exit 1
    fi
    log INFO "Gradle wrapper 확인 완료"

    # Docker 체크
    if ! docker compose version &> /dev/null; then
        log ERROR "Docker Compose가 설치되지 않았습니다."
        exit 1
    fi
    log INFO "Docker Compose 확인 완료"

    # DB 연결 체크
    log INFO "MariaDB 연결 확인 중..."
    if docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb \
        mysqladmin ping -h localhost -u root -particlepass123 &> /dev/null; then
        log SUCCESS "MariaDB 연결 성공"
    else
        log WARN "MariaDB가 실행되지 않음. 시작 중..."
        docker compose -f "$DOCKER_COMPOSE_FILE" up -d article-mariadb article-redis
        sleep 10
    fi

    # 데이터 존재 여부 확인
    local article_count=$(docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb \
        mysql -u root -particlepass123 article_db -sN -e "SELECT COUNT(*) FROM articles;" 2>/dev/null || echo "0")

    log INFO "현재 게시글 수: $article_count"

    if [ "$article_count" -lt 100000 ]; then
        log WARN "테스트 데이터가 부족합니다. (현재: $article_count)"
        log INFO "데이터 생성이 필요하면: ./scripts/performance/setup-test-data.sh"
    fi
}

# 함수: 현재 프로젝트 성능 측정
run_performance_test() {
    log HEADER "현재 프로젝트 성능 측정"

    cd "$PROJECT_DIR"

    # 테스트 실행
    log INFO "성능 테스트 시작 (Epochs: 5, Iterations: 200)"

    ./gradlew test \
        --tests "com.teambind.articleserver.performance.measurement.QueryPerformanceTest.measureCurrentPerformance" \
        -Dspring.profiles.active=performance-test \
        --info 2>&1 | tee -a "$LOG_FILE"

    if [ ${PIPESTATUS[0]} -eq 0 ]; then
        log SUCCESS "성능 측정 완료"
    else
        log ERROR "성능 측정 실패"
        return 1
    fi
}

# 함수: 모든 시나리오 실행
run_all_scenarios() {
    log HEADER "현재 프로젝트 전체 성능 테스트 실행"

    # 단일 통합 테스트로 모든 시나리오 실행
    run_performance_test
}

# 함수: 결과 수집 및 리포트
generate_report() {
    log HEADER "성능 테스트 리포트 생성"

    # 최신 결과 파일 찾기
    local latest_results=$(find "$RESULTS_DIR" -name "performance-metrics-*.json" -type f -exec ls -t {} + | head -n 5)

    if [ -z "$latest_results" ]; then
        log WARN "결과 파일을 찾을 수 없습니다."
        return
    fi

    # 통합 리포트 생성
    local report_file="$RESULTS_DIR/performance-report-$(date +%Y%m%d-%H%M%S).md"

    cat > "$report_file" << EOF
# Performance Test Report
Generated: $(date)

## Environment
- Articles: $(docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb mysql -u root -particlepass123 article_db -sN -e "SELECT COUNT(*) FROM articles;")
- Images: $(docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb mysql -u root -particlepass123 article_db -sN -e "SELECT COUNT(*) FROM article_images;")
- Keywords: $(docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb mysql -u root -particlepass123 article_db -sN -e "SELECT COUNT(*) FROM keyword_mapping_table;")

## Test Results

EOF

    # 각 결과 파일 처리
    for result_file in $latest_results; do
        if [ -f "$result_file" ]; then
            local scenario=$(basename "$result_file" | sed 's/performance-metrics-//' | sed 's/-[0-9]*.json//')
            echo "### $scenario" >> "$report_file"

            # JSON에서 주요 메트릭 추출
            if command -v jq &> /dev/null; then
                local p50=$(jq '.response_time_ms.p50' "$result_file")
                local p95=$(jq '.response_time_ms.p95' "$result_file")
                local p99=$(jq '.response_time_ms.p99' "$result_file")
                local avg_queries=$(jq '.query_metrics.averageCount' "$result_file")

                cat >> "$report_file" << EOF

| Metric | Value |
|--------|-------|
| P50 Response Time | ${p50} ms |
| P95 Response Time | ${p95} ms |
| P99 Response Time | ${p99} ms |
| Avg Queries/Request | ${avg_queries} |

EOF
            fi
        fi
    done

    log SUCCESS "리포트 생성 완료: $report_file"

    # 리포트 출력
    echo ""
    cat "$report_file"
}

# 함수: 성능 비교
compare_results() {
    local baseline=$1
    local current=$2

    log HEADER "성능 비교 분석"

    if [ ! -f "$baseline" ]; then
        log ERROR "기준 파일을 찾을 수 없습니다: $baseline"
        return 1
    fi

    # jq가 설치되어 있는지 확인
    if ! command -v jq &> /dev/null; then
        log WARN "jq가 설치되지 않아 비교 분석을 수행할 수 없습니다."
        log INFO "설치: brew install jq (macOS) 또는 apt-get install jq (Linux)"
        return 1
    fi

    # 비교 스크립트 실행
    local comparison_script="$PROJECT_DIR/scripts/performance/compare-results.py"

    if [ -f "$comparison_script" ]; then
        python3 "$comparison_script" "$baseline" "$current"
    else
        # 간단한 비교 수행
        log INFO "=== 응답 시간 비교 ==="

        local baseline_p95=$(jq '.response_time_ms.p95' "$baseline")
        local current_p95=$(jq '.response_time_ms.p95' "$current")

        local improvement=$(echo "scale=2; (($baseline_p95 - $current_p95) / $baseline_p95) * 100" | bc)

        log INFO "기준 P95: ${baseline_p95}ms"
        log INFO "현재 P95: ${current_p95}ms"

        if (( $(echo "$improvement > 0" | bc -l) )); then
            log SUCCESS "개선율: ${improvement}%"
        else
            log WARN "성능 저하: ${improvement}%"
        fi
    fi
}

# 메인 함수
main() {
    # 결과 디렉토리 생성
    mkdir -p "$RESULTS_DIR"

    # 파라미터 파싱
    OUTPUT_FORMAT="all"
    VERBOSE=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            -s|--scenario)
                SCENARIO="$2"
                shift 2
                ;;
            -c|--compare)
                COMPARE_MODE=true
                BASELINE_FILE="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_FORMAT="$2"
                shift 2
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                log ERROR "알 수 없는 옵션: $1"
                usage
                exit 1
                ;;
        esac
    done

    # 시나리오가 지정되지 않은 경우
    if [ -z "$SCENARIO" ]; then
        log ERROR "테스트 시나리오를 지정해주세요 (-s 옵션)"
        usage
        exit 1
    fi

    # 환경 체크
    check_environment

    # 테스트 시작 시간
    START_TIME=$(date +%s)

    # 테스트 실행
    run_performance_test

    # 종료 시간 및 소요 시간
    END_TIME=$(date +%s)
    ELAPSED=$((END_TIME - START_TIME))

    log INFO "총 소요 시간: $(($ELAPSED / 60))분 $(($ELAPSED % 60))초"

    # 리포트 생성
    generate_report

    # 비교 모드
    if [ "$COMPARE_MODE" = true ] && [ -n "$BASELINE_FILE" ]; then
        latest_result=$(find "$RESULTS_DIR" -name "performance-metrics-*.json" -type f -exec ls -t {} + | head -n 1)
        if [ -n "$latest_result" ]; then
            compare_results "$BASELINE_FILE" "$latest_result"
        fi
    fi

    log SUCCESS "성능 테스트 완료"
}

# 스크립트 실행
main "$@"
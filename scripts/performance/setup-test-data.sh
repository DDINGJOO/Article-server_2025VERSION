#!/bin/bash

#######################################
# 성능 테스트 데이터 셋업 스크립트
#
# 목적: 60만건 게시글 + 180만 이미지 + 240만 키워드 생성
# 사용법: ./setup-test-data.sh [options]
#######################################

set -e  # 에러 발생시 즉시 종료

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 기본 설정
ARTICLE_COUNT=600000
PROFILE="performance-test"
DOCKER_COMPOSE_FILE="docker-compose.yml"
PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
LOG_FILE="$PROJECT_DIR/logs/performance-test-setup-$(date +%Y%m%d-%H%M%S).log"

# 함수: 사용법 출력
usage() {
    cat << EOF
사용법: $0 [OPTIONS]

OPTIONS:
    -c, --count <number>     생성할 게시글 수 (기본값: 600000)
    -p, --profile <name>     Spring Profile (기본값: performance-test)
    -d, --docker <file>      Docker Compose 파일 경로
    -r, --reset              기존 데이터 초기화 후 생성
    -v, --verify             데이터 생성 후 검증
    -h, --help               도움말 출력

예제:
    $0                       # 기본 설정으로 60만건 생성
    $0 -c 100000 -v         # 10만건 생성 후 검증
    $0 -r -c 600000         # 초기화 후 60만건 생성
EOF
}

# 함수: 로그 출력
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
        *)
            echo "${message}"
            ;;
    esac

    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
}

# 함수: Docker 환경 확인
check_docker() {
    log INFO "Docker 환경 확인 중..."

    if ! command -v docker &> /dev/null; then
        log ERROR "Docker가 설치되지 않았습니다."
        exit 1
    fi

    if ! docker compose version &> /dev/null; then
        log ERROR "Docker Compose가 설치되지 않았습니다."
        exit 1
    fi

    log INFO "Docker 환경 확인 완료"
}

# 함수: Docker Compose 서비스 시작
start_services() {
    log INFO "Docker Compose 서비스 시작 중..."

    cd "$PROJECT_DIR"

    # MariaDB, Redis만 시작 (Kafka는 필요시)
    docker compose -f "$DOCKER_COMPOSE_FILE" up -d article-mariadb article-redis

    # MariaDB 준비 대기
    log INFO "MariaDB 준비 대기 중..."
    local max_attempts=30
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb \
            mysqladmin ping -h localhost -u root -particlepass123 &> /dev/null; then
            log INFO "MariaDB 준비 완료"
            break
        fi

        attempt=$((attempt + 1))
        log INFO "MariaDB 대기 중... ($attempt/$max_attempts)"
        sleep 2
    done

    if [ $attempt -eq $max_attempts ]; then
        log ERROR "MariaDB 시작 실패"
        exit 1
    fi
}

# 함수: 데이터베이스 초기화
reset_database() {
    log WARN "데이터베이스 초기화 중..."

    docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb mysql \
        -u root -particlepass123 article_db << EOF
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE keyword_mapping_table;
TRUNCATE TABLE article_images;
TRUNCATE TABLE articles;
TRUNCATE TABLE keywords;
TRUNCATE TABLE boards;
SET FOREIGN_KEY_CHECKS = 1;
EOF

    log INFO "데이터베이스 초기화 완료"
}

# 함수: Spring Boot 애플리케이션으로 데이터 생성
generate_data() {
    log INFO "데이터 생성 시작 (게시글: $ARTICLE_COUNT 건)"
    log INFO "예상 데이터:"
    log INFO "  - 이미지: $(($ARTICLE_COUNT * 3)) 개"
    log INFO "  - 키워드 매핑: $(($ARTICLE_COUNT * 4)) 개"

    cd "$PROJECT_DIR"

    # Gradle로 테스트 실행
    ./gradlew test \
        --tests "com.teambind.articleserver.performance.PerformanceDataGeneratorTest" \
        -Dspring.profiles.active="$PROFILE" \
        -Dperformance.test.article-count="$ARTICLE_COUNT" \
        -Dperformance.test.batch-size=5000 \
        -Dperformance.test.parallel-threads=8 \
        --info

    if [ $? -eq 0 ]; then
        log INFO "데이터 생성 완료"
    else
        log ERROR "데이터 생성 실패"
        exit 1
    fi
}

# 함수: 데이터 검증
verify_data() {
    log INFO "데이터 검증 중..."

    # SQL 쿼리로 데이터 수 확인
    local article_count=$(docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb \
        mysql -u root -particlepass123 article_db -sN -e "SELECT COUNT(*) FROM articles;")

    local image_count=$(docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb \
        mysql -u root -particlepass123 article_db -sN -e "SELECT COUNT(*) FROM article_images;")

    local keyword_count=$(docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb \
        mysql -u root -particlepass123 article_db -sN -e "SELECT COUNT(*) FROM keyword_mapping_table;")

    log INFO "=== 데이터 검증 결과 ==="
    log INFO "게시글: $article_count 건"
    log INFO "이미지: $image_count 개"
    log INFO "키워드 매핑: $keyword_count 개"

    # 목표값과 비교
    local expected_images=$(($ARTICLE_COUNT * 3))
    local expected_keywords=$(($ARTICLE_COUNT * 4))

    if [ "$article_count" -ge "$ARTICLE_COUNT" ]; then
        log INFO "✓ 게시글 수 정상"
    else
        log WARN "✗ 게시글 수 부족 (목표: $ARTICLE_COUNT, 실제: $article_count)"
    fi

    if [ "$image_count" -ge "$expected_images" ]; then
        log INFO "✓ 이미지 수 정상"
    else
        log WARN "✗ 이미지 수 부족 (목표: $expected_images, 실제: $image_count)"
    fi

    if [ "$keyword_count" -ge "$expected_keywords" ]; then
        log INFO "✓ 키워드 매핑 수 정상"
    else
        log WARN "✗ 키워드 매핑 수 부족 (목표: $expected_keywords, 실제: $keyword_count)"
    fi
}

# 함수: 성능 통계 출력
show_statistics() {
    log INFO "=== 데이터베이스 통계 ==="

    docker compose -f "$DOCKER_COMPOSE_FILE" exec -T article-mariadb \
        mysql -u root -particlepass123 article_db << EOF
-- 테이블별 데이터 크기
SELECT
    table_name AS 'Table',
    table_rows AS 'Rows',
    ROUND(data_length / 1024 / 1024, 2) AS 'Data (MB)',
    ROUND(index_length / 1024 / 1024, 2) AS 'Index (MB)',
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS 'Total (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'article_db'
    AND table_name IN ('articles', 'article_images', 'keyword_mapping_table')
ORDER BY table_rows DESC;

-- Article 타입별 분포
SELECT
    article_type AS 'Type',
    COUNT(*) AS 'Count',
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM articles), 2) AS 'Percentage'
FROM articles
GROUP BY article_type;
EOF
}

# 메인 스크립트
main() {
    # 로그 디렉토리 생성
    mkdir -p "$(dirname "$LOG_FILE")"

    log INFO "성능 테스트 데이터 셋업 시작"
    log INFO "로그 파일: $LOG_FILE"

    # 파라미터 파싱
    RESET=false
    VERIFY=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            -c|--count)
                ARTICLE_COUNT="$2"
                shift 2
                ;;
            -p|--profile)
                PROFILE="$2"
                shift 2
                ;;
            -d|--docker)
                DOCKER_COMPOSE_FILE="$2"
                shift 2
                ;;
            -r|--reset)
                RESET=true
                shift
                ;;
            -v|--verify)
                VERIFY=true
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

    # 실행 단계
    check_docker
    start_services

    if [ "$RESET" = true ]; then
        reset_database
    fi

    # 시작 시간 기록
    START_TIME=$(date +%s)

    generate_data

    # 종료 시간 및 소요 시간 계산
    END_TIME=$(date +%s)
    ELAPSED=$((END_TIME - START_TIME))

    log INFO "총 소요 시간: $(($ELAPSED / 60))분 $(($ELAPSED % 60))초"

    if [ "$VERIFY" = true ]; then
        verify_data
        show_statistics
    fi

    log INFO "성능 테스트 데이터 셋업 완료"
}

# 스크립트 실행
main "$@"
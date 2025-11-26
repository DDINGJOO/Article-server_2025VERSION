#!/bin/bash
# 대량 더미 데이터 로드 스크립트

echo "========================================="
echo "Article Server Dummy Data Loader"
echo "========================================="
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 옵션 파싱
BATCH_SIZE=1000
BATCH_COUNT=600
QUICK_MODE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --quick)
            QUICK_MODE=true
            BATCH_COUNT=10  # Quick 모드에서는 1만개만 생성
            shift
            ;;
        --batch-size)
            BATCH_SIZE="$2"
            shift 2
            ;;
        --batch-count)
            BATCH_COUNT="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --quick           Quick mode (generates 10,000 articles instead of 600,000)"
            echo "  --batch-size N    Number of records per batch (default: 1000)"
            echo "  --batch-count N   Number of batches (default: 600)"
            echo "  --help           Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# 계산
TOTAL_RECORDS=$((BATCH_SIZE * BATCH_COUNT))

# Docker 컨테이너 확인
echo -e "${YELLOW}Checking MariaDB container...${NC}"
if ! docker ps | grep -q "article-mariadb-dev"; then
    echo -e "${RED}Error: MariaDB container is not running!${NC}"
    echo "Please start the Docker environment first: ./docker-start.sh"
    exit 1
fi

# 현재 데이터 확인
echo -e "${YELLOW}Checking current data...${NC}"
CURRENT_COUNT=$(docker exec article-mariadb-dev mariadb -u root -particlepass123 -N -e "SELECT COUNT(*) FROM article_db.articles;" 2>/dev/null)
echo -e "${BLUE}Current articles in database: ${CURRENT_COUNT}${NC}"

# 사용자 확인
echo ""
echo -e "${YELLOW}About to generate:${NC}"
echo "  - Total records: $TOTAL_RECORDS"
echo "  - Batch size: $BATCH_SIZE"
echo "  - Number of batches: $BATCH_COUNT"

if [ "$QUICK_MODE" = true ]; then
    echo -e "${GREEN}  - Mode: QUICK (for testing)${NC}"
else
    echo -e "${YELLOW}  - Mode: FULL (this may take 30-60 minutes)${NC}"
fi

echo ""
echo -e "${YELLOW}Warning: This will add $TOTAL_RECORDS dummy articles to your database.${NC}"
echo -e "${YELLOW}Do you want to continue? (y/N)${NC}"
read -r response

if [[ ! "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    echo -e "${RED}Operation cancelled.${NC}"
    exit 0
fi

# 시작 시간 기록
START_TIME=$(date +%s)
echo ""
echo -e "${GREEN}Starting data generation at $(date)${NC}"
echo "========================================="

# SQL 생성 및 실행
echo -e "${YELLOW}Creating data generation procedures...${NC}"

# 프로시저만 생성 (데이터 생성 제외)
docker exec -i article-mariadb-dev mariadb -u root -particlepass123 article_db << 'EOF'
-- 프로시저 생성을 위한 구분자 변경
DELIMITER $$

-- 랜덤 텍스트 생성 함수
DROP FUNCTION IF EXISTS generate_random_text$$
CREATE FUNCTION generate_random_text(min_length INT, max_length INT)
RETURNS TEXT
DETERMINISTIC
BEGIN
    DECLARE result TEXT DEFAULT '';
    DECLARE chars VARCHAR(255) DEFAULT 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ';
    DECLARE text_length INT;
    DECLARE i INT DEFAULT 0;

    SET text_length = FLOOR(min_length + RAND() * (max_length - min_length));

    WHILE i < text_length DO
        SET result = CONCAT(result, SUBSTRING(chars, FLOOR(1 + RAND() * 63), 1));
        SET i = i + 1;
    END WHILE;

    RETURN result;
END$$

-- 랜덤 한글 텍스트 생성 함수
DROP FUNCTION IF EXISTS generate_korean_text$$
CREATE FUNCTION generate_korean_text(word_count INT)
RETURNS TEXT
DETERMINISTIC
BEGIN
    DECLARE result TEXT DEFAULT '';
    DECLARE korean_words TEXT DEFAULT '안녕하세요,감사합니다,개발자,프로그래밍,스프링부트,자바,데이터베이스,테스트,성능,최적화,알고리즘,시스템,아키텍처,마이크로서비스,도커,쿠버네티스,레디스,카프카,메시지,이벤트,트랜잭션,쿼리,인덱스,캐시,서버,클라이언트,프론트엔드,백엔드,풀스택,모니터링';
    DECLARE words_array TEXT;
    DECLARE word_index INT;
    DECLARE i INT DEFAULT 0;

    WHILE i < word_count DO
        SET word_index = FLOOR(1 + RAND() * 30);
        SET words_array = SUBSTRING_INDEX(SUBSTRING_INDEX(korean_words, ',', word_index), ',', -1);
        SET result = CONCAT(result, words_array, ' ');
        SET i = i + 1;
    END WHILE;

    RETURN TRIM(result);
END$$

-- 더미 게시글 생성 프로시저
DROP PROCEDURE IF EXISTS generate_dummy_articles$$
CREATE PROCEDURE generate_dummy_articles(IN batch_size INT, IN batch_count INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE j INT DEFAULT 0;
    DECLARE article_id_val VARCHAR(50);
    DECLARE article_type_val VARCHAR(20);
    DECLARE title_val VARCHAR(200);
    DECLARE contents_val TEXT;
    DECLARE writer_id_val VARCHAR(50);
    DECLARE board_id_val BIGINT;
    DECLARE status_val VARCHAR(20);
    DECLARE view_count_val BIGINT;
    DECLARE created_date DATETIME;
    DECLARE event_start DATETIME;
    DECLARE event_end DATETIME;
    DECLARE batch_start_time DATETIME;
    DECLARE total_inserted INT DEFAULT 0;
    DECLARE timestamp_prefix VARCHAR(20);

    -- 성능 최적화 설정
    SET SESSION bulk_insert_buffer_size = 256 * 1024 * 1024;
    SET SESSION unique_checks = 0;
    SET SESSION foreign_key_checks = 0;

    -- 현재 시간을 기준으로 고유 ID 생성
    SET timestamp_prefix = DATE_FORMAT(NOW(), '%Y%m%d%H%i%s');

    WHILE i < batch_count DO
        SET batch_start_time = NOW();
        SET j = 0;

        START TRANSACTION;

        WHILE j < batch_size DO
            -- Article ID 생성 (고유성 보장)
            SET article_id_val = CONCAT(
                CASE FLOOR(RAND() * 3)
                    WHEN 0 THEN 'ART'
                    WHEN 1 THEN 'NTC'
                    ELSE 'EVT'
                END,
                timestamp_prefix,
                LPAD(i * batch_size + j, 8, '0')
            );

            -- Article Type 결정
            SET article_type_val = CASE
                WHEN RAND() < 0.7 THEN 'REGULAR'
                WHEN RAND() < 0.9 THEN 'NOTICE'
                ELSE 'EVENT'
            END;

            -- 제목 생성
            SET title_val = CONCAT(
                generate_korean_text(3 + FLOOR(RAND() * 5)),
                ' - Test Article #',
                i * batch_size + j
            );

            -- 내용 생성
            SET contents_val = CONCAT(
                '## ', generate_korean_text(5), '\n\n',
                generate_korean_text(50 + FLOOR(RAND() * 100)), '\n\n',
                '### Details\n',
                generate_random_text(200, 500), '\n\n',
                '- ', generate_korean_text(10), '\n',
                '- ', generate_korean_text(10), '\n',
                '- ', generate_korean_text(10), '\n\n',
                generate_korean_text(100 + FLOOR(RAND() * 200))
            );

            -- Writer ID 생성
            SET writer_id_val = CONCAT(
                CASE FLOOR(RAND() * 5)
                    WHEN 0 THEN 'user'
                    WHEN 1 THEN 'dev'
                    WHEN 2 THEN 'admin'
                    WHEN 3 THEN 'manager'
                    ELSE 'member'
                END,
                '_',
                FLOOR(RAND() * 10000)
            );

            -- Board ID
            SET board_id_val = CASE
                WHEN RAND() < 0.4 THEN 1
                WHEN RAND() < 0.6 THEN 5
                WHEN RAND() < 0.8 THEN 2
                WHEN RAND() < 0.9 THEN 3
                ELSE 4
            END;

            -- Status
            SET status_val = CASE
                WHEN RAND() < 0.95 THEN 'ACTIVE'
                WHEN RAND() < 0.98 THEN 'DELETED'
                ELSE 'BLOCKED'
            END;

            -- View Count
            SET view_count_val = FLOOR(EXP(RAND() * LOG(100000)));

            -- Created Date
            SET created_date = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY);

            -- Event dates
            IF article_type_val = 'EVENT' THEN
                SET event_start = DATE_ADD(created_date, INTERVAL FLOOR(RAND() * 7) DAY);
                SET event_end = DATE_ADD(event_start, INTERVAL 7 + FLOOR(RAND() * 30) DAY);
            ELSE
                SET event_start = NULL;
                SET event_end = NULL;
            END IF;

            -- 데이터 삽입
            INSERT INTO articles (
                article_id, article_type, title, contents,
                writer_id, board_id, status, view_count,
                event_start_date, event_end_date,
                created_at, updated_at
            ) VALUES (
                article_id_val, article_type_val, title_val, contents_val,
                writer_id_val, board_id_val, status_val, view_count_val,
                event_start, event_end,
                created_date, created_date
            ) ON DUPLICATE KEY UPDATE article_id = article_id;

            SET j = j + 1;
            SET total_inserted = total_inserted + 1;
        END WHILE;

        COMMIT;

        -- 진행 상황 로그
        SELECT CONCAT('Batch ', i + 1, '/', batch_count, ' completed. Total inserted: ', total_inserted,
                     ' (Time: ', TIMESTAMPDIFF(SECOND, batch_start_time, NOW()), ' seconds)') AS progress;

        SET i = i + 1;

        -- 배치 간 짧은 대기
        DO SLEEP(0.1);
    END WHILE;

    -- 설정 복구
    SET SESSION unique_checks = 1;
    SET SESSION foreign_key_checks = 1;

    SELECT CONCAT('Successfully generated ', total_inserted, ' dummy articles') AS result;
END$$

DELIMITER ;

SELECT 'Procedures created successfully' AS status;
EOF

# 데이터 생성 실행
echo ""
echo -e "${GREEN}Generating dummy articles...${NC}"
echo -e "${YELLOW}This may take a while. Progress will be shown below:${NC}"
echo ""

docker exec article-mariadb-dev mariadb -u root -particlepass123 -e \
    "CALL article_db.generate_dummy_articles($BATCH_SIZE, $BATCH_COUNT);" 2>&1 | \
    while IFS= read -r line; do
        if [[ "$line" == *"Batch"* ]]; then
            echo -e "${GREEN}$line${NC}"
        elif [[ "$line" == *"Successfully"* ]]; then
            echo -e "${GREEN}✓ $line${NC}"
        else
            echo "$line"
        fi
    done

# 키워드 매핑 생성
echo ""
echo -e "${YELLOW}Generating keyword mappings...${NC}"
docker exec article-mariadb-dev mariadb -u root -particlepass123 -e "
    USE article_db;
    INSERT IGNORE INTO keyword_mappings (article_id, keyword_id)
    SELECT
        a.article_id,
        1 + FLOOR(RAND() * 10) as keyword_id
    FROM (
        SELECT article_id FROM articles
        WHERE article_id NOT IN (SELECT DISTINCT article_id FROM keyword_mappings)
        ORDER BY RAND()
        LIMIT 100000
    ) a
    CROSS JOIN (
        SELECT 1 as n UNION SELECT 2 UNION SELECT 3
    ) numbers
    WHERE RAND() < 0.5;
" 2>/dev/null

echo -e "${GREEN}✓ Keyword mappings generated${NC}"

# 이미지 매핑 생성 (일부만)
echo ""
echo -e "${YELLOW}Generating image mappings (30% of articles)...${NC}"
docker exec article-mariadb-dev mariadb -u root -particlepass123 -e "
    USE article_db;
    INSERT IGNORE INTO article_images (article_id, image_url, display_order)
    SELECT
        article_id,
        CONCAT('https://picsum.photos/800/600?random=', UUID()) as image_url,
        1 as display_order
    FROM articles
    WHERE article_id NOT IN (SELECT DISTINCT article_id FROM article_images)
    AND RAND() < 0.3
    LIMIT 50000;
" 2>/dev/null

echo -e "${GREEN}✓ Image mappings generated${NC}"

# 통계 업데이트
echo ""
echo -e "${YELLOW}Updating statistics...${NC}"
docker exec article-mariadb-dev mariadb -u root -particlepass123 -e "
    USE article_db;
    ANALYZE TABLE articles;
    ANALYZE TABLE keyword_mappings;
    ANALYZE TABLE article_images;
" 2>/dev/null

# 종료 시간 및 통계
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
MINUTES=$((DURATION / 60))
SECONDS=$((DURATION % 60))

echo ""
echo "========================================="
echo -e "${GREEN}Data Generation Complete!${NC}"
echo "========================================="
echo -e "${BLUE}Time taken: ${MINUTES} minutes ${SECONDS} seconds${NC}"
echo ""

# 최종 통계 출력
echo -e "${YELLOW}Final Database Statistics:${NC}"
docker exec article-mariadb-dev mariadb -u root -particlepass123 -N -e "
    USE article_db;
    SELECT CONCAT('Total Articles: ', FORMAT(COUNT(*), 0)) FROM articles
    UNION ALL
    SELECT CONCAT('Active Articles: ', FORMAT(COUNT(*), 0)) FROM articles WHERE status = 'ACTIVE'
    UNION ALL
    SELECT CONCAT('Total Views: ', FORMAT(SUM(view_count), 0)) FROM articles
    UNION ALL
    SELECT CONCAT('Average Views: ', FORMAT(AVG(view_count), 2)) FROM articles
    UNION ALL
    SELECT CONCAT('Keyword Mappings: ', FORMAT(COUNT(*), 0)) FROM keyword_mappings
    UNION ALL
    SELECT CONCAT('Article Images: ', FORMAT(COUNT(*), 0)) FROM article_images;
" 2>/dev/null | while IFS= read -r line; do
    echo -e "${GREEN}  $line${NC}"
done

echo ""
echo -e "${GREEN}You can now test the application with the generated data!${NC}"
echo -e "${YELLOW}Run: ./gradlew bootRun --args='--spring.profiles.active=dev'${NC}"
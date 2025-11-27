#!/bin/bash

# ==================================================
# Complete Performance Test Suite Script
# - Database reset
# - 600K data generation
# - ALL performance tests (including new tests)
# - Comprehensive report generation
# - Cleanup options
# ==================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_DIR="$SCRIPT_DIR/results/$TIMESTAMP"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Create result directory
mkdir -p "$RESULT_DIR"

echo -e "${BLUE}===================================================${NC}"
echo -e "${BLUE}       Complete Performance Test Suite${NC}"
echo -e "${BLUE}===================================================${NC}"
echo -e "${CYAN}Timestamp: $TIMESTAMP${NC}"
echo -e "${CYAN}Result Directory: $RESULT_DIR${NC}"
echo -e "${BLUE}===================================================${NC}"

# Step 1: Database Reset
echo -e "\n${YELLOW}[Step 1/6] Resetting Database...${NC}"
docker compose exec -T article-mariadb mysql -u root -particlepass123 article_db <<EOF
SET FOREIGN_KEY_CHECKS = 0;

-- Clean all tables
TRUNCATE TABLE keyword_mapping_table;
TRUNCATE TABLE article_images;
DELETE FROM articles;
DELETE FROM keywords;
DELETE FROM boards;

-- Reset auto_increment if needed
ALTER TABLE articles AUTO_INCREMENT = 1;
ALTER TABLE keywords AUTO_INCREMENT = 1;
ALTER TABLE boards AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Database reset complete' as status;
EOF

# Step 2: Generate 600K Complete Data
echo -e "\n${YELLOW}[Step 2/6] Generating 600K Complete Dataset...${NC}"
echo -e "${YELLOW}This may take 10-15 minutes...${NC}"

cat > "$RESULT_DIR/generate-600k.sql" <<'EOF'
SET FOREIGN_KEY_CHECKS = 0;
SET autocommit = 0;
SET unique_checks = 0;
SET SESSION sql_log_bin = 0;

-- Create boards (no value column, has board_name)
INSERT INTO boards (id, name, board_name, description, is_active, created_at)
VALUES
    (1, '공지사항', 'notice', '공지사항 게시판', true, NOW()),
    (2, '자유게시판', 'free', '자유게시판', true, NOW()),
    (3, '질문답변', 'qna', '질문답변 게시판', true, NOW()),
    (4, '기술블로그', 'tech', '기술블로그', true, NOW()),
    (5, '이벤트', 'event', '이벤트 게시판', true, NOW());

-- Create keywords with keyword_name and usage_count
INSERT INTO keywords (id, name, keyword_name, board_id, is_active, usage_count, description)
VALUES
    (1, 'Spring', 'spring', NULL, true, 0, 'Spring Framework'),
    (2, 'Java', 'java', NULL, true, 0, 'Java Programming'),
    (3, 'JPA', 'jpa', NULL, true, 0, 'Java Persistence API'),
    (4, 'MySQL', 'mysql', NULL, true, 0, 'MySQL Database'),
    (5, 'Redis', 'redis', NULL, true, 0, 'Redis Cache'),
    (6, 'Kafka', 'kafka', NULL, true, 0, 'Apache Kafka'),
    (7, 'Docker', 'docker', NULL, true, 0, 'Docker Container'),
    (8, 'K8s', 'k8s', NULL, true, 0, 'Kubernetes'),
    (9, 'AWS', 'aws', NULL, true, 0, 'Amazon Web Services'),
    (10, 'MSA', 'msa', NULL, true, 0, 'Microservice Architecture');

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_600k_complete$$
CREATE PROCEDURE generate_600k_complete()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_count INT DEFAULT 0;
    DECLARE current_article_id VARCHAR(50);

    SELECT 'Starting 600K data generation...' as status;

    WHILE i <= 600000 DO
        SET current_article_id = CONCAT('PERF', LPAD(i, 15, '0'));

        -- Insert article with value column (required, NOT NULL)
        INSERT INTO articles (
            article_id, article_type, title, contents, writer_id,
            board_id, status, first_image_url, view_count,
            created_at, updated_at, version, value
        ) VALUES (
            current_article_id,
            'REGULAR',  -- Using actual discriminator value
            CONCAT('Performance Test Article #', i,
                CASE MOD(i, 10)
                    WHEN 0 THEN ' - Spring Boot Optimization'
                    WHEN 1 THEN ' - JPA N+1 Problem'
                    WHEN 2 THEN ' - MSA Architecture'
                    WHEN 3 THEN ' - Domain Driven Design'
                    WHEN 4 THEN ' - Redis Caching Strategy'
                    WHEN 5 THEN ' - Kafka Event Streaming'
                    WHEN 6 THEN ' - Docker Containerization'
                    WHEN 7 THEN ' - Kubernetes Deployment'
                    WHEN 8 THEN ' - AWS Cloud Migration'
                    ELSE ' - Performance Tuning'
                END),
            CONCAT('Performance test content for article #', i, '. ',
                   'Lorem ipsum dolor sit amet, consectetur adipiscing elit. ',
                   'This is test data for performance measurement.'),
            CONCAT('user_', MOD(i, 1000)),
            1 + MOD(i, 5),
            'ACTIVE',
            CONCAT('https://cdn.example.com/articles/', current_article_id, '/cover.jpg'),
            MOD(i * 7, 10000),
            -- Time distribution for cursor pagination testing
            CASE
                WHEN i <= 200000 THEN DATE_SUB(NOW(), INTERVAL 300 + MOD(i, 65) DAY)  -- Old data (33%)
                WHEN i <= 400000 THEN DATE_SUB(NOW(), INTERVAL 30 + MOD(i, 270) DAY)  -- Middle data (33%)
                ELSE DATE_SUB(NOW(), INTERVAL MOD(i, 30) DAY)  -- Recent data (33%)
            END,
            NOW(),
            0,
            CONCAT('value_', i)  -- value column is NOT NULL
        );

        -- Add images for sample articles (every 50th article to save time)
        -- Using correct column names: sequence_no, article_image_url, image_id
        IF MOD(i, 50) = 0 THEN
            INSERT INTO article_images (article_id, sequence_no, article_image_url, image_id, image_url, display_order)
            VALUES
                (current_article_id, 1, CONCAT('https://cdn.example.com/img/', i, '_1.jpg'), CONCAT('IMG', i, '_1'), CONCAT('https://cdn.example.com/img/', i, '_1.jpg'), 1),
                (current_article_id, 2, CONCAT('https://cdn.example.com/img/', i, '_2.jpg'), CONCAT('IMG', i, '_2'), CONCAT('https://cdn.example.com/img/', i, '_2.jpg'), 2),
                (current_article_id, 3, CONCAT('https://cdn.example.com/img/', i, '_3.jpg'), CONCAT('IMG', i, '_3'), CONCAT('https://cdn.example.com/img/', i, '_3.jpg'), 3);

            -- Add keyword mappings with created_at
            INSERT INTO keyword_mapping_table (article_id, keyword_id, created_at)
            VALUES
                (current_article_id, 1 + MOD(i, 10), NOW()),
                (current_article_id, 1 + MOD(i + 3, 10), NOW()),
                (current_article_id, 1 + MOD(i + 7, 10), NOW());
        END IF;

        SET i = i + 1;
        SET batch_count = batch_count + 1;

        -- Commit every 10000 records
        IF batch_count >= 10000 THEN
            COMMIT;
            SET batch_count = 0;

            -- Progress report every 100K
            IF MOD(i, 100000) = 0 THEN
                SELECT CONCAT('Progress: ', i, ' / 600000 (', ROUND(i * 100.0 / 600000, 1), '%)') as status;
            END IF;
        END IF;
    END WHILE;

    -- Final commit
    IF batch_count > 0 THEN
        COMMIT;
    END IF;

    SELECT 'Data generation complete!' as status;
END$$

DELIMITER ;

-- Execute procedure
CALL generate_600k_complete();
DROP PROCEDURE IF EXISTS generate_600k_complete;

-- Update statistics
ANALYZE TABLE articles;
ANALYZE TABLE article_images;
ANALYZE TABLE keyword_mapping_table;

-- Report final counts
SELECT 'Data Generation Summary' as Report,
       COUNT(*) as total_articles,
       (SELECT COUNT(*) FROM article_images) as total_images,
       (SELECT COUNT(*) FROM keyword_mapping_table) as total_keywords
FROM articles;

-- Check data distribution
SELECT 'Data Time Distribution' as Report,
       COUNT(CASE WHEN created_at > DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) as recent_30_days,
       COUNT(CASE WHEN created_at BETWEEN DATE_SUB(NOW(), INTERVAL 300 DAY)
                  AND DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) as middle_period,
       COUNT(CASE WHEN created_at < DATE_SUB(NOW(), INTERVAL 300 DAY) THEN 1 END) as older_300_days
FROM articles;

SET FOREIGN_KEY_CHECKS = 1;
SET autocommit = 1;
SET unique_checks = 1;
EOF

# Execute data generation
docker compose exec -T article-mariadb mysql -u root -particlepass123 article_db < "$RESULT_DIR/generate-600k.sql" 2>&1 | tee "$RESULT_DIR/generation.log" | grep -E "Progress:|complete|Summary"

# Step 3: Build Project
echo -e "\n${YELLOW}[Step 3/6] Building Project...${NC}"
cd "$PROJECT_ROOT"
./gradlew clean build -x test > "$RESULT_DIR/build.log" 2>&1

# Step 4: Run ALL Performance Tests
echo -e "\n${YELLOW}[Step 4/6] Running Complete Performance Test Suite...${NC}"

# Define all test classes to run
TEST_CLASSES=(
    "ComprehensivePerformanceTest"
    "IndexAwarePerformanceTest"
    "ConcurrencyLoadTest"
    "NPlusOneQueryTest"
)

# Track test results
declare -a TEST_RESULTS
TOTAL_TESTS=${#TEST_CLASSES[@]}
PASSED_TESTS=0
FAILED_TESTS=0

echo -e "\n${CYAN}========== Test Suite Overview ==========${NC}"
echo -e "${CYAN}Total Tests to Run: ${TOTAL_TESTS}${NC}"
echo -e "${CYAN}Test Classes:${NC}"
for TEST_CLASS in "${TEST_CLASSES[@]}"; do
    echo -e "  ${MAGENTA}• $TEST_CLASS${NC}"
done
echo -e "${CYAN}=========================================${NC}"

# Run each test and save results
for i in "${!TEST_CLASSES[@]}"; do
    TEST_CLASS="${TEST_CLASSES[$i]}"
    TEST_NUM=$((i + 1))

    echo -e "\n${BLUE}┌─────────────────────────────────────────┐${NC}"
    echo -e "${BLUE}│  [${TEST_NUM}/${TOTAL_TESTS}] $TEST_CLASS${NC}"
    echo -e "${BLUE}└─────────────────────────────────────────┘${NC}"

    START_TIME=$(date +%s)

    # Run test - check for timeout command availability
    TEST_COMMAND="./gradlew test --tests \"com.teambind.articleserver.performance.measurement.$TEST_CLASS\" -Dspring.profiles.active=performance-test --no-daemon"

    if command -v timeout >/dev/null 2>&1; then
        # Linux - use timeout
        if timeout 300 bash -c "$TEST_COMMAND" 2>&1 | tee "$RESULT_DIR/${TEST_CLASS}.log"; then
            TEST_RESULT=0
        else
            TEST_RESULT=1
        fi
    elif command -v gtimeout >/dev/null 2>&1; then
        # macOS with coreutils - use gtimeout
        if gtimeout 300 bash -c "$TEST_COMMAND" 2>&1 | tee "$RESULT_DIR/${TEST_CLASS}.log"; then
            TEST_RESULT=0
        else
            TEST_RESULT=1
        fi
    else
        # No timeout command - run without timeout
        echo -e "${YELLOW}Warning: timeout command not found. Running without timeout limit.${NC}"
        if bash -c "$TEST_COMMAND" 2>&1 | tee "$RESULT_DIR/${TEST_CLASS}.log"; then
            TEST_RESULT=0
        else
            TEST_RESULT=1
        fi
    fi

    if [ $TEST_RESULT -eq 0 ]; then

        END_TIME=$(date +%s)
        DURATION=$((END_TIME - START_TIME))
        echo -e "${GREEN}✓ $TEST_CLASS completed successfully (${DURATION}s)${NC}"
        TEST_RESULTS+=("$TEST_CLASS: PASSED (${DURATION}s)")
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        END_TIME=$(date +%s)
        DURATION=$((END_TIME - START_TIME))
        echo -e "${RED}✗ $TEST_CLASS failed or timed out (${DURATION}s)${NC}"
        TEST_RESULTS+=("$TEST_CLASS: FAILED (${DURATION}s)")
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    # Add delay between tests to avoid resource conflicts
    if [ $TEST_NUM -lt $TOTAL_TESTS ]; then
        echo -e "${YELLOW}Waiting 5 seconds before next test...${NC}"
        sleep 5
    fi
done

# Step 5: Generate Comprehensive Report
echo -e "\n${YELLOW}[Step 5/6] Generating Comprehensive Report...${NC}"

cat > "$RESULT_DIR/performance-report.md" <<EOF
# 📊 Complete Performance Test Report
**Date**: $TIMESTAMP
**Data Size**: 600,000 articles

## Test Execution Summary

| Test Class | Status | Duration |
|------------|--------|----------|
EOF

# Add test results to report
for result in "${TEST_RESULTS[@]}"; do
    IFS=': ' read -r class_name status <<< "$result"
    if [[ "$status" == *"PASSED"* ]]; then
        echo "| $class_name | ✅ $status |" >> "$RESULT_DIR/performance-report.md"
    else
        echo "| $class_name | ❌ $status |" >> "$RESULT_DIR/performance-report.md"
    fi
done

cat >> "$RESULT_DIR/performance-report.md" <<EOF

## Overall Statistics
- **Total Tests**: $TOTAL_TESTS
- **Passed**: $PASSED_TESTS
- **Failed**: $FAILED_TESTS
- **Success Rate**: $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%

## Test Details

### 1. ComprehensivePerformanceTest
- Cursor Pagination (Early/Middle/Late)
- Filter Combinations
- Text Search
- Batch Queries

### 2. IndexAwarePerformanceTest
- Early Data Performance
- Middle Data Performance
- Recent Data Performance
- Realistic Usage Pattern

### 3. ConcurrencyLoadTest
- 100 Concurrent Users Simulation
- Multiple Scenarios (Single/List/Search/Filter/JDBC)
- Connection Pool Monitoring
- TPS Measurement

### 4. NPlusOneQueryTest
- Lazy Loading (N+1 Problem Detection)
- Eager Loading
- Fetch Join Optimization
- Entity Graph
- Batch Fetch
- DTO Projection

## Key Metrics Summary
EOF

# Extract key metrics from logs
echo -e "\n### Performance Metrics (P50/P95/P99)" >> "$RESULT_DIR/performance-report.md"
echo '```' >> "$RESULT_DIR/performance-report.md"

for TEST_CLASS in "${TEST_CLASSES[@]}"; do
    if [ -f "$RESULT_DIR/${TEST_CLASS}.log" ]; then
        echo "=== $TEST_CLASS ===" >> "$RESULT_DIR/performance-report.md"
        grep -E "P50|P95|P99|TPS|Success Rate|Error Rate|N\+1|Query Count" "$RESULT_DIR/${TEST_CLASS}.log" | tail -10 >> "$RESULT_DIR/performance-report.md" 2>/dev/null || echo "No metrics found" >> "$RESULT_DIR/performance-report.md"
        echo "" >> "$RESULT_DIR/performance-report.md"
    fi
done

echo '```' >> "$RESULT_DIR/performance-report.md"

# Step 6: Run Analysis Script
echo -e "\n${YELLOW}[Step 6/6] Running Advanced Performance Analysis...${NC}"
if [ -f "$SCRIPT_DIR/analyze-results-ko.sh" ]; then
    chmod +x "$SCRIPT_DIR/analyze-results-ko.sh"
    "$SCRIPT_DIR/analyze-results-ko.sh" "$TIMESTAMP"
    echo -e "${GREEN}✓ Advanced analysis complete${NC}"
else
    echo -e "${YELLOW}⚠ Analysis script not found, basic report only${NC}"
fi

# Final Summary
echo -e "\n${GREEN}===================================================${NC}"
echo -e "${GREEN}      Performance Test Suite Complete!${NC}"
echo -e "${GREEN}===================================================${NC}"
echo -e "${CYAN}Results saved to: $RESULT_DIR${NC}"
echo -e "${CYAN}Report files:${NC}"
echo -e "  ${MAGENTA}• performance-report.md${NC} - Main report"
if [ -f "$RESULT_DIR/analysis_report_ko.md" ]; then
    echo -e "  ${MAGENTA}• analysis_report_ko.md${NC} - Korean analysis"
fi
echo -e "${GREEN}===================================================${NC}"

# Show test summary
echo -e "\n${BLUE}Test Results Summary:${NC}"
echo -e "┌─────────────────────────────────────────┐"
printf "│  Total: %2d  Passed: %2d  Failed: %2d      │\n" $TOTAL_TESTS $PASSED_TESTS $FAILED_TESTS
echo -e "└─────────────────────────────────────────┘"

# Show key metrics if available
echo -e "\n${BLUE}Key Performance Indicators:${NC}"
if [ -f "$RESULT_DIR/analysis_report_ko.md" ]; then
    grep -A 5 "핵심 성과 지표" "$RESULT_DIR/analysis_report_ko.md" 2>/dev/null || true
fi

# Optional: Clean up database after tests
echo ""
read -p "Do you want to clean up the database? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "\n${YELLOW}Cleaning up database...${NC}"
    docker compose exec -T article-mariadb mysql -u root -particlepass123 article_db <<EOF
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE keyword_mapping_table;
TRUNCATE TABLE article_images;
DELETE FROM articles WHERE article_id LIKE 'PERF%';
SET FOREIGN_KEY_CHECKS = 1;
SELECT 'Cleanup complete' as status;
EOF
fi

echo -e "\n${GREEN}All done! Check $RESULT_DIR for detailed results.${NC}"
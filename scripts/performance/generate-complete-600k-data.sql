-- =====================================================
-- 60만개 완전한 테스트 데이터 생성 SQL
-- 게시글 600,000개
-- 이미지 1,800,000개 (게시글당 3개)
-- 키워드 매핑 2,400,000개 (게시글당 4개)
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;
SET autocommit = 0;
SET unique_checks = 0;

-- 성능 향상을 위한 설정
SET SESSION sql_log_bin = 0;
SET SESSION innodb_lock_wait_timeout = 50;

DELIMITER $$

-- 대량 데이터 생성 프로시저
DROP PROCEDURE IF EXISTS generate_complete_test_data$$
CREATE PROCEDURE generate_complete_test_data(IN target_count INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE current_article_id VARCHAR(50);
    DECLARE board_count INT;
    DECLARE keyword_count INT;

    -- Board 데이터 확인 및 생성
    SELECT COUNT(*) INTO board_count FROM boards;
    IF board_count < 5 THEN
        INSERT INTO boards (id, name, description, is_active, created_at)
        VALUES (1, '공지사항', '공지사항 게시판입니다.', true, NOW()),
               (2, '자유게시판', '자유게시판입니다.', true, NOW()),
               (3, '질문답변', '질문답변 게시판입니다.', true, NOW()),
               (4, '기술블로그', '기술블로그 게시판입니다.', true, NOW()),
               (5, '이벤트', '이벤트 게시판입니다.', true, NOW())
        ON DUPLICATE KEY UPDATE name=VALUES(name);
    END IF;

    -- Keywords 생성 (없으면)
    SELECT COUNT(*) INTO keyword_count FROM keywords;
    IF keyword_count < 20 THEN
        INSERT INTO keywords (id, name, board_id, is_active, usage_count)
        VALUES (1, 'Spring', NULL, true, 0),
               (2, 'Java', NULL, true, 0),
               (3, 'JPA', NULL, true, 0),
               (4, 'Hibernate', NULL, true, 0),
               (5, 'MySQL', NULL, true, 0),
               (6, 'Redis', NULL, true, 0),
               (7, 'Kafka', NULL, true, 0),
               (8, 'Docker', NULL, true, 0),
               (9, 'Kubernetes', NULL, true, 0),
               (10, 'AWS', NULL, true, 0),
               (11, 'MSA', NULL, true, 0),
               (12, 'DDD', NULL, true, 0),
               (13, 'Performance', NULL, true, 0),
               (14, 'Optimization', NULL, true, 0),
               (15, 'Testing', NULL, true, 0),
               (16, 'Security', NULL, true, 0),
               (17, 'API', NULL, true, 0),
               (18, 'REST', NULL, true, 0),
               (19, 'GraphQL', NULL, true, 0),
               (20, 'WebSocket', NULL, true, 0)
        ON DUPLICATE KEY UPDATE name=VALUES(name);
    END IF;

    -- 기존 데이터 정리 (선택적)
    -- DELETE FROM keyword_mapping_table WHERE article_id LIKE 'PERF%';
    -- DELETE FROM article_images WHERE article_id LIKE 'PERF%';
    -- DELETE FROM articles WHERE article_id LIKE 'PERF%';

    -- 진행 상황 로그 테이블
    DROP TEMPORARY TABLE IF EXISTS progress_log;
    CREATE TEMPORARY TABLE progress_log
    (
        message    VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    INSERT INTO progress_log (message) VALUES (CONCAT('Starting data generation for ', target_count, ' articles'));

    -- 메인 루프: 배치 단위로 데이터 생성
    WHILE i < target_count
        DO
            -- 현재 배치의 시작
            SET @batch_start = i;
            SET @batch_end = LEAST(i + batch_size, target_count);

            -- 배치 내 각 article 생성
            WHILE @batch_start < @batch_end
                DO
                    SET current_article_id = CONCAT('PERF', LPAD(@batch_start, 15, '0'));

                    -- 1. Article 생성
                    INSERT INTO articles (article_id,
                                          article_type,
                                          title,
                                          contents,
                                          writer_id,
                                          board_id,
                                          status,
                                          first_image_url,
                                          view_count,
                                          created_at,
                                          updated_at,
                                          version)
                    VALUES (current_article_id,
                            CASE
                                WHEN @batch_start % 100 < 90 THEN 'RegularArticle'
                                WHEN @batch_start % 100 < 95 THEN 'NoticeArticle'
                                ELSE 'EventArticle'
                                END,
                            CONCAT('Performance Test Article #', @batch_start,
                                   CASE @batch_start % 10
                                       WHEN 0 THEN ' - Spring Boot 최적화 가이드'
                                       WHEN 1 THEN ' - JPA N+1 문제 해결 방법'
                                       WHEN 2 THEN ' - MSA 아키텍처 설계 패턴'
                                       WHEN 3 THEN ' - 도메인 주도 설계 실습'
                                       WHEN 4 THEN ' - Redis 캐싱 전략'
                                       WHEN 5 THEN ' - Kafka 이벤트 스트리밍'
                                       WHEN 6 THEN ' - Docker 컨테이너 최적화'
                                       WHEN 7 THEN ' - Kubernetes 배포 전략'
                                       WHEN 8 THEN ' - AWS 클라우드 마이그레이션'
                                       ELSE ' - 성능 튜닝 케이스 스터디'
                                       END),
                            CONCAT('이것은 성능 테스트를 위한 본문입니다. 게시글 번호: ', @batch_start, '\n',
                                   '실제 운영 환경과 유사한 텍스트 길이를 만들기 위해 충분한 내용을 포함합니다.\n',
                                   'Lorem ipsum dolor sit amet, consectetur adipiscing elit. ',
                                   'Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ',
                                   'Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris. ',
                                   'Article ID: ', current_article_id),
                            CONCAT('user_', FLOOR(RAND() * 1000)),
                            1 + FLOOR(RAND() * 5),
                            'ACTIVE',
                            CONCAT('https://cdn.example.com/articles/', current_article_id, '/cover.jpg'),
                            FLOOR(RAND() * 10000),
                               -- 시간 분포: 오래된 데이터(20%), 중간 데이터(60%), 최신 데이터(20%)
                            CASE
                                WHEN @batch_start < target_count * 0.2
                                    THEN DATE_SUB(NOW(), INTERVAL 300 + FLOOR(RAND() * 65) DAY)
                                WHEN @batch_start < target_count * 0.8
                                    THEN DATE_SUB(NOW(), INTERVAL 30 + FLOOR(RAND() * 270) DAY)
                                ELSE DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
                                END,
                            NOW(),
                            0);

                    -- 2. Article Images 생성 (게시글당 3개)
                    INSERT INTO article_images (article_id, sequence_num, article_image_url, image_id)
                    VALUES (current_article_id, 1, CONCAT('https://cdn.example.com/img/', current_article_id, '_1.jpg'),
                            CONCAT('IMG', current_article_id, '_1')),
                           (current_article_id, 2, CONCAT('https://cdn.example.com/img/', current_article_id, '_2.jpg'),
                            CONCAT('IMG', current_article_id, '_2')),
                           (current_article_id, 3, CONCAT('https://cdn.example.com/img/', current_article_id, '_3.jpg'),
                            CONCAT('IMG', current_article_id, '_3'));

                    -- 3. Keyword Mappings 생성 (게시글당 4개)
                    -- 랜덤하게 키워드 선택 (실제 사용 패턴 시뮬레이션)
                    INSERT INTO keyword_mapping_table (article_id, keyword_id)
                    SELECT current_article_id, id
                    FROM (SELECT id
                          FROM keywords
                          ORDER BY RAND()
                          LIMIT 4) AS selected_keywords;

                    SET @batch_start = @batch_start + 1;
                END WHILE;

            -- 배치 커밋
            COMMIT;

            -- 진행 상황 로그 (10% 단위로)
            IF i % (target_count / 10) = 0 THEN
                INSERT INTO progress_log (message)
                VALUES (CONCAT('Progress: ', (i * 100 / target_count), '% complete (', i, '/', target_count, ')'));

                -- 진행 상황 출력
                SELECT CONCAT('Generated ', i, ' / ', target_count, ' articles (',
                              ROUND(i * 100 / target_count, 1), '%)') AS progress_status;
            END IF;

            SET i = i + batch_size;
        END WHILE;

    -- 완료 로그
    INSERT INTO progress_log (message) VALUES ('Data generation complete!');

    -- 최종 통계
    SELECT 'Generation Complete!'                                                     as Status,
           (SELECT COUNT(*) FROM articles WHERE article_id LIKE 'PERF%')              as Articles_Generated,
           (SELECT COUNT(*) FROM article_images WHERE article_id LIKE 'PERF%')        as Images_Generated,
           (SELECT COUNT(*) FROM keyword_mapping_table WHERE article_id LIKE 'PERF%') as Keywords_Mapped;

END$$

DELIMITER ;

-- =====================================================
-- 실행 부분
-- =====================================================

-- 현재 데이터 상태 확인
SELECT 'Before Generation:'                         as Phase,
       (SELECT COUNT(*) FROM articles)              as Articles,
       (SELECT COUNT(*) FROM article_images)        as Images,
       (SELECT COUNT(*) FROM keyword_mapping_table) as Keywords;

-- 프로시저 실행 (60만개 생성)
-- 주의: 이 작업은 시간이 오래 걸릴 수 있습니다 (약 10-30분)
CALL generate_complete_test_data(600000);

-- 생성 후 통계
SELECT 'After Generation:'                          as Phase,
       (SELECT COUNT(*) FROM articles)              as Articles,
       (SELECT COUNT(*) FROM article_images)        as Images,
       (SELECT COUNT(*) FROM keyword_mapping_table) as Keywords;

-- 인덱스 통계 업데이트 (중요!)
ANALYZE TABLE articles;
ANALYZE TABLE article_images;
ANALYZE TABLE keyword_mapping_table;

-- 데이터 분포 확인
SELECT 'Data Distribution Check' as Report,
       MIN(created_at)           as Oldest_Article,
       MAX(created_at)           as Newest_Article,
       COUNT(DISTINCT board_id)  as Board_Count,
       COUNT(DISTINCT writer_id) as User_Count,
       AVG(view_count)           as Avg_Views
FROM articles
WHERE article_id LIKE 'PERF%';

-- 키워드 사용 통계
SELECT k.name               as Keyword,
       COUNT(km.article_id) as Usage_Count
FROM keywords k
         LEFT JOIN keyword_mapping_table km ON k.id = km.keyword_id
WHERE km.article_id LIKE 'PERF%'
GROUP BY k.id, k.name
ORDER BY Usage_Count DESC
LIMIT 10;

SET FOREIGN_KEY_CHECKS = 1;
SET autocommit = 1;
SET unique_checks = 1;

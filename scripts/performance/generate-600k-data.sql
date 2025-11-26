-- 60만개 테스트 데이터 생성 SQL
-- 더 효율적인 방법: 기존 데이터를 복제하여 exponentially 증가

SET FOREIGN_KEY_CHECKS = 0;
SET autocommit = 0;

-- 1. 현재 articles 테이블의 최대 ID 확인
SELECT @max_id := MAX(CAST(article_id AS UNSIGNED))
FROM articles;
SELECT @counter := COALESCE(@max_id, 1000000000);

-- 2. Board 데이터 확인 및 생성
INSERT IGNORE INTO boards (name, description, is_active, created_at)
VALUES ('공지사항', '공지사항 게시판입니다.', true, NOW()),
       ('자유게시판', '자유게시판입니다.', true, NOW()),
       ('질문답변', '질문답변 게시판입니다.', true, NOW()),
       ('기술블로그', '기술블로그 게시판입니다.', true, NOW()),
       ('이벤트', '이벤트 게시판입니다.', true, NOW());

-- 3. Keywords 생성 (없으면)
INSERT IGNORE INTO keywords (name, board_id, is_active, usage_count)
VALUES ('Spring', NULL, true, 0),
       ('Java', NULL, true, 0),
       ('JPA', NULL, true, 0),
       ('Hibernate', NULL, true, 0),
       ('MySQL', NULL, true, 0),
       ('Redis', NULL, true, 0),
       ('Kafka', NULL, true, 0),
       ('Docker', NULL, true, 0),
       ('Kubernetes', NULL, true, 0),
       ('AWS', NULL, true, 0),
       ('MSA', NULL, true, 0),
       ('DDD', NULL, true, 0),
       ('Performance', NULL, true, 0),
       ('Optimization', NULL, true, 0),
       ('Testing', NULL, true, 0),
       ('Security', NULL, true, 0),
       ('API', NULL, true, 0),
       ('REST', NULL, true, 0),
       ('GraphQL', NULL, true, 0),
       ('WebSocket', NULL, true, 0);

-- 4. 기존 articles 데이터를 기반으로 복제하여 대량 생성
-- 단계적으로 데이터를 증가시킴 (10K -> 20K -> 40K -> 80K -> 160K -> 320K -> 640K)

-- 임시 테이블 생성
DROP TEMPORARY TABLE IF EXISTS temp_articles;
CREATE TEMPORARY TABLE temp_articles AS
SELECT *
FROM articles
LIMIT 10000;

-- Round 1: 10K -> 20K (기존 데이터 복제)
INSERT INTO articles (article_id, article_type, title, contents, writer_id,
                      board_id, status, first_image_url, view_count,
                      created_at, updated_at, version)
SELECT CONCAT('ART', LPAD(@counter := @counter + 1, 20, '0')),
       article_type,
       CONCAT(title, ' - Copy 1'),
       contents,
       CONCAT('user_', FLOOR(RAND() * 1000)),
       board_id,
       status,
       first_image_url,
       FLOOR(RAND() * 10000),
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
       NOW(),
       0
FROM temp_articles;

COMMIT;

-- Round 2: 20K -> 40K
INSERT INTO articles (article_id, article_type, title, contents, writer_id,
                      board_id, status, first_image_url, view_count,
                      created_at, updated_at, version)
SELECT CONCAT('ART', LPAD(@counter := @counter + 1, 20, '0')),
       article_type,
       CONCAT('Performance Test Article #', @counter),
       CONCAT('Content for article #', @counter, '. ', contents),
       CONCAT('user_', FLOOR(RAND() * 1000)),
       board_id,
       'ACTIVE',
       CONCAT('https://cdn.example.com/img/', @counter, '.jpg'),
       FLOOR(RAND() * 10000),
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
       NOW(),
       0
FROM articles
WHERE article_id LIKE 'ART202511261%'
LIMIT 20000;

COMMIT;

-- Round 3: 40K -> 80K
INSERT INTO articles (article_id, article_type, title, contents, writer_id,
                      board_id, status, first_image_url, view_count,
                      created_at, updated_at, version)
SELECT CONCAT('ART', LPAD(@counter := @counter + 1, 20, '0')),
       article_type,
       CONCAT('Bulk Test Article #', @counter),
       CONCAT('Generated content for performance testing. Article ID: ', @counter),
       CONCAT('user_', FLOOR(RAND() * 1000)),
       board_id,
       'ACTIVE',
       CONCAT('https://cdn.example.com/img/', @counter, '.jpg'),
       FLOOR(RAND() * 10000),
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
       NOW(),
       0
FROM articles
LIMIT 40000;

COMMIT;

-- Round 4: 80K -> 160K
INSERT INTO articles (article_id, article_type, title, contents, writer_id,
                      board_id, status, first_image_url, view_count,
                      created_at, updated_at, version)
SELECT CONCAT('ART', LPAD(@counter := @counter + 1, 20, '0')),
       CASE
           WHEN @counter % 100 < 90 THEN 'RegularArticle'
           WHEN @counter % 100 < 95 THEN 'NoticeArticle'
           ELSE 'EventArticle'
           END,
       CONCAT('Scale Test Article #', @counter),
       CONCAT('This is a performance test article with ID: ', @counter,
              '. Testing query performance with large datasets.'),
       CONCAT('user_', FLOOR(RAND() * 1000)),
       1 + FLOOR(RAND() * 5),
       'ACTIVE',
       CONCAT('https://cdn.example.com/img/', @counter, '.jpg'),
       FLOOR(RAND() * 10000),
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
       NOW(),
       0
FROM articles
LIMIT 80000;

COMMIT;

-- Round 5: 160K -> 320K
INSERT INTO articles (article_id, article_type, title, contents, writer_id,
                      board_id, status, first_image_url, view_count,
                      created_at, updated_at, version)
SELECT CONCAT('ART', LPAD(@counter := @counter + 1, 20, '0')),
       CASE
           WHEN @counter % 100 < 90 THEN 'RegularArticle'
           WHEN @counter % 100 < 95 THEN 'NoticeArticle'
           ELSE 'EventArticle'
           END,
       CONCAT('Load Test Article #', @counter),
       CONCAT(
               'Performance testing content. This article is part of a large dataset for testing query optimization. ID: ',
               @counter),
       CONCAT('user_', FLOOR(RAND() * 1000)),
       1 + FLOOR(RAND() * 5),
       'ACTIVE',
       CONCAT('https://cdn.example.com/img/', @counter, '.jpg'),
       FLOOR(RAND() * 10000),
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
       NOW(),
       0
FROM articles
LIMIT 160000;

COMMIT;

-- Round 6: 320K -> 600K+
INSERT INTO articles (article_id, article_type, title, contents, writer_id,
                      board_id, status, first_image_url, view_count,
                      created_at, updated_at, version)
SELECT CONCAT('ART', LPAD(@counter := @counter + 1, 20, '0')),
       CASE
           WHEN @counter % 100 < 90 THEN 'RegularArticle'
           WHEN @counter % 100 < 95 THEN 'NoticeArticle'
           ELSE 'EventArticle'
           END,
       CONCAT('Final Test Article #', @counter),
       CONCAT(
               'Final round of test data generation. This content is designed for performance benchmarking. Article number: ',
               @counter),
       CONCAT('user_', FLOOR(RAND() * 1000)),
       1 + FLOOR(RAND() * 5),
       'ACTIVE',
       CONCAT('https://cdn.example.com/img/', @counter, '.jpg'),
       FLOOR(RAND() * 10000),
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
       NOW(),
       0
FROM articles
LIMIT 280000;

COMMIT;

-- 5. 최종 확인
SELECT COUNT(*) as total_articles
FROM articles;

-- 6. 통계 업데이트
ANALYZE TABLE articles;

SET FOREIGN_KEY_CHECKS = 1;
SET autocommit = 1;

-- 완료 메시지
SELECT '데이터 생성 완료!' as message, COUNT(*) as total_count
FROM articles;

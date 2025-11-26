-- =====================================================
-- 10만개 테스트 데이터 빠른 생성 SQL
-- 기존 데이터를 복제하여 지수적으로 증가
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;
SET autocommit = 0;
SET unique_checks = 0;

-- 현재 articles 수 확인
SELECT COUNT(*) as current_count
FROM articles;

-- 1단계: 기존 10K를 20K로 (10K 복제)
INSERT INTO articles (article_id, article_type, title, contents, writer_id, board_id, status, first_image_url,
                      view_count, created_at, updated_at, version)
SELECT CONCAT('BULK', LPAD(10000 + ROW_NUMBER() OVER (ORDER BY article_id), 10, '0')),
       article_type,
       CONCAT(title, ' - Bulk Copy 1'),
       contents,
       CONCAT('user_', FLOOR(RAND() * 1000)),
       IFNULL(board_id, 1),
       status,
       first_image_url,
       FLOOR(RAND() * 10000),
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
       NOW(),
       0
FROM articles
WHERE article_id NOT LIKE 'BULK%'
LIMIT 10000;

COMMIT;
SELECT COUNT(*) as after_step1
FROM articles;

-- 2단계: 20K를 40K로 (20K 복제)
INSERT INTO articles (article_id, article_type, title, contents, writer_id, board_id, status, first_image_url,
                      view_count, created_at, updated_at, version)
SELECT CONCAT('BULK', LPAD(20000 + ROW_NUMBER() OVER (ORDER BY article_id), 10, '0')),
       article_type,
       CONCAT('Performance Test - ', SUBSTRING(MD5(RAND()), 1, 8)),
       CONCAT('Test content for performance measurement. Random: ', SUBSTRING(MD5(RAND()), 1, 32)),
       CONCAT('user_', FLOOR(RAND() * 1000)),
       1 + FLOOR(RAND() * 5),
       'ACTIVE',
       CONCAT('https://cdn.example.com/img/', SUBSTRING(MD5(RAND()), 1, 16), '.jpg'),
       FLOOR(RAND() * 10000),
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
       NOW(),
       0
FROM articles
LIMIT 20000;

COMMIT;
SELECT COUNT(*) as after_step2
FROM articles;

-- 3단계: 40K를 80K로 (40K 복제)
INSERT INTO articles (article_id, article_type, title, contents, writer_id, board_id, status, first_image_url,
                      view_count, created_at, updated_at, version)
SELECT CONCAT('BULK', LPAD(40000 + ROW_NUMBER() OVER (ORDER BY article_id), 10, '0')),
       CASE
           WHEN RAND() < 0.9 THEN 'RegularArticle'
           WHEN RAND() < 0.95 THEN 'NoticeArticle'
           ELSE 'EventArticle'
           END,
       CONCAT('Scale Test Article - ', SUBSTRING(MD5(RAND()), 1, 8)),
       CONCAT('Performance testing content with varied data distribution. ID: ', SUBSTRING(MD5(RAND()), 1, 32)),
       CONCAT('user_', FLOOR(RAND() * 1000)),
       1 + FLOOR(RAND() * 5),
       'ACTIVE',
       CONCAT('https://cdn.example.com/img/', SUBSTRING(MD5(RAND()), 1, 16), '.jpg'),
       FLOOR(RAND() * 10000),
       -- 시간 분포: 오래된(20%), 중간(60%), 최신(20%)
       CASE
           WHEN RAND() < 0.2 THEN DATE_SUB(NOW(), INTERVAL 300 + FLOOR(RAND() * 65) DAY)
           WHEN RAND() < 0.8 THEN DATE_SUB(NOW(), INTERVAL 30 + FLOOR(RAND() * 270) DAY)
           ELSE DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
           END,
       NOW(),
       0
FROM articles
LIMIT 40000;

COMMIT;
SELECT COUNT(*) as after_step3
FROM articles;

-- 4단계: 80K를 100K로 (20K 추가)
INSERT INTO articles (article_id, article_type, title, contents, writer_id, board_id, status, first_image_url,
                      view_count, created_at, updated_at, version)
SELECT CONCAT('BULK', LPAD(80000 + ROW_NUMBER() OVER (ORDER BY article_id), 10, '0')),
       CASE
           WHEN RAND() < 0.9 THEN 'RegularArticle'
           WHEN RAND() < 0.95 THEN 'NoticeArticle'
           ELSE 'EventArticle'
           END,
       CONCAT('Final Batch Article - ', SUBSTRING(MD5(RAND()), 1, 8)),
       CONCAT('Final batch for 100K dataset. Testing index performance across data ranges. Random: ',
              SUBSTRING(MD5(RAND()), 1, 32)),
       CONCAT('user_', FLOOR(RAND() * 1000)),
       1 + FLOOR(RAND() * 5),
       'ACTIVE',
       CONCAT('https://cdn.example.com/img/', SUBSTRING(MD5(RAND()), 1, 16), '.jpg'),
       FLOOR(RAND() * 10000),
       -- 최신 데이터 위주로 생성
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
       NOW(),
       0
FROM articles
LIMIT 20000;

COMMIT;

-- 5단계: 이미지 데이터 생성 (간단히 article당 3개)
-- 시간이 오래 걸릴 수 있으므로 샘플만 생성
INSERT INTO article_images (article_id, sequence_num, article_image_url, image_id)
SELECT a.article_id,
       n.num,
       CONCAT('https://cdn.example.com/img/', a.article_id, '_', n.num, '.jpg'),
       CONCAT('IMG_', a.article_id, '_', n.num)
FROM (SELECT article_id
      FROM articles
      WHERE article_id LIKE 'BULK%'
      ORDER BY RAND()
      LIMIT 10000) a
         CROSS JOIN (SELECT 1 as num UNION SELECT 2 UNION SELECT 3) n;

COMMIT;

-- 6단계: 키워드 매핑 생성 (간단히 article당 2-4개)
-- Keywords 테이블에 데이터 확인 및 생성
INSERT IGNORE INTO keywords (id, name, keyword_name, board_id, is_active, usage_count, description)
SELECT id,
       CONCAT('Keyword', id),
       CONCAT('Keyword', id),
       NULL,
       true,
       0,
       CONCAT('Test Keyword ', id)
FROM (SELECT 1 as id
      UNION
      SELECT 2
      UNION
      SELECT 3
      UNION
      SELECT 4
      UNION
      SELECT 5
      UNION
      SELECT 6
      UNION
      SELECT 7
      UNION
      SELECT 8
      UNION
      SELECT 9
      UNION
      SELECT 10
      UNION
      SELECT 11
      UNION
      SELECT 12
      UNION
      SELECT 13
      UNION
      SELECT 14
      UNION
      SELECT 15
      UNION
      SELECT 16
      UNION
      SELECT 17
      UNION
      SELECT 18
      UNION
      SELECT 19
      UNION
      SELECT 20) t;

-- Boards 테이블에 데이터 확인 및 생성
INSERT IGNORE INTO boards (id, name, board_name, description, is_active, created_at)
SELECT id,
       name,
       name,
       CONCAT(name, ' 게시판'),
       true,
       NOW()
FROM (SELECT 1 as id, '공지사항' as name
      UNION
      SELECT 2, '자유게시판'
      UNION
      SELECT 3, '질문답변'
      UNION
      SELECT 4, '기술블로그'
      UNION
      SELECT 5, '이벤트') t;

-- 키워드 매핑 샘플 생성
INSERT INTO keyword_mapping_table (article_id, keyword_id)
SELECT a.article_id,
       1 + FLOOR(RAND() * 20) as keyword_id
FROM (SELECT article_id
      FROM articles
      WHERE article_id LIKE 'BULK%'
      ORDER BY RAND()
      LIMIT 20000) a
UNION ALL
SELECT a.article_id,
       1 + FLOOR(RAND() * 20) as keyword_id
FROM (SELECT article_id
      FROM articles
      WHERE article_id LIKE 'BULK%'
      ORDER BY RAND()
      LIMIT 20000) a;

COMMIT;

-- 통계 업데이트
ANALYZE TABLE articles;
ANALYZE TABLE article_images;
ANALYZE TABLE keyword_mapping_table;

-- 최종 확인
SELECT 'Data Generation Complete!'                                                                            as Status,
       (SELECT COUNT(*) FROM articles)                                                                        as Total_Articles,
       (SELECT COUNT(*) FROM article_images)                                                                  as Total_Images,
       (SELECT COUNT(*) FROM keyword_mapping_table)                                                           as Total_Keyword_Mappings,
       (SELECT COUNT(DISTINCT article_id)
        FROM articles
        WHERE created_at > DATE_SUB(NOW(), INTERVAL 30 DAY))                                                  as Recent_Articles,
       (SELECT COUNT(DISTINCT article_id)
        FROM articles
        WHERE created_at < DATE_SUB(NOW(), INTERVAL 300 DAY))                                                 as Old_Articles;

-- 데이터 분포 확인
SELECT 'Data Distribution'                                                        as Report,
       MIN(created_at)                                                            as Oldest_Date,
       MAX(created_at)                                                            as Newest_Date,
       COUNT(CASE WHEN created_at > DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END)  as Last_30_Days,
       COUNT(CASE
                 WHEN created_at BETWEEN DATE_SUB(NOW(), INTERVAL 300 DAY) AND DATE_SUB(NOW(), INTERVAL 30 DAY)
                     THEN 1 END)                                                  as Middle_Period,
       COUNT(CASE WHEN created_at < DATE_SUB(NOW(), INTERVAL 300 DAY) THEN 1 END) as Older_Than_300_Days
FROM articles;

SET FOREIGN_KEY_CHECKS = 1;
SET autocommit = 1;
SET unique_checks = 1;

SET FOREIGN_KEY_CHECKS = 0;
SET autocommit = 0;
SET unique_checks = 0;
SET SESSION sql_log_bin = 0;

-- Create boards (no value column, has board_name)
INSERT INTO boards (id, name, board_name, description, is_active, created_at)
VALUES (1, '공지사항', 'notice', '공지사항 게시판', true, NOW()),
       (2, '자유게시판', 'free', '자유게시판', true, NOW()),
       (3, '질문답변', 'qna', '질문답변 게시판', true, NOW()),
       (4, '기술블로그', 'tech', '기술블로그', true, NOW()),
       (5, '이벤트', 'event', '이벤트 게시판', true, NOW());

-- Create keywords with keyword_name and usage_count
INSERT INTO keywords (id, name, keyword_name, board_id, is_active, usage_count, description)
VALUES (1, 'Spring', 'spring', NULL, true, 0, 'Spring Framework'),
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

    WHILE i <= 600000
        DO
            SET current_article_id = CONCAT('PERF', LPAD(i, 15, '0'));

            -- Insert article with value column (required, NOT NULL)
            INSERT INTO articles (article_id, article_type, title, contents, writer_id,
                                  board_id, status, first_image_url, view_count,
                                  created_at, updated_at, version, value)
            VALUES (current_article_id,
                    'RegularArticle', -- Using actual discriminator value
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
                        WHEN i <= 200000 THEN DATE_SUB(NOW(), INTERVAL 300 + MOD(i, 65) DAY) -- Old data (33%)
                        WHEN i <= 400000 THEN DATE_SUB(NOW(), INTERVAL 30 + MOD(i, 270) DAY) -- Middle data (33%)
                        ELSE DATE_SUB(NOW(), INTERVAL MOD(i, 30) DAY) -- Recent data (33%)
                        END,
                    NOW(),
                    0,
                    CONCAT('value_', i) -- value column is NOT NULL
                   );

            -- Add images for sample articles (every 50th article to save time)
            -- Using correct column names: sequence_no, article_image_url, image_id
            IF MOD(i, 50) = 0 THEN
                INSERT INTO article_images (article_id, sequence_no, article_image_url, image_id, image_url,
                                            display_order)
                VALUES (current_article_id, 1, CONCAT('https://cdn.example.com/img/', i, '_1.jpg'),
                        CONCAT('IMG', i, '_1'), CONCAT('https://cdn.example.com/img/', i, '_1.jpg'), 1),
                       (current_article_id, 2, CONCAT('https://cdn.example.com/img/', i, '_2.jpg'),
                        CONCAT('IMG', i, '_2'), CONCAT('https://cdn.example.com/img/', i, '_2.jpg'), 2),
                       (current_article_id, 3, CONCAT('https://cdn.example.com/img/', i, '_3.jpg'),
                        CONCAT('IMG', i, '_3'), CONCAT('https://cdn.example.com/img/', i, '_3.jpg'), 3);

                -- Add keyword mappings with created_at
                INSERT INTO keyword_mapping_table (article_id, keyword_id, created_at)
                VALUES (current_article_id, 1 + MOD(i, 10), NOW()),
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
SELECT 'Data Generation Summary'                    as Report,
       COUNT(*)                                     as total_articles,
       (SELECT COUNT(*) FROM article_images)        as total_images,
       (SELECT COUNT(*) FROM keyword_mapping_table) as total_keywords
FROM articles;

-- Check data distribution
SELECT 'Data Time Distribution'                                                   as Report,
       COUNT(CASE WHEN created_at > DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END)  as recent_30_days,
       COUNT(CASE
                 WHEN created_at BETWEEN DATE_SUB(NOW(), INTERVAL 300 DAY)
                     AND DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END)             as middle_period,
       COUNT(CASE WHEN created_at < DATE_SUB(NOW(), INTERVAL 300 DAY) THEN 1 END) as older_300_days
FROM articles;

SET FOREIGN_KEY_CHECKS = 1;
SET autocommit = 1;
SET unique_checks = 1;

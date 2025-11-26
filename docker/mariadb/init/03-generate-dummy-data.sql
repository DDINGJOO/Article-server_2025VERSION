-- 대량 더미 데이터 생성 스크립트 (약 60만개)
-- 성능 테스트를 위한 다양한 패턴의 데이터 생성

USE article_db;

-- 프로시저 생성을 위한 구분자 변경
DELIMITER $$

-- 랜덤 텍스트 생성 함수
CREATE FUNCTION IF NOT EXISTS generate_random_text(min_length INT, max_length INT)
    RETURNS TEXT
    DETERMINISTIC
BEGIN
    DECLARE result TEXT DEFAULT '';
    DECLARE chars VARCHAR(255) DEFAULT 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ';
    DECLARE text_length INT;
    DECLARE i INT DEFAULT 0;

    SET text_length = FLOOR(min_length + RAND() * (max_length - min_length));

    WHILE i < text_length
        DO
            SET result = CONCAT(result, SUBSTRING(chars, FLOOR(1 + RAND() * 63), 1));
            SET i = i + 1;
        END WHILE;

    RETURN result;
END$$

-- 랜덤 한글 텍스트 생성 함수
CREATE FUNCTION IF NOT EXISTS generate_korean_text(word_count INT)
    RETURNS TEXT
    DETERMINISTIC
BEGIN
    DECLARE result TEXT DEFAULT '';
    DECLARE korean_words TEXT DEFAULT '안녕하세요,감사합니다,개발자,프로그래밍,스프링부트,자바,데이터베이스,테스트,성능,최적화,알고리즘,시스템,아키텍처,마이크로서비스,도커,쿠버네티스,레디스,카프카,메시지,이벤트,트랜잭션,쿼리,인덱스,캐시,서버,클라이언트,프론트엔드,백엔드,풀스택,모니터링,로깅,디버깅,배포,운영,보안,인증,권한,세션,토큰,암호화,해싱,네트워크,프로토콜,레스트,그래프큐엘,웹소켓,비동기,동기,스레드,프로세스,메모리,가비지컬렉션,힙,스택,큐,리스트,맵,세트,트리,그래프,정렬,검색,삽입,삭제,수정,조회,페이징,필터링,정렬,그룹핑,집계,조인,서브쿼리,뷰,프로시저,함수,트리거,인덱스,파티션,샤딩,레플리케이션,클러스터링,로드밸런싱,페일오버,백업,복구,마이그레이션,리팩토링,테스트,단위테스트,통합테스트,엔드투엔드,모킹,스터빙,커버리지,품질,코드리뷰,페어프로그래밍,애자일,스크럼,칸반,데브옵스,씨아이,씨디,깃,깃허브,깃랩,비트버킷,젠킨스,도커,쿠버네티스,테라폼,앤서블,클라우드,아마존,구글,애저,서버리스,람다,함수,마이크로서비스,모놀리식,도메인,비즈니스,요구사항,설계,구현,테스트,배포,운영,유지보수';
    DECLARE words_array TEXT;
    DECLARE word_index INT;
    DECLARE i INT DEFAULT 0;

    WHILE i < word_count
        DO
            SET word_index = FLOOR(1 + RAND() * 150);
            SET words_array = SUBSTRING_INDEX(SUBSTRING_INDEX(korean_words, ',', word_index), ',', -1);
            SET result = CONCAT(result, words_array, ' ');
            SET i = i + 1;
        END WHILE;

    RETURN TRIM(result);
END$$

-- 더미 게시글 생성 프로시저
CREATE PROCEDURE IF NOT EXISTS generate_dummy_articles(IN batch_size INT, IN batch_count INT)
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

    -- 성능 최적화 설정
    SET SESSION bulk_insert_buffer_size = 256 * 1024 * 1024;
    SET SESSION unique_checks = 0;
    SET SESSION foreign_key_checks = 0;

    WHILE i < batch_count
        DO
            SET batch_start_time = NOW();
            SET j = 0;

            START TRANSACTION;

            WHILE j < batch_size
                DO
                    -- Article ID 생성 (연도-월-일-시퀀스)
                    SET article_id_val = CONCAT(
                            CASE FLOOR(RAND() * 3)
                                WHEN 0 THEN 'ART'
                                WHEN 1 THEN 'NTC'
                                ELSE 'EVT'
                                END,
                            DATE_FORMAT(DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY), '%Y%m%d'),
                            LPAD(i * batch_size + j, 6, '0')
                                         );

                    -- Article Type 결정 (70% REGULAR, 20% NOTICE, 10% EVENT)
                    SET article_type_val = CASE
                                               WHEN RAND() < 0.7 THEN 'REGULAR'
                                               WHEN RAND() < 0.9 THEN 'NOTICE'
                                               ELSE 'EVENT'
                        END;

                    -- 제목 생성 (한글 + 영문 조합)
                    SET title_val = CONCAT(
                            generate_korean_text(3 + FLOOR(RAND() * 5)),
                            ' - ',
                            generate_random_text(10, 30),
                            ' #', FLOOR(RAND() * 10000)
                                    );

                    -- 내용 생성 (긴 텍스트)
                    SET contents_val = CONCAT(
                            '<h2>', generate_korean_text(5), '</h2>\n',
                            '<p>', generate_korean_text(50 + FLOOR(RAND() * 100)), '</p>\n',
                            '<p>', generate_random_text(200, 500), '</p>\n',
                            '<ul>\n',
                            '<li>', generate_korean_text(10), '</li>\n',
                            '<li>', generate_korean_text(10), '</li>\n',
                            '<li>', generate_korean_text(10), '</li>\n',
                            '</ul>\n',
                            '<p>', generate_korean_text(30 + FLOOR(RAND() * 70)), '</p>\n',
                            '<blockquote>', generate_random_text(50, 150), '</blockquote>\n',
                            '<p>', generate_korean_text(100 + FLOOR(RAND() * 200)), '</p>'
                                       );

                    -- Writer ID 생성 (다양한 패턴)
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

                    -- Board ID (1-5 중 랜덤, 가중치 적용)
                    SET board_id_val = CASE
                                           WHEN RAND() < 0.4 THEN 1 -- 40% 자유게시판
                                           WHEN RAND() < 0.6 THEN 5 -- 20% 개발
                                           WHEN RAND() < 0.8 THEN 2 -- 20% 공지사항
                                           WHEN RAND() < 0.9 THEN 3 -- 10% 이벤트
                                           ELSE 4 -- 10% Q&A
                        END;

                    -- Status (95% ACTIVE, 3% DELETED, 2% BLOCKED)
                    SET status_val = CASE
                                         WHEN RAND() < 0.95 THEN 'ACTIVE'
                                         WHEN RAND() < 0.98 THEN 'DELETED'
                                         ELSE 'BLOCKED'
                        END;

                    -- View Count (지수 분포로 실제와 유사하게)
                    SET view_count_val = FLOOR(EXP(RAND() * LOG(100000)));

                    -- Created Date (최근 1년 내 랜덤)
                    SET created_date = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY);

                    -- Event dates (EVENT 타입인 경우)
                    IF article_type_val = 'EVENT' THEN
                        SET event_start = DATE_ADD(created_date, INTERVAL FLOOR(RAND() * 7) DAY);
                        SET event_end = DATE_ADD(event_start, INTERVAL 7 + FLOOR(RAND() * 30) DAY);
                    ELSE
                        SET event_start = NULL;
                        SET event_end = NULL;
                    END IF;

                    -- 데이터 삽입
                    INSERT INTO articles (article_id, article_type, title, contents,
                                          writer_id, board_id, status, view_count,
                                          event_start_date, event_end_date,
                                          created_at, updated_at)
                    VALUES (article_id_val, article_type_val, title_val, contents_val,
                            writer_id_val, board_id_val, status_val, view_count_val,
                            event_start, event_end,
                            created_date, created_date)
                    ON DUPLICATE KEY UPDATE article_id = article_id;

                    SET j = j + 1;
                    SET total_inserted = total_inserted + 1;
                END WHILE;
            COMMIT;

            -- 진행 상황 로그
            SELECT CONCAT('Batch ', i + 1, '/', batch_count, ' completed. Total inserted: ', total_inserted,
                          ' (Time: ', TIMESTAMPDIFF(SECOND, batch_start_time, NOW()), ' seconds)') AS progress;

            SET i = i + 1;

            -- 배치 간 짧은 대기 (DB 부하 방지)
            DO SLEEP(0.1);
        END WHILE;

    -- 설정 복구
    SET SESSION unique_checks = 1;
    SET SESSION foreign_key_checks = 1;

    SELECT CONCAT('Successfully generated ', total_inserted, ' dummy articles') AS result;
END$$

-- 키워드 매핑 생성 프로시저
CREATE PROCEDURE IF NOT EXISTS generate_keyword_mappings()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE article_id_val VARCHAR(50);
    DECLARE keyword_count INT;
    DECLARE keyword_id_val BIGINT;
    DECLARE i INT;
    DECLARE total_mappings INT DEFAULT 0;
    DECLARE batch_counter INT DEFAULT 0;

    DECLARE article_cursor CURSOR FOR
        SELECT article_id
        FROM articles
        WHERE article_id NOT IN (SELECT DISTINCT article_id FROM keyword_mappings)
        LIMIT 100000;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 성능 최적화
    SET SESSION unique_checks = 0;
    SET SESSION foreign_key_checks = 0;

    OPEN article_cursor;

    read_loop:
    LOOP
        FETCH article_cursor INTO article_id_val;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 각 게시글당 0-5개의 키워드 할당
        SET keyword_count = FLOOR(RAND() * 6);
        SET i = 0;

        WHILE i < keyword_count
            DO
                SET keyword_id_val = 1 + FLOOR(RAND() * 10); -- 1-10 키워드 중 선택

                INSERT IGNORE INTO keyword_mappings (article_id, keyword_id)
                VALUES (article_id_val, keyword_id_val);

                SET total_mappings = total_mappings + 1;
                SET i = i + 1;
            END WHILE;

        SET batch_counter = batch_counter + 1;

        -- 1000개마다 커밋
        IF batch_counter % 1000 = 0 THEN
            COMMIT;
            START TRANSACTION;
            SELECT CONCAT('Keyword mappings progress: ', batch_counter, ' articles processed') AS progress;
        END IF;
    END LOOP;

    CLOSE article_cursor;
    COMMIT;

    -- 설정 복구
    SET SESSION unique_checks = 1;
    SET SESSION foreign_key_checks = 1;

    SELECT CONCAT('Successfully generated ', total_mappings, ' keyword mappings') AS result;
END$$

-- 이미지 매핑 생성 프로시저
CREATE PROCEDURE IF NOT EXISTS generate_image_mappings()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE article_id_val VARCHAR(50);
    DECLARE image_count INT;
    DECLARE i INT;
    DECLARE total_images INT DEFAULT 0;
    DECLARE batch_counter INT DEFAULT 0;

    DECLARE article_cursor CURSOR FOR
        SELECT article_id
        FROM articles
        WHERE article_id NOT IN (SELECT DISTINCT article_id FROM article_images)
          AND RAND() < 0.3 -- 30% 게시글만 이미지 포함
        LIMIT 50000;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 성능 최적화
    SET SESSION unique_checks = 0;
    SET SESSION foreign_key_checks = 0;

    OPEN article_cursor;

    read_loop:
    LOOP
        FETCH article_cursor INTO article_id_val;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 각 게시글당 1-3개의 이미지
        SET image_count = 1 + FLOOR(RAND() * 3);
        SET i = 0;

        WHILE i < image_count
            DO
                INSERT INTO article_images (article_id, image_url, display_order)
                VALUES (article_id_val,
                        CONCAT('https://picsum.photos/800/600?random=', UUID()),
                        i + 1);

                SET total_images = total_images + 1;
                SET i = i + 1;
            END WHILE;

        SET batch_counter = batch_counter + 1;

        -- 1000개마다 커밋
        IF batch_counter % 1000 = 0 THEN
            COMMIT;
            START TRANSACTION;
            SELECT CONCAT('Image mappings progress: ', batch_counter, ' articles processed') AS progress;
        END IF;
    END LOOP;

    CLOSE article_cursor;
    COMMIT;

    -- 설정 복구
    SET SESSION unique_checks = 1;
    SET SESSION foreign_key_checks = 1;

    SELECT CONCAT('Successfully generated ', total_images, ' image mappings') AS result;
END$$

-- 통계 테이블 생성 (성능 테스트용)
CREATE TABLE IF NOT EXISTS article_statistics
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    total_articles   BIGINT,
    active_articles  BIGINT,
    deleted_articles BIGINT,
    blocked_articles BIGINT,
    total_views      BIGINT,
    avg_views        DECIMAL(10, 2),
    max_views        BIGINT,
    min_views        BIGINT,
    total_keywords   BIGINT,
    total_images     BIGINT,
    generation_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 통계 업데이트 프로시저
CREATE PROCEDURE IF NOT EXISTS update_statistics()
BEGIN
    INSERT INTO article_statistics (total_articles,
                                    active_articles,
                                    deleted_articles,
                                    blocked_articles,
                                    total_views,
                                    avg_views,
                                    max_views,
                                    min_views,
                                    total_keywords,
                                    total_images)
    SELECT COUNT(*)                                            as total_articles,
           SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END)  as active_articles,
           SUM(CASE WHEN status = 'DELETED' THEN 1 ELSE 0 END) as deleted_articles,
           SUM(CASE WHEN status = 'BLOCKED' THEN 1 ELSE 0 END) as blocked_articles,
           SUM(view_count)                                     as total_views,
           AVG(view_count)                                     as avg_views,
           MAX(view_count)                                     as max_views,
           MIN(view_count)                                     as min_views,
           (SELECT COUNT(*) FROM keyword_mappings)             as total_keywords,
           (SELECT COUNT(*) FROM article_images)               as total_images
    FROM articles;

    SELECT * FROM article_statistics ORDER BY generation_date DESC LIMIT 1;
END$$

DELIMITER ;

-- 실행 스크립트
SELECT '========================================' AS '';
SELECT 'Starting Dummy Data Generation' AS '';
SELECT '========================================' AS '';
SELECT NOW() AS start_time;

-- 60만개 데이터 생성 (1000개씩 600번)
-- 배치 크기와 횟수는 서버 성능에 따라 조정 가능
CALL generate_dummy_articles(1000, 600);

-- 키워드 매핑 생성
SELECT 'Generating keyword mappings...' AS '';
CALL generate_keyword_mappings();

-- 이미지 매핑 생성
SELECT 'Generating image mappings...' AS '';
CALL generate_image_mappings();

-- 통계 업데이트
SELECT 'Updating statistics...' AS '';
CALL update_statistics();

-- 인덱스 재구축 (성능 최적화)
SELECT 'Optimizing indexes...' AS '';
ANALYZE TABLE articles;
ANALYZE TABLE keyword_mappings;
ANALYZE TABLE article_images;

SELECT '========================================' AS '';
SELECT 'Dummy Data Generation Complete!' AS '';
SELECT '========================================' AS '';
SELECT NOW() AS end_time;

-- 최종 카운트 확인
SELECT 'Total Articles' as metric,
       COUNT(*)         as count
FROM articles
UNION ALL
SELECT 'Total Keyword Mappings' as metric,
       COUNT(*)                 as count
FROM keyword_mappings
UNION ALL
SELECT 'Total Article Images' as metric,
       COUNT(*)               as count
FROM article_images;

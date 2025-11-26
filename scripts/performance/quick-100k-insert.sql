-- 빠른 10만개 데이터 생성 (단순 반복 INSERT)
SET FOREIGN_KEY_CHECKS = 0;
SET autocommit = 0;

-- 숫자 테이블 생성 (1~100000)
DROP TABLE IF EXISTS temp_numbers;
CREATE TEMPORARY TABLE temp_numbers
(
    num INT PRIMARY KEY
);

-- 1~10 삽입
INSERT INTO temp_numbers
VALUES (1),
       (2),
       (3),
       (4),
       (5),
       (6),
       (7),
       (8),
       (9),
       (10);

-- 10 -> 100
INSERT INTO temp_numbers
SELECT a.num + b.num * 10
FROM temp_numbers a
         CROSS JOIN temp_numbers b
WHERE a.num + b.num * 10 <= 100;

-- 100 -> 10000
INSERT INTO temp_numbers
SELECT a.num + b.num * 100
FROM temp_numbers a
         CROSS JOIN temp_numbers b
WHERE b.num <= 99
  AND a.num + b.num * 100 <= 10000;

-- 10000 -> 100000
INSERT INTO temp_numbers
SELECT a.num + b.num * 10000
FROM temp_numbers a
         CROSS JOIN temp_numbers b
WHERE b.num <= 9
  AND a.num + b.num * 10000 <= 100000;

-- Articles 생성 (기존 10K + 새로운 90K = 100K)
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
SELECT CONCAT('PERF', LPAD(num + 10000, 10, '0')),
       CASE
           WHEN num % 100 < 90 THEN 'RegularArticle'
           WHEN num % 100 < 95 THEN 'NoticeArticle'
           ELSE 'EventArticle'
           END,
       CONCAT(
               CASE num % 10
                   WHEN 0 THEN 'Spring Boot 성능 최적화 가이드 #'
                   WHEN 1 THEN 'JPA N+1 문제 해결 방법 #'
                   WHEN 2 THEN 'MSA 아키텍처 설계 패턴 #'
                   WHEN 3 THEN '도메인 주도 설계 실전 #'
                   WHEN 4 THEN 'Redis 캐싱 전략 #'
                   WHEN 5 THEN 'Kafka 이벤트 스트리밍 #'
                   WHEN 6 THEN 'Docker 컨테이너 최적화 #'
                   WHEN 7 THEN 'Kubernetes 배포 전략 #'
                   WHEN 8 THEN 'AWS 클라우드 마이그레이션 #'
                   ELSE '성능 튜닝 케이스 스터디 #'
                   END,
               num
       ),
       CONCAT(
               '이것은 성능 테스트를 위한 본문입니다. 게시글 번호: ', num, '\n',
               '실제 운영 환경과 유사한 텍스트 길이를 만들기 위해 충분한 내용을 포함합니다.\n',
               'Lorem ipsum dolor sit amet, consectetur adipiscing elit. ',
               'Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.'
       ),
       CONCAT('user_', MOD(num, 1000)),
       1 + MOD(num, 5),
       'ACTIVE',
       CONCAT('https://cdn.example.com/img/', num, '.jpg'),
       MOD(num * 7, 10000),
       -- 시간 분포: 오래된 20% (1-20000), 중간 60% (20001-80000), 최신 20% (80001-100000)
       CASE
           WHEN num <= 20000 THEN DATE_SUB(NOW(), INTERVAL (300 + MOD(num, 65)) DAY)
           WHEN num <= 80000 THEN DATE_SUB(NOW(), INTERVAL (30 + MOD(num, 270)) DAY)
           ELSE DATE_SUB(NOW(), INTERVAL MOD(num, 30) DAY)
           END,
       NOW(),
       0
FROM temp_numbers
WHERE num <= 90000;

COMMIT;

-- 통계 업데이트
ANALYZE TABLE articles;

-- 최종 확인
SELECT 'Data Generation Complete!'                                                as Status,
       COUNT(*)                                                                   as Total_Articles,
       COUNT(CASE WHEN created_at > DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END)  as Recent_30_Days,
       COUNT(CASE
                 WHEN created_at BETWEEN DATE_SUB(NOW(), INTERVAL 300 DAY) AND DATE_SUB(NOW(), INTERVAL 30 DAY)
                     THEN 1 END)                                                  as Middle_Period,
       COUNT(CASE WHEN created_at < DATE_SUB(NOW(), INTERVAL 300 DAY) THEN 1 END) as Older_300_Days
FROM articles;

DROP TABLE temp_numbers;

SET FOREIGN_KEY_CHECKS = 1;
SET autocommit = 1;

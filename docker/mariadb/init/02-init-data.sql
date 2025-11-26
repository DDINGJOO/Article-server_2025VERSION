-- 테스트 데이터 삽입
USE article_db;

-- 게시판 데이터
INSERT INTO boards (id, name, description)
VALUES (1, '자유게시판', '자유롭게 글을 작성할 수 있는 게시판입니다.'),
       (2, '공지사항', '공지사항을 등록하는 게시판입니다.'),
       (3, '이벤트', '이벤트 관련 게시판입니다.'),
       (4, '질문답변', 'Q&A 게시판입니다.'),
       (5, '개발', '개발 관련 게시판입니다.')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 키워드 데이터
INSERT INTO keywords (id, name, description)
VALUES (1, '중요', '중요한 내용을 표시합니다'),
       (2, '공지', '공지사항을 표시합니다'),
       (3, '이벤트', '이벤트 관련 내용입니다'),
       (4, '긴급', '긴급한 내용입니다'),
       (5, 'Spring', 'Spring Framework 관련'),
       (6, 'Docker', 'Docker 관련'),
       (7, 'Kubernetes', 'Kubernetes 관련'),
       (8, 'Database', '데이터베이스 관련'),
       (9, 'Performance', '성능 관련'),
       (10, 'Security', '보안 관련')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 샘플 게시글 데이터 (일반 게시글)
INSERT INTO articles (article_id, article_type, title, contents, writer_id, board_id, status, view_count)
VALUES ('ART20251126001', 'REGULAR', 'Spring Boot 3.5 새로운 기능', 'Spring Boot 3.5 버전에서 추가된 새로운 기능들을 소개합니다...',
        'dev_user1', 5, 'ACTIVE', 152),
       ('ART20251126002', 'REGULAR', 'Docker Compose 활용법', 'Docker Compose를 사용한 개발 환경 구성 방법입니다...', 'dev_user2', 5,
        'ACTIVE', 89),
       ('ART20251126003', 'REGULAR', 'JPA N+1 문제 해결', 'JPA에서 발생하는 N+1 문제를 해결하는 여러 가지 방법들...', 'dev_user1', 5, 'ACTIVE',
        234),
       ('ART20251126004', 'REGULAR', '자유게시판 첫 글', '안녕하세요, 자유게시판 첫 글입니다.', 'user123', 1, 'ACTIVE', 45),
       ('ART20251126005', 'REGULAR', 'Redis 캐싱 전략', 'Redis를 활용한 효율적인 캐싱 전략을 공유합니다...', 'dev_user3', 5, 'ACTIVE', 178)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- 샘플 게시글 데이터 (공지사항)
INSERT INTO articles (article_id, article_type, title, contents, writer_id, board_id, status, view_count)
VALUES ('NTC20251126001', 'NOTICE', '서비스 점검 공지', '11월 30일 새벽 2시부터 4시까지 서비스 점검이 있을 예정입니다...', 'admin', 2, 'ACTIVE', 523),
       ('NTC20251126002', 'NOTICE', '개인정보처리방침 변경 안내', '개인정보처리방침이 다음과 같이 변경됩니다...', 'admin', 2, 'ACTIVE', 342),
       ('NTC20251126003', 'NOTICE', '신규 기능 업데이트', '새로운 기능들이 추가되었습니다. 자세한 내용은...', 'admin', 2, 'ACTIVE', 456)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- 샘플 게시글 데이터 (이벤트)
INSERT INTO articles (article_id, article_type, title, contents, writer_id, board_id, status, view_count,
                      event_start_date, event_end_date)
VALUES ('EVT20251126001', 'EVENT', '연말 감사 이벤트', '2025년 한 해 동안 감사했습니다. 특별 이벤트를 준비했습니다...', 'event_manager', 3, 'ACTIVE',
        892, '2025-12-01 00:00:00', '2025-12-31 23:59:59'),
       ('EVT20251126002', 'EVENT', '신규 회원 환영 이벤트', '신규 회원님들을 위한 특별한 혜택!', 'event_manager', 3, 'ACTIVE', 567,
        '2025-11-25 00:00:00', '2025-12-25 23:59:59'),
       ('EVT20251126003', 'EVENT', '개발자 컨퍼런스 할인', '개발자 컨퍼런스 참가 할인 이벤트입니다.', 'event_manager', 3, 'ACTIVE', 234,
        '2025-11-20 00:00:00', '2025-11-30 23:59:59')
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- 키워드 매핑 데이터
INSERT INTO keyword_mappings (article_id, keyword_id)
VALUES ('ART20251126001', 5),  -- Spring
       ('ART20251126002', 6),  -- Docker
       ('ART20251126003', 8),  -- Database
       ('ART20251126003', 9),  -- Performance
       ('ART20251126005', 9),  -- Performance
       ('NTC20251126001', 1),  -- 중요
       ('NTC20251126001', 2),  -- 공지
       ('NTC20251126002', 1),  -- 중요
       ('NTC20251126002', 2),  -- 공지
       ('NTC20251126002', 10), -- Security
       ('NTC20251126003', 2),  -- 공지
       ('EVT20251126001', 3),  -- 이벤트
       ('EVT20251126001', 1),  -- 중요
       ('EVT20251126002', 3),  -- 이벤트
       ('EVT20251126003', 3)   -- 이벤트
ON DUPLICATE KEY UPDATE keyword_id = VALUES(keyword_id);

-- 게시글 이미지 데이터
INSERT INTO article_images (article_id, image_url, display_order)
VALUES ('ART20251126001', 'https://example.com/images/spring-boot-3.5.jpg', 1),
       ('ART20251126002', 'https://example.com/images/docker-compose-1.jpg', 1),
       ('ART20251126002', 'https://example.com/images/docker-compose-2.jpg', 2),
       ('EVT20251126001', 'https://example.com/images/year-end-event.jpg', 1),
       ('EVT20251126002', 'https://example.com/images/welcome-event.jpg', 1)
ON DUPLICATE KEY UPDATE image_url = VALUES(image_url);

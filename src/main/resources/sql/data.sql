-- Article Server Initial Data for MariaDB
-- Version: 1.0
-- Description: 게시판 및 키워드 초기 데이터
-- Created: 2025-10-25

-- ==========================================
-- 1. Board 초기 데이터
-- ==========================================

-- 공지사항 게시판
INSERT INTO `boards` (`board_id`, `board_name`, `description`, `is_active`, `display_order`, `created_at`, `updated_at`)
VALUES (1, '공지사항', '중요한 공지사항 및 업데이트 소식을 확인하세요', TRUE, 1, NOW(6), NOW(6));

-- 자유게시판
INSERT INTO `boards` (`board_id`, `board_name`, `description`, `is_active`, `display_order`, `created_at`, `updated_at`)
VALUES (2, '자유게시판', '자유롭게 소통하고 의견을 나누는 공간입니다', TRUE, 2, NOW(6), NOW(6));

-- 질문 게시판
INSERT INTO `boards` (`board_id`, `board_name`, `description`, `is_active`, `display_order`, `created_at`, `updated_at`)
VALUES (3, '질문게시판', '궁금한 점을 질문하고 답변을 받아보세요', TRUE, 3, NOW(6), NOW(6));

-- 정보공유 게시판
INSERT INTO `boards` (`board_id`, `board_name`, `description`, `is_active`, `display_order`, `created_at`, `updated_at`)
VALUES (4, '정보공유', '유용한 정보와 노하우를 공유하는 공간입니다', TRUE, 4, NOW(6), NOW(6));

-- 이벤트 게시판
INSERT INTO `boards` (`board_id`, `board_name`, `description`, `is_active`, `display_order`, `created_at`, `updated_at`)
VALUES (5, '이벤트', '진행 중인 이벤트와 프로모션을 확인하세요', TRUE, 5, NOW(6), NOW(6));

-- 후기 게시판
INSERT INTO `boards` (`board_id`, `board_name`, `description`, `is_active`, `display_order`, `created_at`, `updated_at`)
VALUES (6, '후기게시판', '사용 후기와 경험담을 공유해주세요', TRUE, 6, NOW(6), NOW(6));

-- ==========================================
-- 2. Common Keywords (공통 키워드 - board_id = NULL)
-- ==========================================

-- 중요도 관련 키워드
INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (1, '공지', NULL, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (2, '중요', NULL, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (3, '긴급', NULL, 0, TRUE);

-- 상태 관련 키워드
INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (4, '진행중', NULL, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (5, '완료', NULL, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (6, '대기', NULL, 0, TRUE);

-- 일반 카테고리 키워드
INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (7, '팁', NULL, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (8, '가이드', NULL, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (9, '이슈', NULL, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (10, '피드백', NULL, 0, TRUE);

-- ==========================================
-- 3. Board-specific Keywords (게시판 전용 키워드)
-- ==========================================

-- 공지사항 게시판 전용 키워드 (board_id = 1)
INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (101, '시스템점검', 1, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (102, '업데이트', 1, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (103, '정책변경', 1, 0, TRUE);

-- 자유게시판 전용 키워드 (board_id = 2)
INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (201, '일상', 2, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (202, '잡담', 2, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (203, '추천', 2, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (204, '토론', 2, 0, TRUE);

-- 질문게시판 전용 키워드 (board_id = 3)
INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (301, '질문', 3, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (302, '답변완료', 3, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (303, '미해결', 3, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (304, '해결', 3, 0, TRUE);

-- 정보공유 게시판 전용 키워드 (board_id = 4)
INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (401, '튜토리얼', 4, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (402, '노하우', 4, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (403, '개발', 4, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (404, '디자인', 4, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (405, '기획', 4, 0, TRUE);

-- 이벤트 게시판 전용 키워드 (board_id = 5)
INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (501, '이벤트', 5, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (502, '프로모션', 5, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (503, '경품', 5, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (504, '할인', 5, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (505, '참여', 5, 0, TRUE);

-- 후기게시판 전용 키워드 (board_id = 6)
INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (601, '후기', 6, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (602, '만족', 6, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (603, '개선필요', 6, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (604, '추천함', 6, 0, TRUE);

INSERT INTO `keywords` (`keyword_id`, `keyword_name`, `board_id`, `usage_count`, `is_active`)
VALUES (605, '비추천', 6, 0, TRUE);

-- ==========================================
-- AUTO_INCREMENT 시작값 설정
-- ==========================================

-- Board는 7부터 시작 (1-6 사용됨)
ALTER TABLE `boards`
    AUTO_INCREMENT = 7;

-- 공통 키워드는 11부터, 게시판별 키워드는 각 범위 유지
-- Keywords는 700부터 시작 (향후 확장 가능)
ALTER TABLE `keywords`
    AUTO_INCREMENT = 700;

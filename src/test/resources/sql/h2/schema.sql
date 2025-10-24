-- Article Server Schema for H2 (Test Environment)
-- Version: 1.0
-- Description: H2 테스트 데이터베이스 스키마 정의
-- Created: 2025-10-25

-- ==========================================
-- 1. Board Table (게시판)
-- ==========================================
CREATE TABLE IF NOT EXISTS boards
(
    board_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '게시판 ID',
    board_name    VARCHAR(50)  NOT NULL COMMENT '게시판 이름',
    description   VARCHAR(200) NULL COMMENT '게시판 설명',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '게시판 활성화 여부',
    display_order INT          NULL COMMENT '게시판 표시 순서',
    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (board_id)
);

CREATE UNIQUE INDEX uk_board_name ON boards (board_name);
CREATE INDEX idx_board_name ON boards (board_name);
CREATE INDEX idx_board_active ON boards (is_active);

-- ==========================================
-- 2. Keyword Table (키워드)
-- ==========================================
CREATE TABLE IF NOT EXISTS keywords
(
    keyword_id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '키워드 ID',
    keyword_name       VARCHAR(50) NOT NULL COMMENT '키워드 이름',
    board_id           BIGINT      NULL COMMENT '게시판 ID (NULL인 경우 공통 키워드)',
    usage_count        INT         NOT NULL DEFAULT 0 COMMENT '사용 빈도',
    is_active          BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '키워드 활성화 여부',
    board_id_coalesced BIGINT GENERATED ALWAYS AS (COALESCE(board_id, -1)) COMMENT 'NULL-safe board_id for unique constraint',
    PRIMARY KEY (keyword_id),
    CONSTRAINT fk_keyword_board
        FOREIGN KEY (board_id)
            REFERENCES boards (board_id)
            ON UPDATE CASCADE
            ON DELETE SET NULL
);

-- 키워드 유니크 제약: (keyword_name, board_id_coalesced) 조합이 유일해야 함
-- board_id_coalesced는 NULL을 -1로 변환하므로 공통 키워드도 중복 방지됨
CREATE UNIQUE INDEX uk_keyword_board ON keywords (keyword_name, board_id_coalesced);
CREATE INDEX idx_keyword_board ON keywords (board_id);
CREATE INDEX idx_keyword_name ON keywords (keyword_name);

-- ==========================================
-- 3. Article Table (게시글)
-- Single Table Inheritance 전략 사용
-- ==========================================
CREATE TABLE IF NOT EXISTS articles
(
    article_id       VARCHAR(50)  NOT NULL COMMENT '게시글 ID',
    article_type     VARCHAR(50)  NOT NULL COMMENT '게시글 타입 (RegularArticle/EventArticle/NoticeArticle)',
    title            VARCHAR(200) NOT NULL COMMENT '제목',
    contents         TEXT         NOT NULL COMMENT '내용',
    writer_id        VARCHAR(50)  NOT NULL COMMENT '작성자 ID',
    board_id         BIGINT       NOT NULL COMMENT '게시판 ID',
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE/DELETED/BLOCKED)',
    version          BIGINT       NULL     DEFAULT 0 COMMENT 'Optimistic Lock 버전',
    view_count       BIGINT       NOT NULL DEFAULT 0 COMMENT '조회수',
    first_image_url  VARCHAR(500) NULL COMMENT '대표 이미지 URL',
    event_start_date TIMESTAMP(6) NULL COMMENT '이벤트 시작일 (EventArticle 전용)',
    event_end_date   TIMESTAMP(6) NULL COMMENT '이벤트 종료일 (EventArticle 전용)',
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (article_id),

    CONSTRAINT fk_articles_board
        FOREIGN KEY (board_id)
            REFERENCES boards (board_id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);

-- 단일 인덱스
CREATE INDEX idx_article_board ON articles (board_id);
CREATE INDEX idx_article_writer ON articles (writer_id);

-- 복합 인덱스
CREATE INDEX idx_status_created_id ON articles (status, created_at, article_id);
CREATE INDEX idx_status_updated_id ON articles (status, updated_at, article_id);
CREATE INDEX idx_board_status_created ON articles (board_id, status, created_at);
CREATE INDEX idx_writer_status_created ON articles (writer_id, status, created_at);
CREATE INDEX idx_type_status_created ON articles (article_type, status, created_at);
CREATE INDEX idx_event_status_dates ON articles (article_type, status, event_start_date, event_end_date);
CREATE INDEX idx_event_status_end ON articles (article_type, status, event_end_date);
CREATE INDEX idx_event_status_start ON articles (article_type, status, event_start_date);

-- ==========================================
-- 4. Article Image Table (게시글 이미지)
-- ==========================================
CREATE TABLE IF NOT EXISTS article_images
(
    article_id        VARCHAR(50)  NOT NULL COMMENT '게시글 ID',
    sequence_num      BIGINT       NOT NULL COMMENT '이미지 순서',
    article_image_url VARCHAR(500) NOT NULL COMMENT '이미지 URL',
    image_id          VARCHAR(100) NOT NULL COMMENT 'Image Server의 이미지 ID',
    PRIMARY KEY (article_id, sequence_num),

    CONSTRAINT fk_article_images_article
        FOREIGN KEY (article_id)
            REFERENCES articles (article_id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

CREATE INDEX idx_article_image_article ON article_images (article_id);
CREATE INDEX idx_article_image_id ON article_images (image_id);

-- ==========================================
-- 5. Keyword Mapping Table (게시글-키워드 매핑)
-- ==========================================
CREATE TABLE IF NOT EXISTS keyword_mapping_table
(
    keyword_id BIGINT       NOT NULL COMMENT '키워드 ID',
    article_id VARCHAR(50)  NOT NULL COMMENT '게시글 ID',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '매핑 생성일시',
    PRIMARY KEY (keyword_id, article_id),

    CONSTRAINT fk_kmt_keyword
        FOREIGN KEY (keyword_id)
            REFERENCES keywords (keyword_id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT fk_kmt_article
        FOREIGN KEY (article_id)
            REFERENCES articles (article_id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

CREATE INDEX idx_mapping_article ON keyword_mapping_table (article_id);
CREATE INDEX idx_mapping_keyword ON keyword_mapping_table (keyword_id);
CREATE INDEX idx_mapping_created ON keyword_mapping_table (created_at);

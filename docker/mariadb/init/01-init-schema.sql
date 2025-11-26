-- Article Server 데이터베이스 스키마 초기화
-- Docker 컨테이너 시작 시 자동 실행됩니다

-- 데이터베이스 생성 (이미 docker-compose에서 생성되지만 안전을 위해)
CREATE DATABASE IF NOT EXISTS article_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE article_db;

-- 게시판 테이블
CREATE TABLE IF NOT EXISTS boards
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_board_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 키워드 테이블
CREATE TABLE IF NOT EXISTS keywords
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_keyword_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 게시글 테이블 (Single Table Inheritance)
CREATE TABLE IF NOT EXISTS articles
(
    article_id       VARCHAR(50) PRIMARY KEY,
    article_type     VARCHAR(20)  NOT NULL,                  -- REGULAR, EVENT, NOTICE
    title            VARCHAR(200) NOT NULL,
    contents         TEXT         NOT NULL,
    writer_id        VARCHAR(50)  NOT NULL,
    board_id         BIGINT       NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DELETED, BLOCKED
    view_count       BIGINT                DEFAULT 0,

    -- Event Article 전용 필드
    event_start_date DATETIME     NULL,
    event_end_date   DATETIME     NULL,

    -- 메타데이터
    created_at       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP    NULL,

    -- 외래키
    CONSTRAINT fk_article_board FOREIGN KEY (board_id) REFERENCES boards (id),

    -- 인덱스 (성능 최적화)
    INDEX idx_status_created_id (status, created_at DESC, article_id),
    INDEX idx_status_updated_id (status, updated_at DESC, article_id),
    INDEX idx_board_status_created (board_id, status, created_at DESC),
    INDEX idx_writer_status_created (writer_id, status, created_at DESC),
    INDEX idx_type_status_created (article_type, status, created_at DESC),
    INDEX idx_event_status_dates (article_type, status, event_start_date, event_end_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 게시글-키워드 매핑 테이블
CREATE TABLE IF NOT EXISTS keyword_mappings
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id VARCHAR(50) NOT NULL,
    keyword_id BIGINT      NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mapping_article FOREIGN KEY (article_id) REFERENCES articles (article_id) ON DELETE CASCADE,
    CONSTRAINT fk_mapping_keyword FOREIGN KEY (keyword_id) REFERENCES keywords (id),

    UNIQUE KEY uk_article_keyword (article_id, keyword_id),
    INDEX idx_keyword_mapping_article (article_id),
    INDEX idx_keyword_mapping_keyword (keyword_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 게시글 이미지 테이블
CREATE TABLE IF NOT EXISTS article_images
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id    VARCHAR(50)  NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    display_order INT       DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_image_article FOREIGN KEY (article_id) REFERENCES articles (article_id) ON DELETE CASCADE,

    INDEX idx_image_article (article_id),
    INDEX idx_image_order (article_id, display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

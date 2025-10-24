-- Article Server Schema for MariaDB
-- Version: 1.0
-- Description: 게시글 관리 시스템 스키마 정의
-- Created: 2025-10-25

-- ==========================================
-- 1. Board Table (게시판)
-- ==========================================
CREATE TABLE IF NOT EXISTS `boards`
(
    `board_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '게시판 ID',
    `board_name`    VARCHAR(50)  NOT NULL COMMENT '게시판 이름',
    `description`   VARCHAR(200) NULL COMMENT '게시판 설명',
    `is_active`     BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '게시판 활성화 여부',
    `display_order` INT          NULL COMMENT '게시판 표시 순서',
    `created_at`    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    `updated_at`    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (`board_id`),
    UNIQUE KEY `uk_board_name` (`board_name`),
    INDEX `idx_board_name` (`board_name`),
    INDEX `idx_board_active` (`is_active`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='게시판 테이블';

-- ==========================================
-- 2. Keyword Table (키워드)
-- ==========================================
CREATE TABLE IF NOT EXISTS `keywords`
(
    `keyword_id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '키워드 ID',
    `keyword_name` VARCHAR(50) NOT NULL COMMENT '키워드 이름',
    `board_id`     BIGINT      NULL COMMENT '게시판 ID (NULL인 경우 공통 키워드)',
    `usage_count`  INT         NOT NULL DEFAULT 0 COMMENT '사용 빈도',
    `is_active`    BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '키워드 활성화 여부',
    PRIMARY KEY (`keyword_id`),
    UNIQUE KEY `uk_keyword_board` (`keyword_name`, `board_id`),
    INDEX `idx_keyword_board` (`board_id`),
    INDEX `idx_keyword_name` (`keyword_name`),
    CONSTRAINT `fk_keyword_board`
        FOREIGN KEY (`board_id`)
            REFERENCES `boards` (`board_id`)
            ON UPDATE CASCADE
            ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='키워드 테이블 (공통 키워드 + 게시판 전용 키워드)';

-- ==========================================
-- 3. Article Table (게시글)
-- Single Table Inheritance 전략 사용
-- ==========================================
CREATE TABLE IF NOT EXISTS `articles`
(
    `article_id`       VARCHAR(50)  NOT NULL COMMENT '게시글 ID',
    `article_type`     VARCHAR(50)  NOT NULL COMMENT '게시글 타입 (RegularArticle/EventArticle/NoticeArticle)',
    `title`            VARCHAR(200) NOT NULL COMMENT '제목',
    `contents`         TEXT         NOT NULL COMMENT '내용',
    `writer_id`        VARCHAR(50)  NOT NULL COMMENT '작성자 ID',
    `board_id`         BIGINT       NOT NULL COMMENT '게시판 ID',
    `status`           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE/DELETED/BLOCKED)',
    `version`          BIGINT       NULL     DEFAULT 0 COMMENT 'Optimistic Lock 버전',
    `view_count`       BIGINT       NOT NULL DEFAULT 0 COMMENT '조회수',
    `first_image_url`  VARCHAR(500) NULL COMMENT '대표 이미지 URL',
    `event_start_date` DATETIME(6)  NULL COMMENT '이벤트 시작일 (EventArticle 전용)',
    `event_end_date`   DATETIME(6)  NULL COMMENT '이벤트 종료일 (EventArticle 전용)',
    `created_at`       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    `updated_at`       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (`article_id`),

    -- Foreign Keys
    CONSTRAINT `fk_articles_board`
        FOREIGN KEY (`board_id`)
            REFERENCES `boards` (`board_id`)
            ON UPDATE CASCADE
            ON DELETE RESTRICT,

    -- 단일 인덱스 (기본 조회용)
    INDEX `idx_article_board` (`board_id`),
    INDEX `idx_article_writer` (`writer_id`),

    -- 복합 인덱스 (성능 최적화)
    -- 1. 상태별 최신순 조회 (가장 자주 사용)
    INDEX `idx_status_created_id` (`status`, `created_at`, `article_id`),

    -- 2. 커서 페이징용 (updated_at 기준)
    INDEX `idx_status_updated_id` (`status`, `updated_at`, `article_id`),

    -- 3. 게시판별 상태 및 최신순 조회
    INDEX `idx_board_status_created` (`board_id`, `status`, `created_at`),

    -- 4. 작성자별 상태 및 최신순 조회
    INDEX `idx_writer_status_created` (`writer_id`, `status`, `created_at`),

    -- 5. 타입별 조회 (Single Table Inheritance)
    INDEX `idx_type_status_created` (`article_type`, `status`, `created_at`),

    -- 6. EventArticle 전용 인덱스
    -- 진행중 이벤트 조회: WHERE status = ? AND now BETWEEN start AND end
    INDEX `idx_event_status_dates` (`article_type`, `status`, `event_start_date`, `event_end_date`),

    -- 종료된 이벤트 조회: WHERE status = ? AND end < now
    INDEX `idx_event_status_end` (`article_type`, `status`, `event_end_date`),

    -- 예정 이벤트 조회: WHERE status = ? AND start > now
    INDEX `idx_event_status_start` (`article_type`, `status`, `event_start_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='게시글 테이블 (Single Table Inheritance)';

-- ==========================================
-- 4. Article Image Table (게시글 이미지)
-- ==========================================
CREATE TABLE IF NOT EXISTS `article_images`
(
    `article_id`        VARCHAR(50)  NOT NULL COMMENT '게시글 ID',
    `sequence_num`      BIGINT       NOT NULL COMMENT '이미지 순서',
    `article_image_url` VARCHAR(500) NOT NULL COMMENT '이미지 URL',
    `image_id`          VARCHAR(100) NOT NULL COMMENT 'Image Server의 이미지 ID',
    PRIMARY KEY (`article_id`, `sequence_num`),

    -- Foreign Keys
    CONSTRAINT `fk_article_images_article`
        FOREIGN KEY (`article_id`)
            REFERENCES `articles` (`article_id`)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    -- Indexes
    INDEX `idx_article_image_article` (`article_id`),
    INDEX `idx_article_image_id` (`image_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='게시글 이미지 테이블';

-- ==========================================
-- 5. Keyword Mapping Table (게시글-키워드 매핑)
-- ==========================================
CREATE TABLE IF NOT EXISTS `keyword_mapping_table`
(
    `keyword_id` BIGINT      NOT NULL COMMENT '키워드 ID',
    `article_id` VARCHAR(50) NOT NULL COMMENT '게시글 ID',
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '매핑 생성일시',
    PRIMARY KEY (`keyword_id`, `article_id`),

    -- Foreign Keys
    CONSTRAINT `fk_kmt_keyword`
        FOREIGN KEY (`keyword_id`)
            REFERENCES `keywords` (`keyword_id`)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT `fk_kmt_article`
        FOREIGN KEY (`article_id`)
            REFERENCES `articles` (`article_id`)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    -- Indexes
    INDEX `idx_mapping_article` (`article_id`),
    INDEX `idx_mapping_keyword` (`keyword_id`),
    INDEX `idx_mapping_created` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='게시글-키워드 매핑 테이블';

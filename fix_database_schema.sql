-- Database Schema Migration Script
-- Purpose: Align database schema with JPA entity definitions
-- Date: 2025-11-30

USE articles;

-- Step 1: Drop foreign key constraints that reference the columns we're going to rename
ALTER TABLE articles
    DROP FOREIGN KEY fk_articles_board;
ALTER TABLE keyword_mapping_table
    DROP FOREIGN KEY fk_kmt_keyword;
ALTER TABLE keywords
    DROP FOREIGN KEY fk_keyword_board;

-- Step 2: Remove unnecessary 'value' column from articles table
ALTER TABLE articles
    DROP COLUMN IF EXISTS value;

-- Step 3: Rename board_id to id in boards table
ALTER TABLE boards
    CHANGE COLUMN board_id id BIGINT AUTO_INCREMENT COMMENT '게시판 ID';

-- Step 4: Rename keyword_id to id in keywords table
ALTER TABLE keywords
    CHANGE COLUMN keyword_id id BIGINT AUTO_INCREMENT COMMENT '키워드 ID';

-- Step 5: Update foreign key column names in referencing tables
ALTER TABLE articles
    CHANGE COLUMN board_id board_id BIGINT NOT NULL COMMENT '게시판 ID';
ALTER TABLE keywords
    CHANGE COLUMN board_id board_id BIGINT NULL COMMENT '게시판 ID (NULL인 경우 공통 키워드)';
ALTER TABLE keyword_mapping_table
    CHANGE COLUMN keyword_id keyword_id BIGINT NOT NULL COMMENT '키워드 ID';

-- Step 6: Re-create foreign key constraints with updated references
ALTER TABLE articles
    ADD CONSTRAINT fk_articles_board
        FOREIGN KEY (board_id) REFERENCES boards (id)
            ON UPDATE CASCADE;

ALTER TABLE keywords
    ADD CONSTRAINT fk_keyword_board
        FOREIGN KEY (board_id) REFERENCES boards (id)
            ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE keyword_mapping_table
    ADD CONSTRAINT fk_kmt_keyword
        FOREIGN KEY (keyword_id) REFERENCES keywords (id)
            ON UPDATE CASCADE ON DELETE CASCADE;

-- Step 7: Verify the changes
SELECT 'Boards table columns:' as Info;
SHOW COLUMNS FROM boards;

SELECT 'Keywords table columns:' as Info;
SHOW COLUMNS FROM keywords;

SELECT 'Articles table columns (value column should be removed):' as Info;
SHOW COLUMNS FROM articles;

SELECT 'Foreign key constraints:' as Info;
SELECT CONSTRAINT_NAME,
       TABLE_NAME,
       COLUMN_NAME,
       REFERENCED_TABLE_NAME,
       REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'articles'
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

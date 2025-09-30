-- SQL
CREATE TABLE boards (
                        board_id INT NOT NULL AUTO_INCREMENT,
                        board_name VARCHAR(100) NOT NULL,
                        PRIMARY KEY (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE articles (
                          article_id VARCHAR(100) NOT NULL,
                          contents   TEXT         NOT NULL,
                          writer_id  VARCHAR(100) NOT NULL,
                          version INT,
                          board_id   INT          NOT NULL,
                          title      VARCHAR(100) NOT NULL,
                          status     VARCHAR(100) NOT NULL,
                          created_at DATETIME     NOT NULL,
                          updated_at DATETIME     NOT NULL,
                          PRIMARY KEY (article_id),
                          CONSTRAINT fk_articles_board
                              FOREIGN KEY (board_id) REFERENCES boards (board_id)
                                  ON DELETE RESTRICT
                                  ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_images (
                                article_id VARCHAR(100) NOT NULL,
                                article_image_url VARCHAR(255) NOT NULL,
                                sequence_no INT NOT NULL,
                                PRIMARY KEY (article_id, sequence_no),
                                CONSTRAINT fk_article_images_article
                                    FOREIGN KEY (article_id) REFERENCES articles (article_id)
                                        ON DELETE CASCADE
                                        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE keywords (
                          keyword_id INT NOT NULL,
                          keyword_name VARCHAR(100) NOT NULL,
                          PRIMARY KEY (keyword_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE keyword_mapping_table (
                                       keyword_id INT NOT NULL,
                                       article_id VARCHAR(100) NOT NULL,
                                       PRIMARY KEY (keyword_id, article_id),
                                       CONSTRAINT fk_kmt_keyword
                                           FOREIGN KEY (keyword_id) REFERENCES keywords (keyword_id)
                                               ON DELETE CASCADE
                                               ON UPDATE CASCADE,
                                       CONSTRAINT fk_kmt_article
                                           FOREIGN KEY (article_id) REFERENCES articles (article_id)
                                               ON DELETE CASCADE
                                               ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

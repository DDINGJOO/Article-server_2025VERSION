create table boards
(
    board_id   int auto_increment
        primary key,
    board_name varchar(100) not null
)
    collate = utf8mb4_unicode_ci;

create table articles
(
    article_id       varchar(100) not null
        primary key,
    contents         text         null,
    writer_id        varchar(100) null,
    version          int          null,
    board_id         int          null,
    status           varchar(100) null,
    created_at       datetime     null,
    updated_at       datetime     null,
    title            varchar(100) not null,
    article_type     varchar(50)  null,
    first_image_url  varchar(255) null,
    event_start_date datetime     null,
    event_end_date   datetime     null,
    constraint fk_articles_board
        foreign key (board_id) references boards (board_id)
            on update cascade on delete set null
)
    collate = utf8mb4_unicode_ci;

create table article_images
(
    article_id        varchar(100) not null,
    article_image_url varchar(255) null,
    sequence_no       int          not null,
    image_id varchar(100) null,
    primary key (article_id, sequence_no),
    constraint fk_article_images_article
        foreign key (article_id) references articles (article_id)
            on update cascade on delete cascade
)
    collate = utf8mb4_unicode_ci;

create table keywords
(
    keyword_id   int          not null
        primary key,
    keyword_name varchar(100) not null
)
    collate = utf8mb4_unicode_ci;

create table keyword_mapping_table
(
    keyword_id int          not null,
    article_id varchar(100) not null,
    primary key (keyword_id, article_id),
    constraint fk_kmt_article
        foreign key (article_id) references articles (article_id)
            on update cascade on delete cascade,
    constraint fk_kmt_keyword
        foreign key (keyword_id) references keywords (keyword_id)
            on update cascade on delete cascade
)
    collate = utf8mb4_unicode_ci;


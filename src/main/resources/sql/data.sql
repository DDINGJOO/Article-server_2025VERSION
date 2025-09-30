-- Initial seed data for boards and keywords

-- Boards
INSERT INTO boards (board_id, board_name)
VALUES (1, '공지사항'),
       (2, '자유게시판'),
       (3, 'Q&A'),
       (4, '정보공유'),
       (5, '이벤트');

-- Keywords
INSERT INTO keywords (keyword_id, keyword_name)
VALUES (1, '공지'),
       (2, '이슈'),
       (3, '업데이트'),
       (4, '버그'),
       (5, '질문'),
       (6, '답변'),
       (7, '팁'),
       (8, '가이드'),
       (9, '이벤트'),
       (10, '중요');

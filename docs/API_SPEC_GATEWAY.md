# Article Server API 명세서 (게이트웨이 전달용)

본 문서는 게이트웨이 라우팅/연동을 위한 최소한의 API 명세입니다.

중요: “기본 아티클 정보” 관련 엔드포인트는 Regular 전용 엔드포인트로 이전되었습니다. 즉, 기존 /api/articles/* 범주의 일반(Regular) 아티클 기능은
/api/articles/regular/* 를 사용하세요.

버전: v1
Base Path: 서비스 컨텍스트 기준 (예: https://<host>/)

목차

- Regular Articles (/api/articles/regular)
- Notice Articles (/api/articles/notices)
- Event Articles (/api/articles/events)
- Enums (/api/enums)

공통 사항

- 모든 성공 응답은 2xx 범위를 사용합니다.
- 오류 발생 시 서비스 공통 에러 포맷을 따릅니다(예: {"code":"...","message":"..."}).
- 날짜/시간 포맷: ISO-8601 (예: 2025-10-15T10:00:00).

1) Regular Articles
   Base: /api/articles/regular

1-1. Regular 아티클 생성
POST /api/articles/regular
요청 Body (application/json):
{
"title": "string",
"content": "string",
"writerId": "string",
"board": 1 또는 "BOARD_NAME", // 숫자 ID 또는 보드 이름 모두 허용
"keywords": [1, 2] 또는 ["키워드1", "키워드2"] // 숫자 ID 목록 또는 이름 목록 허용
}
응답(200): ArticleResponse
{
"articleId": "string",
"title": "string",
"content": "string",
"writerId": "string",
"board": {"<id>": "<name>"},
"LastestUpdateId": "2025-10-15T10:00:00",
"imageUrls": {"<imageId>": "<url>"},
"keywords": {"<id>": "<name>"}
}

1-2. Regular 아티클 수정
PUT /api/articles/regular/{articleId}
요청 Body: Regular 생성과 동일 필드
응답(200): ArticleResponse

1-3. Regular 아티클 단건 조회
GET /api/articles/regular/{articleId}
응답(200): ArticleResponse

1-4. Regular 아티클 삭제
DELETE /api/articles/regular/{articleId}
응답(204): 본문 없음

1-5. Regular 아티클 검색(커서 페이지네이션)
GET
/api/articles/regular/search?size={n}&cursorId={id}&board={idOrName}&keyword={k1}&keyword={k2}&title={t}&content={c}&writerIds={w1}&writerIds={w2}

- size: 페이지 크기(기본 10)
- cursorId: 다음 페이지 커서용 ID(선택)
- board: 보드 ID 또는 보드 이름(선택)
- keyword: 키워드(여러 개 지정 가능, URL 인코딩 허용; 숫자 ID 또는 이름)(선택)
- title: 제목 검색(선택)
- content: 본문 검색(선택)
- writerIds: 작성자 ID 배열(여러 개 지정 가능)(선택)

응답(200): ArticleCursorPageResponse
{
"items": [ArticleResponse, ...],
"nextCursorUpdatedAt": "2025-10-15T10:05:00",
"nextCursorId": "string",
"hasNext": true,
"size": 10
}

2) Notice Articles
   Base: /api/articles/notices

2-1. 공지 생성
POST /api/articles/notices
요청 Body:
{
"title": "string",
"content": "string",
"writerId": "string",
"keywords": [1, 2] 또는 ["키워드1", "키워드2"]
// board 필드는 무시됩니다(항상 공지사항 보드로 처리)
}
응답(200): ArticleResponse

2-2. 공지 수정
PUT /api/articles/notices/{articleId}
요청 Body: 생성과 동일(보드는 여전히 무시)
응답(200): ArticleResponse

2-3. 공지 단건 조회
GET /api/articles/notices/{articleId}
응답(200): ArticleResponse

2-4. 공지 삭제
DELETE /api/articles/notices/{articleId}
응답(204)

2-5. 공지 목록 조회(페이지네이션)
GET /api/articles/notices?page={n}&size={m}
응답(200): Spring Data Page<ArticleResponse>
{
"content": [ArticleResponse, ...],
"number": 0,
"size": 10,
"totalElements": 100,
"totalPages": 10,
... (Spring Page 기본 필드)
}

3) Event Articles
   Base: /api/articles/events

3-1. 이벤트 생성
POST /api/articles/events
요청 Body:
{
"title": "string",
"content": "string",
"writerId": "string",
"keywords": [1, 2] 또는 ["키워드1", "키워드2"],
"eventStartDate": "2025-10-15T09:00:00",
"eventEndDate": "2025-10-20T18:00:00"
}
응답(200): EventArticleResponse (ArticleResponse + 이벤트 기간)
{
"articleId": "string",
"title": "string",
"content": "string",
"writerId": "string",
"board": {"<id>": "<name>"},
"LastestUpdateId": "2025-10-15T10:00:00",
"imageUrls": {"<imageId>": "<url>"},
"keywords": {"<id>": "<name>"},
"eventStartDate": "2025-10-15T09:00:00",
"eventEndDate": "2025-10-20T18:00:00"
}

3-2. 이벤트 수정
PUT /api/articles/events/{articleId}
요청 Body: 생성과 동일
응답(200): EventArticleResponse

3-3. 이벤트 단건 조회
GET /api/articles/events/{articleId}
응답(200): EventArticleResponse

3-4. 이벤트 삭제
DELETE /api/articles/events/{articleId}
응답(204)

3-5. 이벤트 목록 조회(상태 필터 + 페이지네이션)
GET /api/articles/events?status={all|ongoing|ended|upcoming}&page={n}&size={m}
응답(200): Spring Data Page<EventArticleResponse>
{
"content": [EventArticleResponse, ...],
"number": 0,
"size": 10,
"totalElements": 100,
"totalPages": 10,
...
}

4) Enums
   Base: /api/enums

4-1. 보드 목록
GET /api/enums/boards
응답(200): Map<Long, String>
{
"1": "NOTICE",
"2": "REGULAR",
...
}

4-2. 키워드 목록
GET /api/enums/keywords
응답(200): Map<Long, String>
{
"10": "java",
"11": "spring",
...
}

부록 A. 이전/변경 사항(게이트웨이 공지용)

- BulkController 제외: /api/v1/bulk/* 엔드포인트는 게이트웨이 라우팅/문서에서 제외합니다.
- 기본 아티클 정보 이동: 기존 /api/articles/* 경로 중 일반(Regular) 아티클 관련 CRUD/검색은 /api/articles/regular/* 로 이전되었습니다. 게이트웨이는 Regular
  엔드포인트를 기준으로 라우팅/정책을 구성해주세요.

참고

- 키워드와 보드 값은 숫자 ID 또는 문자열 이름 모두 지원합니다. URL 인코딩된 문자열도 안전하게 처리됩니다.
- 커서 페이지네이션 응답의 nextCursorUpdatedAt/nextCursorId는 다음 페이지 요청 시 그대로 전달하십시오.

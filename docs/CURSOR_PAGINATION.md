# Article 검색: Cursor 기반 페이지네이션 가이드

이 문서는 Articles 검색 API의 커서 기반(keyset) 페이지네이션 방식과 파라미터 의미를 설명합니다.

## 핵심 요약

- 정렬 기준: 최신순(updated_at DESC) + 동률일 때 article_id DESC로 tie-breaker
- 커서 값: 기본은 `cursorId` 하나만 보내면 됩니다. 서버가 해당 id의 `updated_at`을 조회해서 커서 조건을 구성합니다. 필요시 `cursorUpdatedAt`을 함께 보내도 됩니다.
- 다음 페이지 요청 시에는 응답으로 받은 `nextCursorId`만 전달해도 동작합니다. 안정적인 호환을 위해 `nextCursorUpdatedAt`도 그대로 전달해도 무방합니다.

## 왜 updated_at 기준인가?

게시글은 수정될 수 있으므로, “최신”의 의미를 반영하기 위해 업데이트 시각(`updated_at`)이 더 적합합니다.
이 프로젝트는 최신순(수정 시각 기준) 무한스크롤 UX를 목표로 하며, 그에 따라 정렬 기준을 `updated_at`으로 설정했습니다.

## 커서 기반 페이지네이션이란?

- Offset 기반(`page`, `offset`) 대신 현재 보고 있는 목록의 경계값을 커서(cursor)로 전달하는 방식입니다.
- 장점: 대량 데이터에서도 빠르고 안정적인 정렬/탐색이 가능하며, 중간 삽입/삭제로 인한 흔들림이 적습니다.

## 커서 필드의 의미

- `cursorUpdatedAt` (LocalDateTime)
	- 마지막으로 클라이언트가 받은 목록의 “가장 마지막 항목”의 `updated_at` 값입니다.
	- 서버는 `updated_at < cursorUpdatedAt` 조건을 우선 적용하여 그 이후(더 오래된) 데이터만 가져옵니다.
- `cursorId` (String)
	- tie-breaker 용입니다.
	- 같은 `updated_at`을 가진 행들 사이의 안정적인 순서를 보장하기 위해 `article_id`를 함께 사용합니다.
	- 서버는 `updated_at = cursorUpdatedAt`인 경우에만 `id < cursorId` 조건을 추가로 적용합니다.

둘 다 비어 있으면(첫 페이지) 최신순으로 첫 N개를 내려줍니다.

## 정렬 및 커서 조건 (의사코드)

- ORDER BY `updated_at` DESC, `id` DESC
- WHERE 커서가 있을 때:
	- `updated_at < :cursorUpdatedAt`
	- OR (`updated_at = :cursorUpdatedAt` AND `id < :cursorId`)

## API 사용 방법

- Endpoint (권장): GET `/api/articles/search`
	- Query Params: 필터와 커서를 모두 쿼리 파라미터로 관리합니다.
	- 공통: `size`(선택), `cursorId`(선택), `cursorUpdatedAt`(선택; `cursorId`만 보내도 서버가 유도)
	- 필터: `boardId` 또는 `boardName`, `keywordIds`(복수 가능), `keywordNames`(복수 가능), `title`, `content`, `writerIds`(복수 가능),
	  `status`
	- 리스트 파라미터는 `?keywordIds=101,102` 또는 `?keywordIds=101&keywordIds=102` 모두 지원됩니다.

예시 요청 (첫 페이지, GET):

```
GET /api/articles/search?size=20&boardId=1&keywordIds=101,102
```

다음 페이지 요청 (응답에서 받은 커서 사용, GET):

```
GET /api/articles/search?size=20&cursorId=ART_000123&boardId=1&keywordIds=101,102
```

- Endpoint (호환): POST `/api/articles/search`
	- Body: 검색 필터(보드, 키워드, 작성자 등)
	- Query Params: `size`(선택), `cursorUpdatedAt`(선택), `cursorId`(선택)
	- 기존 클라이언트 호환을 위해 유지합니다.

## 응답의 커서 필드

- `nextCursorUpdatedAt`: 다음 페이지를 요청할 때 사용할 커서의 `updated_at`값
- `nextCursorId`: 다음 페이지를 요청할 때 사용할 커서의 `article_id`
- `hasNext`: 다음 페이지 존재 여부

## 주의 사항

- 클라이언트는 페이지 전환 시 반드시 `nextCursorUpdatedAt`, `nextCursorId`를 그대로 전달해야 동일한 순서를 이어갈 수 있습니다.
- 정렬 기준이 `updated_at`이므로, 데이터가 수정되면 다음 페이지 경계가 달라질 수 있습니다. 이는 “최신순” UX 특성상 자연스러운 동작입니다.

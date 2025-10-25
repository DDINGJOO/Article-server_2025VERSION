# Article Server API 명세서

## 기본 정보

- **Base URL (Gateway)**: `https://api.teambind.com/article`
- **Base URL (Direct)**: `http://localhost:8080`
- **API Version**: v1
- **Content-Type**: `application/json`
- **Charset**: UTF-8

## 인증

모든 API는 Gateway를 통해 인증 토큰이 검증됩니다.

```http
Authorization: Bearer {access_token}
```

---

## 목차

1. [게시글 API](#게시글-api)
2. [이벤트 API](#이벤트-api)
3. [공지사항 API](#공지사항-api)
4. [대량 조회 API](#대량-조회-api)
5. [Enums API](#enums-api)
6. [공통 응답 형식](#공통-응답-형식)
7. [에러 코드](#에러-코드)

---

## 게시글 API

### 1. 게시글 생성

게시글을 생성합니다. 타입은 eventStartDate/eventEndDate 유무에 따라 자동 판별됩니다.

```http
POST /api/v1/articles
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body

```json
{
  "title": "게시글 제목",
  "content": "게시글 내용입니다.",
  "writerId": "user123",
  "boardIds": 1,
  "keywordIds": [
    1,
    2
  ]
}
```

#### Request Fields

| Field          | Type          | Required | Description                      |
|----------------|---------------|----------|----------------------------------|
| title          | String        | Yes      | 게시글 제목 (최대 100자)                 |
| content        | String        | Yes      | 게시글 내용                           |
| writerId       | String        | Yes      | 작성자 ID                           |
| boardIds       | Long          | Yes      | 게시판 ID (존재하는 Board ID여야 함)       |
| keywordIds     | List<Long>    | No       | 키워드 ID 리스트 (존재하는 Keyword ID여야 함) |
| eventStartDate | LocalDateTime | No       | 이벤트 시작일 (이벤트 게시글만)               |
| eventEndDate   | LocalDateTime | No       | 이벤트 종료일 (이벤트 게시글만)               |

#### Validation Rules

- `title`, `content`, `writerId`: 필수
- `boardIds`: 존재하는 Board ID
- `keywordIds`: 모든 ID가 존재하는 Keyword ID
- `eventStartDate`, `eventEndDate`: 둘 다 null이거나 둘 다 있어야 함, 종료일 >= 시작일

#### Response (200 OK)

```json
{
  "articleId": "ART_20251025_001",
  "title": "게시글 제목",
  "content": "게시글 내용입니다.",
  "writerId": "user123",
  "board": {
    "boardId": 1,
    "boardName": "자유게시판",
    "description": "자유롭게 이야기하는 게시판"
  },
  "status": "ACTIVE",
  "viewCount": 0,
  "firstImageUrl": null,
  "createdAt": "2025-10-25T10:30:00",
  "updatedAt": "2025-10-25T10:30:00",
  "images": [],
  "keywords": [
    {
      "keywordId": 1,
      "keywordName": "Java",
      "isCommon": true,
      "boardId": null,
      "boardName": null
    },
    {
      "keywordId": 2,
      "keywordName": "Spring",
      "isCommon": true,
      "boardId": null,
      "boardName": null
    }
  ]
}
```

---

### 2. 게시글 단건 조회

```http
GET /api/v1/articles/{articleId}
Authorization: Bearer {token}
```

#### Path Parameters

| Parameter | Type   | Description |
|-----------|--------|-------------|
| articleId | String | 게시글 ID      |

#### Response (200 OK)

```json
{
  "articleId": "ART_20251025_001",
  "title": "게시글 제목",
  "content": "게시글 내용입니다.",
  "writerId": "user123",
  "board": {
    "boardId": 1,
    "boardName": "자유게시판",
    "description": "자유롭게 이야기하는 게시판"
  },
  "status": "ACTIVE",
  "viewCount": 15,
  "firstImageUrl": "https://cdn.teambind.com/images/img1.webp",
  "createdAt": "2025-10-25T10:30:00",
  "updatedAt": "2025-10-25T10:30:00",
  "images": [
    {
      "imageId": "IMG_001",
      "imageUrl": "https://cdn.teambind.com/images/img1.webp",
      "sequence": 1
    },
    {
      "imageId": "IMG_002",
      "imageUrl": "https://cdn.teambind.com/images/img2.webp",
      "sequence": 2
    }
  ],
  "keywords": [
    {
      "keywordId": 1,
      "keywordName": "Java",
      "isCommon": true,
      "boardId": null,
      "boardName": null
    },
    {
      "keywordId": 3,
      "keywordName": "질문",
      "isCommon": false,
      "boardId": 1,
      "boardName": "자유게시판"
    }
  ]
}
```

---

### 3. 게시글 수정

```http
PUT /api/v1/articles/{articleId}
Content-Type: application/json
Authorization: Bearer {token}
```

#### Path Parameters

| Parameter | Type   | Description |
|-----------|--------|-------------|
| articleId | String | 게시글 ID      |

#### Request Body

```json
{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "writerId": "user123",
  "boardIds": 1,
  "keywordIds": [
    1,
    3
  ]
}
```

#### Response (200 OK)

게시글 생성과 동일한 응답 형식

---

### 4. 게시글 삭제

게시글을 소프트 삭제 처리합니다. (status = DELETED)

```http
DELETE /api/v1/articles/{articleId}
Authorization: Bearer {token}
```

#### Response (204 No Content)

Body 없음

---

### 5. 게시글 검색

QueryDSL 기반 동적 검색과 커서 페이지네이션을 제공합니다.

```http
GET /api/v1/articles/search
Authorization: Bearer {token}
```

#### Query Parameters

| Parameter | Type       | Required | Default | Description         |
|-----------|------------|----------|---------|---------------------|
| size      | Integer    | No       | 10      | 페이지 크기              |
| cursorId  | String     | No       | null    | 커서 ID (다음 페이지 조회 시) |
| boardIds  | Long       | No       | null    | 게시판 ID 필터           |
| keyword   | List<Long> | No       | null    | 키워드 ID 필터 (복수 가능)   |
| title     | String     | No       | null    | 제목 검색 (부분 일치)       |
| content   | String     | No       | null    | 내용 검색 (부분 일치)       |
| writerId  | String     | No       | null    | 작성자 ID 필터           |

#### Example Request

```http
GET /api/v1/articles/search?boardIds=1&keyword=1&keyword=2&title=Spring&size=20
```

#### Response (200 OK)

```json
{
  "items": [
    {
      "articleId": "ART_20251025_001",
      "title": "Spring Boot 튜토리얼",
      "content": "Spring Boot를 시작하는 방법...",
      "writerId": "user123",
      "board": {
        "boardId": 1,
        "boardName": "자유게시판",
        "description": "자유롭게 이야기하는 게시판"
      },
      "status": "ACTIVE",
      "viewCount": 20,
      "firstImageUrl": null,
      "createdAt": "2025-10-25T10:30:00",
      "updatedAt": "2025-10-25T10:30:00",
      "images": [],
      "keywords": [
        {
          "keywordId": 1,
          "keywordName": "Java",
          "isCommon": true,
          "boardId": null,
          "boardName": null
        },
        {
          "keywordId": 2,
          "keywordName": "Spring",
          "isCommon": true,
          "boardId": null,
          "boardName": null
        }
      ]
    }
  ],
  "nextCursorId": "ART_20251025_010",
  "nextCursorUpdatedAt": "2025-10-25T10:00:00",
  "hasNext": true,
  "size": 20
}
```

#### 커서 페이지네이션 사용법

1. 첫 페이지 요청: `GET /api/v1/articles/search?size=20`
2. 다음 페이지 요청: `GET /api/v1/articles/search?size=20&cursorId={nextCursorId}`
3. `hasNext`가 false일 때까지 반복

---

## 이벤트 API

### 1. 이벤트 생성

```http
POST /api/v1/events
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body

```json
{
  "title": "크리스마스 이벤트",
  "content": "크리스마스 특별 이벤트입니다.",
  "writerId": "admin",
  "boardIds": 3,
  "keywordIds": [
    4,
    5
  ],
  "eventStartDate": "2025-12-24T00:00:00",
  "eventEndDate": "2025-12-25T23:59:59"
}
```

#### Response (200 OK)

```json
{
  "articleId": "ART_20251225_001",
  "title": "크리스마스 이벤트",
  "content": "크리스마스 특별 이벤트입니다.",
  "writerId": "admin",
  "board": {
    "boardId": 3,
    "boardName": "이벤트",
    "description": "이벤트 게시판"
  },
  "status": "ACTIVE",
  "viewCount": 0,
  "firstImageUrl": null,
  "createdAt": "2025-12-01T00:00:00",
  "updatedAt": "2025-12-01T00:00:00",
  "images": [],
  "keywords": [
    {
      "keywordId": 4,
      "keywordName": "이벤트",
      "isCommon": true,
      "boardId": null,
      "boardName": null
    }
  ],
  "eventStartDate": "2025-12-24T00:00:00",
  "eventEndDate": "2025-12-25T23:59:59"
}
```

---

### 2. 이벤트 수정

```http
PUT /api/v1/events/{articleId}
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body

```json
{
  "title": "수정된 이벤트 제목",
  "content": "수정된 이벤트 내용",
  "writerId": "admin",
  "boardIds": 3,
  "keywordIds": [
    4
  ],
  "eventStartDate": "2025-12-24T00:00:00",
  "eventEndDate": "2025-12-26T23:59:59"
}
```

#### Response (200 OK)

이벤트 생성과 동일한 응답 형식

---

### 3. 이벤트 단건 조회

```http
GET /api/v1/events/{articleId}
Authorization: Bearer {token}
```

#### Response (200 OK)

이벤트 생성과 동일한 응답 형식

---

### 4. 이벤트 삭제

```http
DELETE /api/v1/events/{articleId}
Authorization: Bearer {token}
```

#### Response (204 No Content)

Body 없음

---

### 5. 이벤트 목록 조회

이벤트 상태별 필터링을 지원합니다.

```http
GET /api/v1/events
Authorization: Bearer {token}
```

#### Query Parameters

| Parameter | Type    | Required | Default | Description                            |
|-----------|---------|----------|---------|----------------------------------------|
| status    | String  | No       | all     | 이벤트 상태 (all, ongoing, ended, upcoming) |
| page      | Integer | No       | 0       | 페이지 번호 (0부터 시작)                        |
| size      | Integer | No       | 10      | 페이지 크기                                 |

#### Status Values

- `all`: 전체 이벤트
- `ongoing`: 진행 중 (현재 시간이 시작일과 종료일 사이)
- `ended`: 종료됨 (현재 시간이 종료일 이후)
- `upcoming`: 진행 예정 (현재 시간이 시작일 이전)

#### Example Request

```http
GET /api/v1/events?status=ongoing&page=0&size=10
```

#### Response (200 OK)

```json
{
  "content": [
    {
      "articleId": "ART_20251225_001",
      "title": "크리스마스 이벤트",
      "content": "크리스마스 특별 이벤트입니다.",
      "writerId": "admin",
      "board": {
        "boardId": 3,
        "boardName": "이벤트",
        "description": "이벤트 게시판"
      },
      "status": "ACTIVE",
      "viewCount": 150,
      "firstImageUrl": "https://cdn.teambind.com/events/christmas.webp",
      "createdAt": "2025-12-01T00:00:00",
      "updatedAt": "2025-12-01T00:00:00",
      "images": [
        {
          "imageId": "IMG_EVENT_001",
          "imageUrl": "https://cdn.teambind.com/events/christmas.webp",
          "sequence": 1
        }
      ],
      "keywords": [],
      "eventStartDate": "2025-12-24T00:00:00",
      "eventEndDate": "2025-12-25T23:59:59"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 5,
  "totalPages": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 5,
  "first": true,
  "empty": false
}
```

---

## 공지사항 API

### 1. 공지사항 생성

```http
POST /api/v1/notices
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body

```json
{
  "title": "시스템 점검 안내",
  "content": "2025년 1월 1일 시스템 점검 예정입니다.",
  "writerId": "admin",
  "boardIds": 2,
  "keywordIds": [
    1
  ]
}
```

#### Response (200 OK)

```json
{
  "articleId": "ART_20251101_001",
  "title": "시스템 점검 안내",
  "content": "2025년 1월 1일 시스템 점검 예정입니다.",
  "writerId": "admin",
  "board": {
    "boardId": 2,
    "boardName": "공지사항",
    "description": "중요 공지사항 게시판"
  },
  "status": "ACTIVE",
  "viewCount": 0,
  "firstImageUrl": null,
  "createdAt": "2025-11-01T10:00:00",
  "updatedAt": "2025-11-01T10:00:00",
  "images": [],
  "keywords": [
    {
      "keywordId": 1,
      "keywordName": "공지",
      "isCommon": true,
      "boardId": null,
      "boardName": null
    }
  ]
}
```

---

### 2. 공지사항 수정

```http
PUT /api/v1/notices/{articleId}
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body

```json
{
  "title": "수정된 공지사항",
  "content": "수정된 내용입니다.",
  "writerId": "admin",
  "boardIds": 2,
  "keywordIds": [
    1
  ]
}
```

#### Response (200 OK)

공지사항 생성과 동일한 응답 형식

---

### 3. 공지사항 단건 조회

```http
GET /api/v1/notices/{articleId}
Authorization: Bearer {token}
```

#### Response (200 OK)

공지사항 생성과 동일한 응답 형식

---

### 4. 공지사항 삭제

```http
DELETE /api/v1/notices/{articleId}
Authorization: Bearer {token}
```

#### Response (204 No Content)

Body 없음

---

### 5. 공지사항 목록 조회

```http
GET /api/v1/notices
Authorization: Bearer {token}
```

#### Query Parameters

| Parameter | Type    | Required | Default | Description |
|-----------|---------|----------|---------|-------------|
| page      | Integer | No       | 0       | 페이지 번호      |
| size      | Integer | No       | 10      | 페이지 크기      |

#### Response (200 OK)

Spring Data JPA Page 형식 (이벤트 API와 동일)

---

## 대량 조회 API

### 1. 게시글 대량 조회

여러 게시글을 ID 리스트로 한 번에 조회합니다.

```http
GET /api/v1/bulk/articles?ids={id1}&ids={id2}&ids={id3}
Authorization: Bearer {token}
```

#### Query Parameters

| Parameter | Type         | Required | Description |
|-----------|--------------|----------|-------------|
| ids       | List<String> | Yes      | 게시글 ID 리스트  |

#### Example Request

```http
GET /api/v1/bulk/articles?ids=ART_001&ids=ART_002&ids=ART_003
```

#### Response (200 OK)

```json
[
  {
    "articleId": "ART_001",
    "title": "게시글 제목 1",
    "writerId": "user123",
    "boardId": 1,
    "boardName": "자유게시판",
    "articleType": "REGULAR",
    "status": "ACTIVE",
    "viewCount": 10,
    "firstImageUrl": "https://cdn.teambind.com/images/img1.webp",
    "createdAt": "2025-10-25T10:30:00",
    "updatedAt": "2025-10-25T10:30:00"
  },
  {
    "articleId": "ART_002",
    "title": "이벤트 제목",
    "writerId": "admin",
    "boardId": 3,
    "boardName": "이벤트",
    "articleType": "EVENT",
    "status": "ACTIVE",
    "viewCount": 50,
    "firstImageUrl": null,
    "createdAt": "2025-10-25T11:00:00",
    "updatedAt": "2025-10-25T11:00:00"
  },
  {
    "articleId": "ART_003",
    "title": "공지사항 제목",
    "writerId": "admin",
    "boardId": 2,
    "boardName": "공지사항",
    "articleType": "NOTICE",
    "status": "ACTIVE",
    "viewCount": 100,
    "firstImageUrl": null,
    "createdAt": "2025-10-25T09:00:00",
    "updatedAt": "2025-10-25T09:00:00"
  }
]
```

---

## Enums API

### 1. 게시판 목록 조회

```http
GET /api/v1/enums/boards
```

#### Response (200 OK)

```json
{
  "1": {
    "boardId": 1,
    "boardName": "자유게시판",
    "description": "자유롭게 이야기하는 게시판"
  },
  "2": {
    "boardId": 2,
    "boardName": "공지사항",
    "description": "중요 공지사항 게시판"
  },
  "3": {
    "boardId": 3,
    "boardName": "이벤트",
    "description": "이벤트 게시판"
  }
}
```

---

### 2. 키워드 목록 조회

```http
GET /api/v1/enums/keywords
```

#### Response (200 OK)

```json
{
  "1": {
    "keywordId": 1,
    "keywordName": "공지",
    "isCommon": true,
    "boardId": null,
    "boardName": null
  },
  "2": {
    "keywordId": 2,
    "keywordName": "이벤트",
    "isCommon": true,
    "boardId": null,
    "boardName": null
  },
  "3": {
    "keywordId": 3,
    "keywordName": "질문",
    "isCommon": false,
    "boardId": 1,
    "boardName": "자유게시판"
  },
  "4": {
    "keywordId": 4,
    "keywordName": "Java",
    "isCommon": true,
    "boardId": null,
    "boardName": null
  }
}
```

---

## 공통 응답 형식

### ArticleBaseResponse

모든 게시글 응답의 기본 형식입니다.

```json
{
  "articleId": "ART_20251025_001",
  "title": "게시글 제목",
  "content": "게시글 내용",
  "writerId": "user123",
  "board": {
    "boardId": 1,
    "boardName": "자유게시판",
    "description": "자유롭게 이야기하는 게시판"
  },
  "status": "ACTIVE",
  "viewCount": 15,
  "firstImageUrl": "https://cdn.teambind.com/images/img1.webp",
  "createdAt": "2025-10-25T10:30:00",
  "updatedAt": "2025-10-25T10:30:00",
  "images": [
    {
      "imageId": "IMG_001",
      "imageUrl": "https://cdn.teambind.com/images/img1.webp",
      "sequence": 1
    }
  ],
  "keywords": [
    {
      "keywordId": 1,
      "keywordName": "Java",
      "isCommon": true,
      "boardId": null,
      "boardName": null
    }
  ]
}
```

### EventArticleResponse

이벤트 게시글 응답 (ArticleBaseResponse 확장)

```json
{
  "articleId": "ART_20251225_001",
  "title": "크리스마스 이벤트",
  "content": "이벤트 내용",
  "writerId": "admin",
  "board": {
    "boardId": 3,
    "boardName": "이벤트",
    "description": "이벤트 게시판"
  },
  "status": "ACTIVE",
  "viewCount": 100,
  "firstImageUrl": null,
  "createdAt": "2025-12-01T00:00:00",
  "updatedAt": "2025-12-01T00:00:00",
  "images": [],
  "keywords": [],
  "eventStartDate": "2025-12-24T00:00:00",
  "eventEndDate": "2025-12-25T23:59:59"
}
```

### ArticleSimpleResponse

게시글 간단 응답 (대량 조회용)

```json
{
  "articleId": "ART_001",
  "title": "게시글 제목",
  "writerId": "user123",
  "boardId": 1,
  "boardName": "자유게시판",
  "articleType": "REGULAR",
  "status": "ACTIVE",
  "viewCount": 10,
  "firstImageUrl": "https://cdn.teambind.com/images/img1.webp",
  "createdAt": "2025-10-25T10:30:00",
  "updatedAt": "2025-10-25T10:30:00"
}
```

### BoardInfo

게시판 정보

```json
{
  "boardId": 1,
  "boardName": "자유게시판",
  "description": "자유롭게 이야기하는 게시판"
}
```

### KeywordInfo

키워드 정보

```json
{
  "keywordId": 1,
  "keywordName": "Java",
  "isCommon": true,
  "boardId": null,
  "boardName": null
}
```

보드 전용 키워드인 경우:

```json
{
  "keywordId": 3,
  "keywordName": "질문",
  "isCommon": false,
  "boardId": 1,
  "boardName": "자유게시판"
}
```

### ImageInfo

이미지 정보

```json
{
  "imageId": "IMG_001",
  "imageUrl": "https://cdn.teambind.com/images/img1.webp",
  "sequence": 1
}
```

### CursorPageResponse

커서 기반 페이지네이션 응답

```json
{
  "items": [
    {
      "articleId": "ART_001",
      "title": "게시글 제목",
      ...
    }
  ],
  "nextCursorId": "ART_010",
  "nextCursorUpdatedAt": "2025-10-25T10:00:00",
  "hasNext": true,
  "size": 20
}
```

---

## 에러 코드

### HTTP Status Codes

| Status | Description            |
|--------|------------------------|
| 200    | 성공                     |
| 204    | 성공 (응답 본문 없음)          |
| 400    | 잘못된 요청 (Validation 실패) |
| 401    | 인증 실패                  |
| 403    | 권한 없음                  |
| 404    | 리소스 없음                 |
| 500    | 서버 에러                  |

### Error Response Format

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2025-10-25T10:30:00",
  "path": "/api/v1/articles",
  "errors": [
    {
      "field": "boardIds",
      "message": "존재하지 않는 Board ID입니다"
    },
    {
      "field": "eventEndDate",
      "message": "이벤트 시작일과 종료일은 모두 입력되어야 합니다"
    }
  ]
}
```

### Custom Error Codes

| Code                 | Message        | Description    |
|----------------------|----------------|----------------|
| ARTICLE_NOT_FOUND    | 게시글을 찾을 수 없습니다 | 존재하지 않는 게시글 ID |
| ARTICLE_IS_BLOCKED   | 차단된 게시글입니다     | 차단 상태의 게시글     |
| BOARD_NOT_FOUND      | 게시판을 찾을 수 없습니다 | 존재하지 않는 게시판 ID |
| KEYWORD_NOT_FOUND    | 키워드를 찾을 수 없습니다 | 존재하지 않는 키워드 ID |
| INVALID_EVENT_PERIOD | 잘못된 이벤트 기간입니다  | 종료일이 시작일보다 이전  |

---

## Gateway 라우팅 규칙

모든 요청은 API Gateway를 통해 라우팅됩니다.

### 라우팅 매핑

| Client Request | Gateway Route  | Backend Service |
|----------------|----------------|-----------------|
| `/article/**`  | → `/api/v1/**` | Article Server  |

### Example

```
Client: GET https://api.teambind.com/article/articles/ART_001
   ↓
Gateway: GET http://article-server:8080/api/v1/articles/ART_001
   ↓
Article Server: 응답 반환
```

---

## Rate Limiting

Gateway에서 Rate Limiting이 적용됩니다.

- 인증된 사용자: 100 requests/minute
- 미인증 사용자: 20 requests/minute

Rate Limit 초과 시:

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later."
}
```

---

## 작성일

2025-10-25

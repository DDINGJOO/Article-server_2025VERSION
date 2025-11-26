# Article Server API Specification

**Version**: 2.0.0
**Last Updated**: 2025-11-26
**Base URL**: `http://localhost:8080`
**API Version Prefix**: `/api/v1`, `/api/v2`

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Common Headers](#common-headers)
4. [Error Responses](#error-responses)
5. [API Endpoints](#api-endpoints)
	- [Articles API v1](#articles-api-v1)
	- [Articles API v2](#articles-api-v2)
	- [Event Articles API](#event-articles-api)
	- [Notice Articles API](#notice-articles-api)
	- [Bulk Operations API](#bulk-operations-api)
	- [Enums API](#enums-api)
6. [Data Models](#data-models)
7. [Pagination](#pagination)
8. [Rate Limiting](#rate-limiting)
9. [Postman Collection](#postman-collection)

---

## Overview

Article Server API는 게시글 관리를 위한 RESTful API 서비스입니다. 다양한 게시글 타입(Regular, Event, Notice)을 지원하며, 검색, 페이징, 벌크 조회 등의 기능을
제공합니다.

### Key Features

- **Multi-type Article Support**: Regular, Event, Notice 게시글 타입 지원
- **Advanced Search**: 제목, 내용, 작성자, 키워드 기반 검색
- **Cursor-based Pagination**: 대용량 데이터 처리를 위한 커서 기반 페이징
- **Bulk Operations**: 다중 게시글 동시 조회
- **Event Streaming**: Kafka 기반 실시간 이벤트 발행

### API Versioning

- **v1**: Legacy API - 기존 클라이언트 호환성 유지
- **v2**: Hexagonal Architecture 기반 신규 API

---

## Authentication

현재 Article Server API는 인증을 직접 처리하지 않습니다. API Gateway 레벨에서 인증이 처리됩니다.

```http
Authorization: Bearer {access_token}
X-User-Id: {user_id}
```

---

## Common Headers

### Request Headers

| Header         | Required | Description         | Example                   |
|----------------|----------|---------------------|---------------------------|
| `Content-Type` | Yes      | Request body format | `application/json`        |
| `Accept`       | No       | Response format     | `application/json`        |
| `X-Request-ID` | No       | Request tracking ID | `550e8400-e29b-41d4-a716` |
| `X-User-Id`    | Yes      | User identifier     | `user123`                 |

### Response Headers

| Header            | Description          | Example                   |
|-------------------|----------------------|---------------------------|
| `X-Request-ID`    | Request tracking ID  | `550e8400-e29b-41d4-a716` |
| `X-Response-Time` | Processing time (ms) | `123`                     |

---

## Error Responses

### Error Response Format

```json
{
  "timestamp": "2025-11-26T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/articles",
  "errors": [
    {
      "field": "title",
      "message": "제목은 필수입니다"
    }
  ]
}
```

### Common Error Codes

| HTTP Status | Code                    | Description  |
|-------------|-------------------------|--------------|
| `400`       | `INVALID_INPUT_VALUE`   | 잘못된 입력값      |
| `404`       | `ENTITY_NOT_FOUND`      | 엔티티를 찾을 수 없음 |
| `409`       | `DUPLICATE_RESOURCE`    | 중복된 리소스      |
| `500`       | `INTERNAL_SERVER_ERROR` | 서버 내부 오류     |

---

## API Endpoints

### Articles API v1

#### Create Article

```http
POST /api/v1/articles
```

**Request Body:**

```json
{
  "title": "게시글 제목",
  "content": "게시글 내용",
  "writerId": "user123",
  "boardIds": 1,
  "keywordIds": [1, 2, 3],
  "eventStartDate": "2025-12-01T00:00:00",
  "eventEndDate": "2025-12-31T23:59:59"
}
```

**Response:** `200 OK`

```json
{
  "articleId": "ART20251126001",
  "title": "게시글 제목",
  "content": "게시글 내용",
  "writerId": "user123",
  "board": {
    "boardId": 1,
    "boardName": "자유게시판"
  },
  "keywords": [
    {
      "keywordId": 1,
      "keywordName": "공지"
    }
  ],
  "images": [],
  "status": "ACTIVE",
  "viewCount": 0,
  "createdAt": "2025-11-26T10:00:00Z",
  "updatedAt": "2025-11-26T10:00:00Z"
}
```

#### Get Article

```http
GET /api/v1/articles/{articleId}
```

**Path Parameters:**

- `articleId` (string, required): 게시글 ID

**Response:** `200 OK`

```json
{
  "articleId": "ART20251126001",
  "title": "게시글 제목",
  "content": "게시글 내용",
  "writerId": "user123",
  "board": {
    "boardId": 1,
    "boardName": "자유게시판"
  },
  "keywords": [],
  "images": [],
  "status": "ACTIVE",
  "viewCount": 42,
  "createdAt": "2025-11-26T10:00:00Z",
  "updatedAt": "2025-11-26T10:00:00Z"
}
```

#### Update Article

```http
PUT /api/v1/articles/{articleId}
```

**Path Parameters:**

- `articleId` (string, required): 게시글 ID

**Request Body:**

```json
{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "writerId": "user123",
  "boardIds": 1,
  "keywordIds": [2, 3]
}
```

**Response:** `200 OK`

#### Delete Article

```http
DELETE /api/v1/articles/{articleId}
```

**Path Parameters:**

- `articleId` (string, required): 게시글 ID

**Response:** `204 No Content`

#### Search Articles

```http
GET /api/v1/articles/search
```

**Query Parameters:**

| Parameter  | Type        | Required | Description | Default |
|------------|-------------|----------|-------------|---------|
| `size`     | integer     | No       | 페이지 크기      | 10      |
| `cursorId` | string      | No       | 커서 ID (페이징) | -       |
| `boardIds` | long        | No       | 게시판 ID      | -       |
| `keyword`  | array[long] | No       | 키워드 ID 리스트  | -       |
| `title`    | string      | No       | 제목 검색어      | -       |
| `content`  | string      | No       | 내용 검색어      | -       |
| `writerId` | string      | No       | 작성자 ID      | -       |

**Response:** `200 OK`

```json
{
  "items": [
    {
      "articleId": "ART20251126001",
      "title": "게시글 제목",
      "content": "게시글 내용",
      "writerId": "user123",
      "board": {
        "boardId": 1,
        "boardName": "자유게시판"
      },
      "keywords": [],
      "status": "ACTIVE",
      "viewCount": 42,
      "createdAt": "2025-11-26T10:00:00Z"
    }
  ],
  "hasNext": true,
  "nextCursorId": "ART20251126002",
  "totalElements": 100
}
```

---

### Articles API v2

#### Create Article (Hexagonal)

```http
POST /api/v2/articles
```

**Request Body:**

```json
{
  "title": "게시글 제목",
  "content": "게시글 내용",
  "writerId": "user123",
  "boardId": 1,
  "keywordIds": [1, 2],
  "eventStartDate": null,
  "eventEndDate": null
}
```

**Response:** `200 OK`

```json
{
  "articleId": "ART20251126001",
  "title": "게시글 제목",
  "boardName": "자유게시판",
  "status": "ACTIVE",
  "createdAt": "2025-11-26T10:00:00Z"
}
```

#### Get Article (Hexagonal)

```http
GET /api/v2/articles/{articleId}
```

**Response:** `200 OK`

```json
{
  "articleId": "ART20251126001",
  "title": "게시글 제목",
  "content": "게시글 내용",
  "writerId": "user123",
  "boardName": "자유게시판",
  "keywords": ["공지", "중요"],
  "images": [],
  "status": "ACTIVE",
  "viewCount": 42,
  "createdAt": "2025-11-26T10:00:00Z",
  "updatedAt": "2025-11-26T10:00:00Z"
}
```

#### Search Articles (Hexagonal)

```http
GET /api/v2/articles/search
```

**Query Parameters:**

| Parameter    | Type        | Required | Description         | Default |
|--------------|-------------|----------|---------------------|---------|
| `boardId`    | long        | No       | 게시판 ID              | -       |
| `title`      | string      | No       | 제목 검색어              | -       |
| `content`    | string      | No       | 내용 검색어              | -       |
| `writerId`   | string      | No       | 작성자 ID              | -       |
| `keywordIds` | array[long] | No       | 키워드 ID 리스트          | -       |
| `status`     | string      | No       | 상태 (ACTIVE/DELETED) | ACTIVE  |
| `page`       | integer     | No       | 페이지 번호              | 0       |
| `size`       | integer     | No       | 페이지 크기              | 10      |

**Response:** `200 OK`

```json
{
  "articles": [
    {
      "articleId": "ART20251126001",
      "title": "게시글 제목",
      "content": "게시글 내용",
      "writerId": "user123",
      "boardName": "자유게시판",
      "keywords": ["공지"],
      "images": [],
      "status": "ACTIVE",
      "viewCount": 42,
      "createdAt": "2025-11-26T10:00:00Z",
      "updatedAt": "2025-11-26T10:00:00Z"
    }
  ],
  "currentPage": 0,
  "totalPages": 10,
  "totalElements": 100,
  "hasNext": true
}
```

---

### Event Articles API

#### Create Event Article

```http
POST /api/v1/events
```

**Request Body:**

```json
{
  "title": "이벤트 제목",
  "content": "이벤트 내용",
  "writerId": "admin",
  "boardIds": 2,
  "keywordIds": [4, 5],
  "eventStartDate": "2025-12-01T00:00:00",
  "eventEndDate": "2025-12-31T23:59:59"
}
```

**Response:** `200 OK`

```json
{
  "articleId": "EVT20251126001",
  "title": "이벤트 제목",
  "content": "이벤트 내용",
  "writerId": "admin",
  "board": {
    "boardId": 2,
    "boardName": "이벤트"
  },
  "eventStartDate": "2025-12-01T00:00:00",
  "eventEndDate": "2025-12-31T23:59:59",
  "eventStatus": "UPCOMING",
  "status": "ACTIVE",
  "viewCount": 0,
  "createdAt": "2025-11-26T10:00:00Z"
}
```

#### Get Event Articles

```http
GET /api/v1/events
```

**Query Parameters:**

| Parameter | Type    | Required | Description                   | Default |
|-----------|---------|----------|-------------------------------|---------|
| `status`  | string  | No       | all, ongoing, ended, upcoming | all     |
| `page`    | integer | No       | 페이지 번호                        | 0       |
| `size`    | integer | No       | 페이지 크기                        | 10      |

**Response:** `200 OK`

```json
{
  "content": [
    {
      "articleId": "EVT20251126001",
      "title": "연말 이벤트",
      "eventStartDate": "2025-12-01T00:00:00",
      "eventEndDate": "2025-12-31T23:59:59",
      "eventStatus": "ONGOING",
      "status": "ACTIVE",
      "viewCount": 150
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false
}
```

---

### Notice Articles API

#### Create Notice Article

```http
POST /api/v1/notices
```

**Request Body:**

```json
{
  "title": "공지사항 제목",
  "content": "공지사항 내용",
  "writerId": "admin",
  "boardIds": 3,
  "keywordIds": [1]
}
```

**Response:** `200 OK`

#### Get Notice Articles

```http
GET /api/v1/notices
```

**Query Parameters:**

| Parameter | Type    | Required | Description | Default |
|-----------|---------|----------|-------------|---------|
| `page`    | integer | No       | 페이지 번호      | 0       |
| `size`    | integer | No       | 페이지 크기      | 10      |

**Response:** `200 OK`

```json
{
  "content": [
    {
      "articleId": "NTC20251126001",
      "title": "시스템 점검 공지",
      "writerId": "admin",
      "board": {
        "boardId": 3,
        "boardName": "공지사항"
      },
      "status": "ACTIVE",
      "viewCount": 523,
      "createdAt": "2025-11-26T10:00:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 15,
  "totalPages": 2,
  "last": false
}
```

---

### Bulk Operations API

#### Bulk Fetch Articles

```http
GET /api/v1/bulk/articles?ids=1&ids=2&ids=3
```

**Query Parameters:**

- `ids` (array[string], required): 게시글 ID 리스트

**Response:** `200 OK`

```json
[
  {
    "articleId": "ART20251126001",
    "title": "첫 번째 게시글",
    "writerId": "user1",
    "boardName": "자유게시판",
    "status": "ACTIVE",
    "createdAt": "2025-11-26T10:00:00Z"
  },
  {
    "articleId": "ART20251126002",
    "title": "두 번째 게시글",
    "writerId": "user2",
    "boardName": "질문답변",
    "status": "ACTIVE",
    "createdAt": "2025-11-26T11:00:00Z"
  }
]
```

---

### Enums API

#### Get Available Enums

```http
GET /api/v1/enums
```

**Response:** `200 OK`

```json
{
  "articleTypes": ["REGULAR", "EVENT", "NOTICE"],
  "articleStatus": ["ACTIVE", "DELETED", "HIDDEN"],
  "eventStatus": ["UPCOMING", "ONGOING", "ENDED"]
}
```

---

## Data Models

### Article Base Model

```typescript
interface Article {
  articleId: string;           // Snowflake ID
  title: string;               // Max 200 chars
  content: string;             // Max 65535 chars
  writerId: string;            // Max 50 chars
  board: BoardInfo;
  keywords: KeywordInfo[];
  images: ImageInfo[];
  status: "ACTIVE" | "DELETED" | "HIDDEN";
  viewCount: number;
  createdAt: string;           // ISO 8601
  updatedAt: string;           // ISO 8601
}
```

### Event Article Model

```typescript
interface EventArticle extends Article {
  eventStartDate: string;      // ISO 8601
  eventEndDate: string;        // ISO 8601
  eventStatus: "UPCOMING" | "ONGOING" | "ENDED";
}
```

### Board Info

```typescript
interface BoardInfo {
  boardId: number;
  boardName: string;
}
```

### Keyword Info

```typescript
interface KeywordInfo {
  keywordId: number;
  keywordName: string;
}
```

### Image Info

```typescript
interface ImageInfo {
  imageId: number;
  imageUrl: string;
  displayOrder: number;
}
```

---

## Pagination

### Cursor-based Pagination

커서 기반 페이징은 대용량 데이터셋에서 효율적인 페이징을 제공합니다.

```http
GET /api/v1/articles/search?size=20&cursorId=ART20251126050
```

**Response:**

```json
{
  "items": [...],
  "hasNext": true,
  "nextCursorId": "ART20251126070",
  "totalElements": null  // 커서 기반에서는 정확한 총 개수 제공 안 함
}
```

### Offset-based Pagination

전통적인 오프셋 기반 페이징 (v2 API, Notice, Event 목록)

```http
GET /api/v2/articles/search?page=2&size=20
```

**Response:**

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 2,
    "pageSize": 20,
    "offset": 40
  },
  "totalElements": 500,
  "totalPages": 25,
  "first": false,
  "last": false
}
```

---

## Rate Limiting

현재 API Gateway 레벨에서 처리되며, 다음과 같은 제한이 적용됩니다:

| Endpoint Type    | Rate Limit   | Window   |
|------------------|--------------|----------|
| GET (Read)       | 1000 req/min | 1 minute |
| POST/PUT (Write) | 100 req/min  | 1 minute |
| DELETE           | 50 req/min   | 1 minute |
| Bulk Operations  | 10 req/min   | 1 minute |

Rate limit 초과 시 `429 Too Many Requests` 응답:

```json
{
  "error": "Rate limit exceeded",
  "retryAfter": 45,
  "limit": 100,
  "remaining": 0,
  "reset": "2025-11-26T10:01:00Z"
}
```

---

## Postman Collection

### Import Instructions

1. Postman 열기
2. Collections → Import
3. 아래 JSON 파일 import

### Collection Download

[Article Server API Collection v2.0.0](https://api.postman.com/collections/article-server-v2)

### Environment Variables

```json
{
  "baseUrl": "http://localhost:8080",
  "apiVersion": "v1",
  "authToken": "{{bearerToken}}",
  "userId": "testuser"
}
```

---

## Webhook Events

Article Server는 다음 이벤트를 Kafka로 발행합니다:

### Article Created Event

**Topic:** `article.created`

```json
{
  "eventId": "evt_20251126_001",
  "eventType": "ARTICLE_CREATED",
  "aggregateId": "ART20251126001",
  "occurredAt": "2025-11-26T10:00:00Z",
  "payload": {
    "articleId": "ART20251126001",
    "title": "새 게시글",
    "writerId": "user123",
    "boardId": 1
  }
}
```

### Article Deleted Event

**Topic:** `article.deleted`

```json
{
  "eventId": "evt_20251126_002",
  "eventType": "ARTICLE_DELETED",
  "aggregateId": "ART20251126001",
  "occurredAt": "2025-11-26T11:00:00Z",
  "payload": {
    "articleId": "ART20251126001",
    "deletedBy": "user123"
  }
}
```

### Article Image Changed Event

**Topic:** `article-image-changed`

```json
{
  "eventId": "evt_20251126_003",
  "eventType": "ARTICLE_IMAGE_CHANGED",
  "aggregateId": "ART20251126001",
  "occurredAt": "2025-11-26T12:00:00Z",
  "payload": {
    "articleId": "ART20251126001",
    "addedImages": ["img1.jpg"],
    "removedImages": ["img2.jpg"]
  }
}
```

---

## Migration Guide

### v1 to v2 Migration

#### Key Changes

1. **Response Structure**: v2는 더 단순화된 응답 구조 사용
2. **Pagination**: v2는 표준 Spring Page 응답 사용
3. **Error Handling**: v2는 RFC 7807 Problem Details 형식 사용

#### Migration Steps

1. **Update Base URL**
	- v1: `/api/v1/articles`
	- v2: `/api/v2/articles`

2. **Update Response Parsing**
   ```javascript
   // v1
   const articles = response.items;
   const hasMore = response.hasNext;

   // v2
   const articles = response.articles;
   const hasMore = response.hasNext;
   ```

3. **Update Error Handling**
   ```javascript
   // v1
   if (error.status === 400) {
     console.log(error.message);
   }

   // v2
   if (error.status === 400) {
     console.log(error.detail);
   }
   ```

---

## Support

### API Status

- Status Page: https://status.teambind.com
- Health Check: `GET /actuator/health`

### Contact

- Email: api-support@teambind.com
- Slack: #article-server-api
- GitHub Issues: [Article Server Issues](https://github.com/DDINGJOO/Article-server_2025VERSION/issues)

---

**Document Version**: 2.0.0
**Last Review**: 2025-11-26
**Next Review**: 2025-01-26

---

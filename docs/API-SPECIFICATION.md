# Article Server API 명세서

## 목차

1. [개요](#개요)
2. [공통 사항](#공통-사항)
3. [게시글 API](#1-게시글-api-apiv1articles)
4. [공지사항 API](#2-공지사항-api-apiv1notices)
5. [이벤트 API](#3-이벤트-api-apiv1events)
6. [벌크 조회 API](#4-벌크-조회-api-apiv1bulk)
7. [열거형 정보 API](#5-열거형-정보-api-apiv1enums)
8. [헬스 체크 API](#6-헬스-체크-api-health)
9. [응답 구조](#공통-응답-구조)

## 개요

Article Server는 게시글 관리를 위한 REST API 서버입니다. Hexagonal Architecture를 기반으로 설계되었으며, 다양한 게시글 타입(일반, 공지사항, 이벤트)을 지원합니다.

### Base URL

```
http://localhost:8080
```

### API Version

- 현재 버전: v1
- API 경로 prefix: `/api/v1`

## 공통 사항

### 인증

현재 버전에서는 별도의 인증이 필요하지 않습니다. (추후 JWT 기반 인증 추가 예정)

### 요청 헤더

```
Content-Type: application/json
Accept: application/json
```

### HTTP 상태 코드

| 상태 코드                     | 설명                 |
|---------------------------|--------------------|
| 200 OK                    | 요청 성공              |
| 201 Created               | 리소스 생성 성공          |
| 204 No Content            | 삭제 성공              |
| 400 Bad Request           | 잘못된 요청 (유효성 검사 실패) |
| 404 Not Found             | 리소스를 찾을 수 없음       |
| 500 Internal Server Error | 서버 내부 오류           |

## 1. 게시글 API (`/api/v1/articles`)

### 1.1 게시글 생성

- **Method**: `POST`
- **Path**: `/api/v1/articles`
- **설명**: 새로운 게시글을 생성합니다.

#### 요청 바디

```json
{
  "title": "제목 (필수)",
  "content": "내용 (필수)",
  "writerId": "작성자ID (필수)",
  "boardIds": 1,                            // 게시판 ID (필수, Long)
  "keywordIds": [1, 2, 3],                  // 키워드 ID 리스트 (선택)
  "eventStartDate": "2024-01-01T00:00:00",  // 이벤트 시작일 (이벤트 게시글만)
  "eventEndDate": "2024-12-31T23:59:59"     // 이벤트 종료일 (이벤트 게시글만)
}
```

#### 응답

- **Status**: `200 OK`
- **Body**: `ArticleBaseResponse` (게시글 타입에 따라 달라짐)

### 1.2 게시글 수정

- **Method**: `PUT`
- **Path**: `/api/v1/articles/{articleId}`
- **설명**: 기존 게시글을 수정합니다.

#### 경로 파라미터

- `articleId`: 수정할 게시글 ID

#### 요청 바디

생성과 동일한 구조

#### 응답

- **Status**: `200 OK`
- **Body**: `ArticleBaseResponse`

### 1.3 게시글 조회

- **Method**: `GET`
- **Path**: `/api/v1/articles/{articleId}`
- **설명**: 특정 게시글의 상세 정보를 조회합니다.

#### 경로 파라미터

- `articleId`: 조회할 게시글 ID

#### 응답

- **Status**: `200 OK`
- **Body**: `ArticleBaseResponse`

### 1.4 게시글 삭제

- **Method**: `DELETE`
- **Path**: `/api/v1/articles/{articleId}`
- **설명**: 게시글을 소프트 삭제합니다.

#### 경로 파라미터

- `articleId`: 삭제할 게시글 ID

#### 응답

- **Status**: `204 No Content`

### 1.5 게시글 검색

- **Method**: `GET`
- **Path**: `/api/v1/articles/search`
- **설명**: 조건에 따른 게시글을 검색합니다. 커서 기반 페이징을 지원합니다.

#### 쿼리 파라미터

| 파라미터     | 타입         | 필수  | 설명                 | 기본값 |
|----------|------------|-----|--------------------|-----|
| size     | Integer    | 아니오 | 페이지 크기             | 10  |
| cursorId | String     | 아니오 | 커서 ID (다음 페이지 조회용) | -   |
| boardIds | Long       | 아니오 | 게시판 ID             | -   |
| keyword  | List<Long> | 아니오 | 키워드 ID 리스트         | -   |
| title    | String     | 아니오 | 제목 검색어             | -   |
| content  | String     | 아니오 | 내용 검색어             | -   |
| writerId | String     | 아니오 | 작성자 ID             | -   |

#### 응답

- **Status**: `200 OK`
- **Body**: `ArticleCursorPageResponse`

```json
{
  "content": [
    // ArticleBaseResponse 리스트
  ],
  "nextCursorId": "다음페이지커서ID",
  "hasNext": true,
  "pageSize": 10
}
```

## 2. 공지사항 API (`/api/v1/notices`)

### 2.1 공지사항 생성

- **Method**: `POST`
- **Path**: `/api/v1/notices`
- **설명**: 새로운 공지사항을 생성합니다.

#### 요청 바디

`ArticleCreateRequest`와 동일

#### 응답

- **Status**: `200 OK`
- **Body**: `NoticeArticleResponse`

### 2.2 공지사항 수정

- **Method**: `PUT`
- **Path**: `/api/v1/notices/{articleId}`
- **설명**: 기존 공지사항을 수정합니다.

#### 경로 파라미터

- `articleId`: 수정할 공지사항 ID

#### 응답

- **Status**: `200 OK`
- **Body**: `NoticeArticleResponse`

### 2.3 공지사항 조회

- **Method**: `GET`
- **Path**: `/api/v1/notices/{articleId}`
- **설명**: 특정 공지사항의 상세 정보를 조회합니다.

#### 경로 파라미터

- `articleId`: 조회할 공지사항 ID

#### 응답

- **Status**: `200 OK`
- **Body**: `NoticeArticleResponse`

### 2.4 공지사항 삭제

- **Method**: `DELETE`
- **Path**: `/api/v1/notices/{articleId}`
- **설명**: 공지사항을 삭제합니다.

#### 경로 파라미터

- `articleId`: 삭제할 공지사항 ID

#### 응답

- **Status**: `204 No Content`

### 2.5 공지사항 목록 조회

- **Method**: `GET`
- **Path**: `/api/v1/notices`
- **설명**: 공지사항 목록을 페이징하여 조회합니다.

#### 쿼리 파라미터

| 파라미터 | 타입      | 필수  | 설명              | 기본값 |
|------|---------|-----|-----------------|-----|
| page | Integer | 아니오 | 페이지 번호 (0부터 시작) | 0   |
| size | Integer | 아니오 | 페이지 크기          | 10  |

#### 응답

- **Status**: `200 OK`
- **Body**: `Page<ArticleBaseResponse>`

## 3. 이벤트 API (`/api/v1/events`)

### 3.1 이벤트 게시글 생성

- **Method**: `POST`
- **Path**: `/api/v1/events`
- **설명**: 새로운 이벤트 게시글을 생성합니다.

#### 요청 바디

`ArticleCreateRequest` (eventStartDate, eventEndDate 필수)

#### 응답

- **Status**: `200 OK`
- **Body**: `EventArticleResponse`

### 3.2 이벤트 게시글 수정

- **Method**: `PUT`
- **Path**: `/api/v1/events/{articleId}`
- **설명**: 기존 이벤트 게시글을 수정합니다.

#### 경로 파라미터

- `articleId`: 수정할 이벤트 게시글 ID

#### 응답

- **Status**: `200 OK`
- **Body**: `EventArticleResponse`

### 3.3 이벤트 게시글 조회

- **Method**: `GET`
- **Path**: `/api/v1/events/{articleId}`
- **설명**: 특정 이벤트 게시글의 상세 정보를 조회합니다.

#### 경로 파라미터

- `articleId`: 조회할 이벤트 게시글 ID

#### 응답

- **Status**: `200 OK`
- **Body**: `EventArticleResponse`

### 3.4 이벤트 게시글 삭제

- **Method**: `DELETE`
- **Path**: `/api/v1/events/{articleId}`
- **설명**: 이벤트 게시글을 삭제합니다.

#### 경로 파라미터

- `articleId`: 삭제할 이벤트 게시글 ID

#### 응답

- **Status**: `204 No Content`

### 3.5 이벤트 목록 조회

- **Method**: `GET`
- **Path**: `/api/v1/events`
- **설명**: 이벤트 목록을 페이징하여 조회합니다. 상태별 필터링을 지원합니다.

#### 쿼리 파라미터

| 파라미터   | 타입      | 필수  | 설명              | 기본값   |
|--------|---------|-----|-----------------|-------|
| status | String  | 아니오 | 이벤트 상태 필터       | "all" |
| page   | Integer | 아니오 | 페이지 번호 (0부터 시작) | 0     |
| size   | Integer | 아니오 | 페이지 크기          | 10    |

#### 상태 필터 옵션

- `all`: 전체
- `ongoing`: 진행중
- `ended`: 종료
- `upcoming`: 예정

#### 응답

- **Status**: `200 OK`
- **Body**: `Page<EventArticleResponse>`

## 4. 벌크 조회 API (`/api/v1/bulk`)

### 4.1 다중 게시글 조회

- **Method**: `GET`
- **Path**: `/api/v1/bulk/articles`
- **설명**: 여러 게시글을 한 번에 조회합니다.

#### 쿼리 파라미터

- `ids`: 조회할 게시글 ID 목록 (예: `?ids=1&ids=2&ids=3`)

#### 응답

- **Status**: `200 OK`
- **Body**: `List<ArticleSimpleResponse>`

```json
[
  {
    "articleId": "1",
    "title": "제목1",
    "writerId": "user1",
    "boardName": "게시판1",
    "viewCount": 100,
    "createdAt": "2024-01-01T00:00:00"
  },
  {
    "articleId": "2",
    "title": "제목2",
    "writerId": "user2",
    "boardName": "게시판2",
    "viewCount": 50,
    "createdAt": "2024-01-02T00:00:00"
  }
]
```

## 5. 열거형 정보 API (`/api/v1/enums`)

### 5.1 사용 가능한 열거형 목록 조회

- **Method**: `GET`
- **Path**: `/api/v1/enums`
- **설명**: 시스템에서 사용하는 모든 열거형 값 목록을 조회합니다.

#### 응답

- **Status**: `200 OK`
- **Body**:

```json
{
  "status": ["ACTIVE", "DELETED", "BLOCKED"],
  "eventStatus": ["UPCOMING", "ONGOING", "ENDED"],
  "articleType": ["REGULAR", "NOTICE", "EVENT"]
}
```

### 5.2 게시판 목록 조회

- **Method**: `GET`
- **Path**: `/api/v1/enums/boards`
- **설명**: 사용 가능한 게시판 목록을 조회합니다.

#### 응답

- **Status**: `200 OK`
- **Body**:

```json
{
  "1": {
    "id": 1,
    "name": "자유게시판",
    "url": "free-board",
    "description": "자유롭게 글을 작성할 수 있는 게시판"
  },
  "2": {
    "id": 2,
    "name": "공지사항",
    "url": "notice-board",
    "description": "공지사항 게시판"
  }
}
```

### 5.3 키워드 목록 조회

- **Method**: `GET`
- **Path**: `/api/v1/enums/keywords`
- **설명**: 사용 가능한 키워드 목록을 조회합니다.

#### 응답

- **Status**: `200 OK`
- **Body**:

```json
{
  "1": {
    "id": 1,
    "name": "개발",
    "url": "development",
    "category": "기술"
  },
  "2": {
    "id": 2,
    "name": "디자인",
    "url": "design",
    "category": "디자인"
  }
}
```

## 6. 헬스 체크 API (`/health`)

### 6.1 기본 헬스 체크

- **Method**: `GET`
- **Path**: `/health`
- **설명**: 서버의 기본 상태를 확인합니다.

#### 응답

- **Status**: `200 OK`
- **Body**:

```json
{
  "status": "UP",
  "timestamp": "2024-12-07T10:00:00",
  "service": "article-server"
}
```

### 6.2 상세 헬스 체크

- **Method**: `GET`
- **Path**: `/health/detail`
- **설명**: 서버의 상세 상태 정보를 조회합니다. (메모리, 시스템 정보 포함)

#### 응답

- **Status**: `200 OK`
- **Body**:

```json
{
  "status": "UP",
  "timestamp": "2024-12-07T10:00:00",
  "service": "article-server",
  "memory": {
    "max": "2048 MB",
    "total": "1024 MB",
    "free": "512 MB",
    "used": "512 MB"
  },
  "system": {
    "processors": 4,
    "javaVersion": "17.0.1",
    "osName": "Mac OS X",
    "osVersion": "14.0"
  }
}
```

### 6.3 Liveness 체크

- **Method**: `GET`
- **Path**: `/health/liveness`
- **설명**: Kubernetes용 Liveness 프로브. 애플리케이션 생존 여부를 확인합니다.

#### 응답

- **Status**: `200 OK`
- **Body**:

```json
{
  "status": "ALIVE"
}
```

### 6.4 Readiness 체크

- **Method**: `GET`
- **Path**: `/health/readiness`
- **설명**: Kubernetes용 Readiness 프로브. 트래픽 수신 준비 상태를 확인합니다.

#### 응답

- **Status**: `200 OK`
- **Body**:

```json
{
  "status": "READY"
}
```

## 공통 응답 구조

### ArticleBaseResponse

모든 게시글 응답의 기본 구조입니다.

```json
{
  "articleId": "게시글ID",
  "title": "제목",
  "content": "내용",
  "writerId": "작성자ID",
  "board": {
    "id": 1,
    "name": "게시판명",
    "url": "board-url"
  },
  "status": "ACTIVE",
  "viewCount": 100,
  "firstImageUrl": "https://example.com/image.jpg",
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00",
  "images": [
    {
      "id": "image1",
      "url": "https://example.com/image1.jpg"
    }
  ],
  "keywords": [
    {
      "id": 1,
      "name": "키워드1",
      "url": "keyword1"
    }
  ]
}
```

### EventArticleResponse

이벤트 게시글 전용 응답 구조입니다. `ArticleBaseResponse`의 모든 필드를 포함하며, 추가로 다음 필드들을 가집니다:

```json
{
  // ArticleBaseResponse의 모든 필드 +
  "eventStartDate": "2024-01-01T00:00:00",
  "eventEndDate": "2024-12-31T23:59:59",
  "eventStatus": "ONGOING"  // UPCOMING, ONGOING, ENDED
}
```

### NoticeArticleResponse

공지사항 전용 응답 구조입니다. `ArticleBaseResponse`의 모든 필드를 포함하며, 공지사항 특화 필드를 추가할 수 있습니다.

### ArticleCursorPageResponse

커서 기반 페이징 응답 구조입니다.

```json
{
  "content": [
    // ArticleBaseResponse 리스트
  ],
  "nextCursorId": "다음페이지커서ID",
  "hasNext": true,
  "pageSize": 10
}
```

### Page Response (Spring Data)

Spring Data의 표준 페이징 응답 구조입니다.

```json
{
  "content": [
    // 데이터 리스트
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "unsorted": true
    },
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 100,
  "totalPages": 10,
  "last": false,
  "first": true,
  "numberOfElements": 10,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": false,
    "unsorted": true
  }
}
```

## 에러 응답

### 에러 응답 구조

```json
{
  "timestamp": "2024-12-07T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/articles",
  "errors": [
    {
      "field": "title",
      "message": "제목은 필수입니다"
    },
    {
      "field": "boardIds",
      "message": "존재하지 않는 Board ID입니다"
    }
  ]
}
```

### 주요 에러 코드

| 에러 코드                | 설명           | 예시 메시지                               |
|----------------------|--------------|--------------------------------------|
| ARTICLE_NOT_FOUND    | 게시글을 찾을 수 없음 | "해당 게시글을 찾을 수 없습니다: {articleId}"     |
| INVALID_BOARD_ID     | 잘못된 게시판 ID   | "존재하지 않는 Board ID입니다: {boardId}"     |
| INVALID_KEYWORD_ID   | 잘못된 키워드 ID   | "존재하지 않는 Keyword ID입니다: {keywordId}" |
| INVALID_EVENT_PERIOD | 잘못된 이벤트 기간   | "이벤트 시작일은 종료일보다 이전이어야 합니다"           |
| VALIDATION_ERROR     | 유효성 검사 실패    | "요청 데이터가 유효하지 않습니다"                  |

## 페이징

### 오프셋 기반 페이징

공지사항과 이벤트 목록 조회에서 사용됩니다.

- `page`: 페이지 번호 (0부터 시작)
- `size`: 페이지 크기

### 커서 기반 페이징

게시글 검색에서 사용됩니다. 대량의 데이터를 효율적으로 처리할 수 있습니다.

- `cursorId`: 이전 응답에서 받은 `nextCursorId` 값
- `size`: 페이지 크기

#### 사용 예시

1. 첫 번째 요청:
   ```
   GET /api/v1/articles/search?size=10
   ```

2. 다음 페이지 요청:
   ```
   GET /api/v1/articles/search?size=10&cursorId=abc123
   ```

## 참고 사항

### 게시글 타입별 특징

- **일반 게시글 (REGULAR)**: 기본적인 게시글 기능
- **공지사항 (NOTICE)**: 관리자 전용, 상단 고정 기능 (예정)
- **이벤트 (EVENT)**: 기간 설정 가능, 자동 상태 관리

### 소프트 삭제

삭제 API 호출 시 실제로 데이터가 삭제되지 않고 `status`가 `DELETED`로 변경됩니다.

### 이미지 처리

- 게시글 생성 시 이미지 URL을 함께 전송
- `firstImageUrl`은 자동으로 첫 번째 이미지로 설정됨

### 키워드 매핑

- 게시글과 키워드는 다대다 관계
- 하나의 게시글에 여러 키워드 지정 가능

## 개발 환경

### 로컬 개발 서버

```bash
./gradlew bootRun
```

### Docker Compose

```bash
docker-compose up -d
```

### 테스트

```bash
./gradlew test
```

## 관련 문서

- [아키텍처 개요](./00-ARCHITECTURE-OVERVIEW.md)
- [개발 환경 설정](./01-DEVELOPMENT-SETUP.md)
- [Docker 설정](./DOCKER-SETUP.md)
- [성능 최적화 가이드](./README-PERFORMANCE.md)
- [유효성 검증 가이드](./guides/VALIDATION_GUIDE.md)

# 엔티티 리팩토링 요약

## 1. 리팩토링 목적

키워드 요구사항 변경으로 인한 엔티티 구조 개선:

- **보드별 전용 키워드 지원**: 각 게시판마다 고유한 키워드 관리
- **공통 키워드 지원**: 모든 게시판에서 사용 가능한 키워드
- **객체지향적 패키지 구조**: 도메인별로 패키지 분리
- **연관관계 편의 메서드 보강**: 양방향 관계 관리 개선

## 2. 패키지 구조 변경

### 기존 구조

```
entity/
├── Article.java
├── Board.java
├── Keyword.java
├── KeywordMappingTable.java
├── ArticleImage.java
└── articleType/
    ├── RegularArticle.java
    ├── EventArticle.java
    └── NoticeArticle.java
```

### 개선된 구조

```
entity/
├── board/
│   └── Board.java
├── article/
│   └── Article.java
├── keyword/
│   ├── Keyword.java
│   └── KeywordMappingTable.java
├── image/
│   └── ArticleImage.java
└── articleType/
    ├── RegularArticle.java
    ├── EventArticle.java
    └── NoticeArticle.java
```

## 3. 주요 변경사항

### 3.1 Keyword 엔티티 (entity/keyword/Keyword.java)

**추가된 필드:**

- `board` (ManyToOne): 키워드가 속한 게시판 (null = 공통 키워드)
- `usageCount` (Integer): 키워드 사용 빈도 추적
- `isActive` (Boolean): 키워드 활성화 여부

**추가된 편의 메서드:**

- `assignToBoard(Board board)`: 보드 할당 (양방향 관계 설정)
- `detachFromBoard()`: 보드에서 분리
- `isCommonKeyword()`: 공통 키워드 여부 확인
- `belongsToBoard(Board board)`: 특정 보드 키워드 여부 확인
- `incrementUsageCount()` / `decrementUsageCount()`: 사용 빈도 관리
- `activate()` / `deactivate()`: 활성화 상태 관리

**제약 조건:**

- Unique constraint: (keyword_name, board_id)
- 같은 이름의 키워드가 보드별로 존재 가능
- 공통 키워드는 board_id가 null

### 3.2 Board 엔티티 (entity/board/Board.java)

**추가된 컬렉션:**

- `keywords` (OneToMany): 게시판 전용 키워드 목록

**추가된 편의 메서드:**

- `addKeyword(Keyword keyword)`: 키워드 추가 (양방향 관계 설정)
- `removeKeyword(Keyword keyword)`: 키워드 제거
- `clearKeywords()`: 모든 키워드 제거
- `findKeywordByName(String name)`: 이름으로 키워드 조회
- `getActiveKeywords()`: 활성화된 키워드만 조회

**비즈니스 로직 추가:**

- `updateInfo(String name, String description)`: 게시판 정보 업데이트
- `activate()` / `deactivate()`: 활성화 상태 관리
- `updateDisplayOrder(Integer order)`: 표시 순서 변경
- `getArticleCount()` / `getKeywordCount()`: 카운트 조회

### 3.3 Article 엔티티 (entity/article/Article.java)

**필드명 변경:**

- `keywords` → `keywordMappings` (명확성 향상)

**추가된 필드:**

- `viewCount` (Long): 조회 수 추적

**개선된 이미지 관리:**

- `addImage(String imageId, String imageUrl)`: 시퀀스 자동 관리
- `removeImage(ArticleImage image)`: 대표 이미지 자동 재설정
- `removeImages()`: 모든 이미지 제거
- `setFirstImageUrl(String imageUrl)`: 대표 이미지 설정

**개선된 키워드 관리:**

- `addKeyword(Keyword keyword)`: 중복 체크 추가
- `addKeywords(List<Keyword> keywords)`: 일괄 추가
- `removeKeyword(Keyword keyword)`: 양방향 관계 정리
- `removeKeywords()`: 모든 키워드 제거
- `replaceKeywords(List<Keyword> newKeywords)`: 키워드 교체
- `getBoardKeywords()`: 게시판 전용 키워드만 필터링
- `getCommonKeywords()`: 공통 키워드만 필터링

**비즈니스 로직 추가:**

- `updateContent(String title, String content)`: 내용 수정
- `delete()` / `block()` / `activate()`: 상태 관리 (Soft Delete)
- `incrementViewCount()`: 조회 수 증가
- `isActive()` / `isDeleted()` / `isBlocked()`: 상태 확인
- `isWrittenBy(String userId)`: 작성자 확인

### 3.4 ArticleImage 엔티티 (entity/image/ArticleImage.java)

**추가된 편의 메서드:**

- `updateImageUrl(String newImageUrl)`: 이미지 URL 변경
- `updateImageId(String newImageId)`: 이미지 ID 변경
- `updateSequence(Long newSequence)`: 순서 변경
- `getArticleId()`: 복합키에서 게시글 ID 추출
- `getSequence()`: 복합키에서 순서 추출

### 3.5 KeywordMappingTable 엔티티 (entity/keyword/KeywordMappingTable.java)

**추가된 필드:**

- `createdAt` (LocalDateTime): 매핑 생성 시간

**추가된 편의 메서드:**

- `getArticleId()`: 복합키에서 게시글 ID 추출
- `getKeywordId()`: 복합키에서 키워드 ID 추출
- `detach()`: 양방향 관계 정리

### 3.6 Article 서브클래스 (entity/articleType/)

**임포트 경로 수정:**

- `entity.Article` → `entity.article.Article`

## 4. 데이터베이스 마이그레이션 필요사항

### 4.1 keywords 테이블 변경

```sql
-- board_id 컬럼 추가 (nullable, 공통 키워드는 NULL)
ALTER TABLE keywords ADD COLUMN board_id BIGINT NULL;

-- 사용 빈도 컬럼 추가
ALTER TABLE keywords ADD COLUMN usage_count INT NOT NULL DEFAULT 0;

-- 활성화 여부 컬럼 추가
ALTER TABLE keywords ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

-- 외래키 제약 조건 추가
ALTER TABLE keywords ADD CONSTRAINT fk_keyword_board
  FOREIGN KEY (board_id) REFERENCES boards(board_id);

-- Unique 제약 조건 추가
ALTER TABLE keywords ADD CONSTRAINT uk_keyword_board
  UNIQUE (keyword_name, board_id);

-- 인덱스 추가
CREATE INDEX idx_keyword_board ON keywords(board_id);
CREATE INDEX idx_keyword_name ON keywords(keyword_name);
```

### 4.2 keyword_mapping_table 테이블 변경

```sql
-- 생성 시간 컬럼 추가
ALTER TABLE keyword_mapping_table
  ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 인덱스 추가
CREATE INDEX idx_mapping_created ON keyword_mapping_table(created_at);
```

### 4.3 articles 테이블 변경

```sql
-- 조회 수 컬럼 추가 (기존에 없었다면)
ALTER TABLE articles ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
```

## 5. 코드 마이그레이션 가이드

### 5.1 Repository 계층

**변경 필요사항:**

- 임포트 경로 수정: `entity.Article` → `entity.article.Article`
- 키워드 조회 쿼리 수정: 보드별/공통 키워드 구분

**예시:**

```java
// 게시판 전용 키워드 조회
List<Keyword> findByBoardAndIsActiveTrue(Board board);

// 공통 키워드 조회
List<Keyword> findByBoardIsNullAndIsActiveTrue();

// 특정 게시판에서 사용 가능한 모든 키워드 (보드 전용 + 공통)
@Query("SELECT k FROM Keyword k WHERE (k.board = :board OR k.board IS NULL) AND k.isActive = true")
List<Keyword> findAvailableKeywordsForBoard(@Param("board") Board board);
```

### 5.2 Service 계층

**주요 변경사항:**

1. 키워드 생성 시 보드 지정 여부 결정
2. 편의 메서드 활용으로 양방향 관계 자동 관리
3. 필터링 메서드 활용 (getBoardKeywords, getCommonKeywords)

**예시:**

```java
// 게시판 전용 키워드 생성
Keyword boardKeyword = Keyword.builder()
    .name("공지")
    .board(board)
    .build();
board.addKeyword(boardKeyword); // 양방향 관계 자동 설정

// 공통 키워드 생성
Keyword commonKeyword = Keyword.builder()
    .name("긴급")
    .board(null) // null = 공통 키워드
    .build();

// 게시글에 키워드 추가 (중복 자동 체크)
article.addKeyword(keyword);

// 게시글의 보드 전용 키워드만 조회
List<Keyword> boardKeywords = article.getBoardKeywords();

// 키워드 교체
article.replaceKeywords(newKeywordList);
```

### 5.3 Controller 계층

**추가 가능한 엔드포인트:**

```java
// 게시판별 키워드 조회
GET /api/boards/{boardId}/keywords

// 공통 키워드 조회
GET /api/keywords/common

// 특정 게시판에서 사용 가능한 모든 키워드
GET /api/boards/{boardId}/available-keywords
```

## 6. 주요 개선 효과

### 6.1 도메인 분리

- 각 도메인(board, article, keyword, image)이 명확히 분리
- 패키지 구조만으로 도메인 이해 가능
- 유지보수성 향상

### 6.2 키워드 관리 유연성

- 보드별 전용 키워드와 공통 키워드 동시 지원
- 키워드 사용 빈도 추적으로 인기 키워드 분석 가능
- 키워드 활성화/비활성화로 사용 제어 가능

### 6.3 연관관계 관리 개선

- 양방향 관계 설정/해제 자동화
- 편의 메서드로 일관성 보장
- 비즈니스 로직이 엔티티에 캡슐화

### 6.4 데이터 무결성

- Unique 제약 조건으로 중복 방지
- 외래키 제약 조건으로 참조 무결성 보장
- 연관관계 편의 메서드로 양방향 동기화

## 7. 테스트 시나리오

### 7.1 키워드 시나리오

```java
// 1. 보드 전용 키워드 생성
Board techBoard = boardRepository.findByName("기술");
Keyword javaKeyword = new Keyword("Java", techBoard);
techBoard.addKeyword(javaKeyword);

// 2. 공통 키워드 생성
Keyword urgentKeyword = new Keyword("긴급", null);
keywordRepository.save(urgentKeyword);

// 3. 게시글에 두 종류 키워드 모두 추가
article.addKeyword(javaKeyword);    // 보드 전용
article.addKeyword(urgentKeyword);  // 공통

// 4. 필터링
List<Keyword> boardKeywords = article.getBoardKeywords();   // [Java]
List<Keyword> commonKeywords = article.getCommonKeywords(); // [긴급]

// 5. 사용 빈도 확인
assertEquals(1, javaKeyword.getUsageCount());
```

### 7.2 연관관계 시나리오

```java
// 1. 게시판에 게시글 추가 (양방향 자동 설정)
board.addArticle(article);
assertTrue(article.getBoard().equals(board));

// 2. 게시글에 이미지 추가 (시퀀스 자동 관리)
article.addImage("img-001", "https://example.com/1.jpg");
article.addImage("img-002", "https://example.com/2.jpg");
assertEquals("https://example.com/1.jpg", article.getFirstImageUrl());

// 3. 이미지 제거 시 대표 이미지 자동 재설정
article.removeImage(article.getImages().get(0));
assertEquals("https://example.com/2.jpg", article.getFirstImageUrl());

// 4. 키워드 제거 시 양방향 정리
article.removeKeyword(javaKeyword);
assertFalse(javaKeyword.getMappings().contains(mapping));
```

## 8. 주의사항

### 8.1 기존 데이터 마이그레이션

- 기존 키워드는 모두 공통 키워드(board_id = NULL)로 설정됨
- 필요시 수동으로 보드별 키워드 재분류 필요

### 8.2 성능 고려사항

- Lazy Loading 유지: 필요시에만 연관 엔티티 로딩
- 키워드 조회 시 보드 조건 인덱스 활용
- 사용 빈도(usageCount)는 캐싱 최적화에 활용 가능

### 8.3 트랜잭션 관리

- 편의 메서드 사용 시에도 트랜잭션 필수
- 양방향 관계 설정은 영속성 컨텍스트 내에서 수행
- 벌크 연산 시 1차 캐시 동기화 주의

## 9. 다음 단계

1. **데이터베이스 마이그레이션 스크립트 작성 및 실행**
2. **Repository 계층 임포트 및 쿼리 수정**
3. **Service 계층 키워드 로직 업데이트**
4. **Controller 계층 엔드포인트 추가/수정**
5. **단위 테스트 및 통합 테스트 작성**
6. **기존 데이터 마이그레이션 (키워드 재분류)**

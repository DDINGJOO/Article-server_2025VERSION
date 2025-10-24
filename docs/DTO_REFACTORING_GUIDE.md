# DTO 리팩토링 가이드

## 📋 개요

게시글 응답 DTO를 **상속 구조**로 리팩토링하여 중복 코드를 제거하고 유지보수성을 향상시켰습니다.

## 🎯 리팩토링 목표

1. ✅ **중복 코드 제거**: 35줄의 중복 코드 → 공통 베이스 클래스로 통합
2. ✅ **타입 안전성**: 게시글 타입별로 명확한 Response DTO
3. ✅ **확장성**: 새로운 게시글 타입 추가 시 최소한의 코드만 작성
4. ✅ **클린 코드**: Map 대신 명확한 객체 사용 (BoardInfo, ImageInfo, KeywordInfo)

## 📁 새로운 패키지 구조

```
dto/response/
├── article/
│   ├── ArticleBaseResponse.java          (추상 클래스 - 공통 필드)
│   ├── RegularArticleResponse.java       (일반 게시글)
│   ├── EventArticleResponse.java         (이벤트 게시글)
│   └── NoticeArticleResponse.java        (공지사항)
├── common/
│   ├── BoardInfo.java                    (게시판 정보)
│   ├── ImageInfo.java                    (이미지 정보)
│   └── KeywordInfo.java                  (키워드 정보)
├── ArticleResponse.java                  (팩토리 클래스)
└── ArticleSimpleResponse.java            (목록용 간단한 응답)
```

## 🏗️ 클래스 구조

### 1. ArticleBaseResponse (추상 클래스)

**공통 필드:**

```java
- articleId: String           // 게시글 ID
- title: String               // 제목
- content: String             // 내용
- writerId: String            // 작성자 ID
- board: BoardInfo            // 게시판 정보
- status: String              // 상태 (ACTIVE, DELETED, BLOCKED)
- viewCount: Long             // 조회 수
- firstImageUrl: String       // 대표 이미지 URL
- createdAt: LocalDateTime    // 생성 일시
- updatedAt: LocalDateTime    // 수정 일시
- images: List<ImageInfo>     // 이미지 목록
- keywords: List<KeywordInfo> // 키워드 목록
```

**핵심 메서드:**

```java
protected void setCommonFields(Article article)
```

- 하위 클래스에서 `fromEntity()` 구현 시 호출
- Article 엔티티로부터 공통 필드 자동 설정

### 2. 하위 클래스들

#### RegularArticleResponse (일반 게시글)

```java
public class RegularArticleResponse extends ArticleBaseResponse {
    // 추가 필드 없음

    public static RegularArticleResponse fromEntity(RegularArticle article) {
        RegularArticleResponse response = new RegularArticleResponse();
        response.setCommonFields(article);
        return response;
    }
}
```

#### EventArticleResponse (이벤트 게시글)

```java
public class EventArticleResponse extends ArticleBaseResponse {
    private LocalDateTime eventStartDate;  // 이벤트 시작일
    private LocalDateTime eventEndDate;    // 이벤트 종료일

    // 편의 메서드
    public boolean isOngoing()    // 진행 중 여부
    public boolean isEnded()      // 종료 여부
    public boolean isUpcoming()   // 시작 전 여부
}
```

#### NoticeArticleResponse (공지사항)

```java
public class NoticeArticleResponse extends ArticleBaseResponse {
    // 현재 추가 필드 없음
    // 향후 isPinned, priority 등 추가 가능
}
```

### 3. 공통 정보 클래스들

#### BoardInfo

```java
{
    "boardId": 1,
    "boardName": "기술",
    "description": "기술 관련 게시판"
}
```

#### ImageInfo

```java
{
    "imageId": "img-001",
    "imageUrl": "https://example.com/image.jpg",
    "sequence": 1
}
```

#### KeywordInfo

```java
{
    "keywordId": 1,
    "keywordName": "Java",
    "isCommon": false  // true: 공통 키워드, false: 보드 전용
}
```

### 4. ArticleResponse (팩토리 클래스)

**다형성 활용:**

```java
ArticleBaseResponse response = ArticleResponse.fromEntity(article);

// 내부적으로 instanceof 체크
if (article instanceof EventArticle) {
    return EventArticleResponse.fromEntity((EventArticle) article);
} else if (article instanceof NoticeArticle) {
    return NoticeArticleResponse.fromEntity((NoticeArticle) article);
} else if (article instanceof RegularArticle) {
    return RegularArticleResponse.fromEntity((RegularArticle) article);
}
```

## 🔄 사용 예시

### Service 계층

```java
@Service
public class ArticleService {

    // 방법 1: 자동 타입 변환 (추천)
    public ArticleBaseResponse getArticle(String articleId) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ArticleNotFoundException());

        // 자동으로 적절한 타입의 Response 생성
        return ArticleResponse.fromEntity(article);
    }

    // 방법 2: 명시적 타입 변환
    public EventArticleResponse getEventArticle(String articleId) {
        EventArticle article = (EventArticle) articleRepository.findById(articleId)
            .orElseThrow(() -> new ArticleNotFoundException());

        return ArticleResponse.fromEventArticle(article);
    }

    // 방법 3: 리스트 변환
    public List<ArticleBaseResponse> getArticles() {
        List<Article> articles = articleRepository.findAll();

        return articles.stream()
            .map(ArticleResponse::fromEntity)
            .collect(Collectors.toList());
    }
}
```

### Controller 계층

```java
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @GetMapping("/{articleId}")
    public ResponseEntity<ArticleBaseResponse> getArticle(@PathVariable String articleId) {
        ArticleBaseResponse response = articleService.getArticle(articleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ArticleBaseResponse>> getArticles() {
        List<ArticleBaseResponse> responses = articleService.getArticles();
        return ResponseEntity.ok(responses);
    }
}
```

## 📤 JSON 응답 예시

### 일반 게시글 (RegularArticleResponse)

```json
{
    "articleId": "ART-001",
    "title": "Spring Boot 시작하기",
    "content": "Spring Boot를 시작하는 방법...",
    "writerId": "user123",
    "board": {
        "boardId": 1,
        "boardName": "기술",
        "description": "기술 관련 게시판"
    },
    "status": "ACTIVE",
    "viewCount": 42,
    "firstImageUrl": "https://example.com/image1.jpg",
    "createdAt": "2025-10-24T10:00:00",
    "updatedAt": "2025-10-24T15:30:00",
    "images": [
        {
            "imageId": "img-001",
            "imageUrl": "https://example.com/image1.jpg",
            "sequence": 1
        }
    ],
    "keywords": [
        {
            "keywordId": 1,
            "keywordName": "Java",
            "isCommon": false
        },
        {
            "keywordId": 5,
            "keywordName": "긴급",
            "isCommon": true
        }
    ]
}
```

### 이벤트 게시글 (EventArticleResponse)

```json
{
    "articleId": "ART-002",
    "title": "해커톤 대회 안내",
    "content": "2025 해커톤 대회를 개최합니다...",
    "writerId": "admin",
    "board": {
        "boardId": 2,
        "boardName": "이벤트",
        "description": "이벤트 게시판"
    },
    "status": "ACTIVE",
    "viewCount": 150,
    "firstImageUrl": "https://example.com/event.jpg",
    "createdAt": "2025-10-20T09:00:00",
    "updatedAt": "2025-10-23T14:00:00",
    "images": [...],
    "keywords": [...],
    "eventStartDate": "2025-11-01T00:00:00",
    "eventEndDate": "2025-11-03T23:59:59"
}
```

## 🔧 변경 전후 비교

### 이전 (중복 코드)

```java
// ArticleResponse.java (65줄)
public class ArticleResponse {
    private String articleId;
    private String title;
    // ... 공통 필드 8개

    private Map<String, String> imageUrls;  // ❌ Map 사용
    private Map<Long, String> keywords;     // ❌ Map 사용

    public static ArticleResponse fromEntity(Article article) {
        // 35줄의 변환 로직
    }
}

// EventArticleResponse.java (72줄)
public class EventArticleResponse {
    private String articleId;
    private String title;
    // ... 동일한 공통 필드 8개 (중복!)

    private Map<String, String> imageUrls;  // ❌ 중복
    private Map<Long, String> keywords;     // ❌ 중복

    private LocalDateTime eventStartDate;   // 추가 필드
    private LocalDateTime eventEndDate;     // 추가 필드

    public static EventArticleResponse fromEntity(EventArticle article) {
        // 40줄의 변환 로직 (35줄은 중복!)
    }
}
```

### 이후 (상속 구조)

```java
// ArticleBaseResponse.java (공통 필드 정의)
public abstract class ArticleBaseResponse {
    private String articleId;
    private String title;
    // ... 공통 필드 12개

    private List<ImageInfo> images;      // ✅ 명확한 객체
    private List<KeywordInfo> keywords;  // ✅ 명확한 객체

    protected void setCommonFields(Article article) {
        // 공통 변환 로직 한 곳에만
    }
}

// EventArticleResponse.java (추가 필드만)
public class EventArticleResponse extends ArticleBaseResponse {
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;

    public static EventArticleResponse fromEntity(EventArticle article) {
        EventArticleResponse response = EventArticleResponse.builder()
            .eventStartDate(article.getEventStartDate())
            .eventEndDate(article.getEventEndDate())
            .build();
        response.setCommonFields(article);  // ✅ 재사용
        return response;
    }

    // ✅ 편의 메서드 추가
    public boolean isOngoing() { ... }
}
```

## ✅ 개선 효과

### 1. 코드 중복 제거

- **이전**: ArticleResponse 65줄 + EventArticleResponse 72줄 = **137줄**
- **이후**: ArticleBaseResponse 120줄 + EventArticleResponse 80줄 = **200줄** (전체)
	- 하지만 공통 로직은 **한 곳에만** 존재
	- 새로운 타입 추가 시 **10줄 미만**으로 가능

### 2. 유지보수성

- 공통 필드 추가/수정 시 **ArticleBaseResponse만 수정**
- 각 게시글 타입별 고유 로직은 **분리**되어 관리

### 3. 타입 안전성

```java
// ✅ 컴파일 타임에 타입 체크
EventArticleResponse response = ArticleResponse.fromEventArticle(article);
boolean isOngoing = response.isOngoing();  // ✅ 이벤트 전용 메서드 사용 가능

// ❌ 이전 방식: 런타임 에러 가능
Map<String, Object> response = ...;
LocalDateTime startDate = (LocalDateTime) response.get("eventStartDate");  // ❌ 타입 캐스팅 필요
```

### 4. 확장성

```java
// 새로운 게시글 타입 추가 시
public class PollArticleResponse extends ArticleBaseResponse {
    private List<String> options;
    private Map<String, Integer> votes;

    public static PollArticleResponse fromEntity(PollArticle article) {
        PollArticleResponse response = PollArticleResponse.builder()
            .options(article.getOptions())
            .votes(article.getVotes())
            .build();
        response.setCommonFields(article);  // ✅ 공통 로직 재사용
        return response;
    }
}
```

## 🚀 다음 단계

1. **Service 계층 수정**: 기존 `ArticleResponse.fromEntity()` 호출 코드 유지 가능
2. **Controller 계층 수정**: 반환 타입을 `ArticleBaseResponse`로 변경
3. **테스트 작성**: 각 Response 타입별 변환 테스트
4. **API 문서 업데이트**: Swagger/OpenAPI 스키마 업데이트

## 📝 마이그레이션 체크리스트

- [x] ArticleBaseResponse 추상 클래스 작성
- [x] BoardInfo, ImageInfo, KeywordInfo 공통 클래스 작성
- [x] RegularArticleResponse 작성
- [x] EventArticleResponse 리팩토링
- [x] NoticeArticleResponse 작성
- [x] ArticleResponse 팩토리 클래스 수정
- [x] 기존 EventArticleResponse.java 삭제
- [ ] Service 계층 임포트 경로 수정
- [ ] Controller 계층 반환 타입 수정
- [ ] 테스트 코드 작성
- [ ] API 문서 업데이트

---

**작성일**: 2025-10-24
**작성자**: Claude Code
**버전**: 1.0.0

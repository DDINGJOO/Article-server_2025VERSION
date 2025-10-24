# 어노테이션 기반 Validation 가이드

## 개요

기존의 여러 곳에 흩어져 있던 검증 로직을 어노테이션 기반으로 통합하여 코드의 가독성과 유지보수성을 향상시켰습니다.

## 주요 변경사항

### 1. ID 기반 통일

- ✅ 키워드와 보드를 **ID 기준으로만** 받도록 통일
- ✅ Convertor 로직 완전 제거
- ✅ 직접적인 Repository 조회로 변경

### 2. 커스텀 Validation 어노테이션 구현

- ✅ `@ValidBoardId` - Board ID 존재 여부 검증
- ✅ `@ValidKeywordIds` - Keyword ID 리스트 존재 여부 검증
- ✅ `@ValidEventPeriod` - 이벤트 기간 유효성 검증

### 3. 검증 로직 위치 변경

- **변경 전**: Service 레이어에서 수동 검증
- **변경 후**: DTO에 어노테이션 적용 → Controller 진입 시점에 자동 검증

## 커스텀 Validation 어노테이션

### 1. @ValidBoardId

**용도**: Board ID가 실제로 존재하는지 검증

**위치**: `src/main/java/com/teambind/articleserver/validation/ValidBoardId.java`

```java
@ValidBoardId
private Long boardIds;

// null 불허
@ValidBoardId(nullable = false)
private Long boardIds;
```

**검증 로직**:

```java
public class ValidBoardIdValidator implements ConstraintValidator<ValidBoardId, Long> {
    private final BoardRepository boardRepository;

    @Override
    public boolean isValid(Long boardId, ConstraintValidatorContext context) {
        if (boardId == null) {
            return nullable;  // nullable 옵션에 따라 결정
        }
        return boardRepository.existsById(boardId);
    }
}
```

### 2. @ValidKeywordIds

**용도**: Keyword ID 리스트의 모든 ID가 실제로 존재하는지 검증

**위치**: `src/main/java/com/teambind/articleserver/validation/ValidKeywordIds.java`

```java
@ValidKeywordIds
private List<Long> keywordIds;
```

**검증 로직**:

```java
public class ValidKeywordIdsValidator implements ConstraintValidator<ValidKeywordIds, List<Long>> {
    private final KeywordRepository keywordRepository;

    @Override
    public boolean isValid(List<Long> keywordIds, ConstraintValidatorContext context) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return true;  // null 또는 빈 리스트는 허용
        }

        // 모든 ID가 존재하는지 확인
        long existingCount = keywordRepository.countByIdIn(keywordIds);
        return existingCount == keywordIds.size();
    }
}
```

### 3. @ValidEventPeriod

**용도**: 이벤트 시작일과 종료일의 유효성 검증 (클래스 레벨)

**위치**: `src/main/java/com/teambind/articleserver/validation/ValidEventPeriod.java`

```java
@ValidEventPeriod
public class ArticleCreateRequest {
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
}
```

**검증 로직**:

```java
public class ValidEventPeriodValidator implements ConstraintValidator<ValidEventPeriod, Object> {
    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        // Reflection으로 필드 접근
        LocalDateTime startDate = getFieldValue(obj, "eventStartDate");
        LocalDateTime endDate = getFieldValue(obj, "eventEndDate");

        // 둘 다 null이면 통과
        if (startDate == null && endDate == null) {
            return true;
        }

        // 하나만 null이면 실패
        if (startDate == null || endDate == null) {
            return false;
        }

        // 종료일이 시작일보다 이전이면 실패
        return !endDate.isBefore(startDate);
    }
}
```

## DTO 적용 예시

### ArticleCreateRequest.java

**변경 전**:

```java
public class ArticleCreateRequest {
    private String title;
    private String content;
    private String writerId;
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
    private List<Integer> keywordIds;
    private Integer boardIds;
}
```

**변경 후**:

```java
@ValidEventPeriod  // 클래스 레벨 검증
public class ArticleCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    @NotBlank(message = "작성자 ID는 필수입니다")
    private String writerId;

    // 이벤트 기간 (클래스 레벨에서 검증됨)
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;

    // 키워드 ID 리스트 (선택)
    @ValidKeywordIds
    private List<Long> keywordIds;

    // Board ID (필수)
    @NotNull(message = "Board ID는 필수입니다")
    @ValidBoardId(nullable = false)
    private Long boardIds;
}
```

## Controller 적용

컨트롤러에서 `@Valid` 어노테이션만 추가하면 자동으로 검증됩니다:

```java
@PostMapping()
public ResponseEntity<EventArticleResponse> createEventArticle(
    @Valid @RequestBody ArticleCreateRequest request) {  // @Valid 추가
    EventArticle article = articleCreateService.createEventArticle(request);
    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
}
```

## Service 레이어 변경

### 변경 전

```java
public EventArticle createEventArticle(ArticleCreateRequest request) {
    // 수동 검증
    validateArticleInput(request.getTitle(), request.getContent(), request.getWriterId(), null);
    validateEventDates(request.getEventStartDate(), request.getEventEndDate());

    // Convertor로 변환
    List<Keyword> keywords = request.getKeywordIds() != null
        ? convertor.convertKeywords(request.getKeywordIds())
        : null;

    // 비즈니스 로직...
}

// 별도의 검증 메서드들
private void validateArticleInput(...) { }
private void validateEventDates(...) { }
```

### 변경 후

```java
public EventArticle createEventArticle(ArticleCreateRequest request) {
    // Validation 어노테이션으로 자동 검증됨 - 수동 검증 불필요!

    // ID로 직접 조회
    List<Keyword> keywords = request.getKeywordIds() != null && !request.getKeywordIds().isEmpty()
        ? keywordRepository.findAllById(request.getKeywordIds())
        : null;

    // 비즈니스 로직...
}

// 검증 메서드 삭제됨!
```

## 검증 실패 시 응답

검증 실패 시 Spring이 자동으로 `MethodArgumentNotValidException`을 발생시키며, 다음과 같은 형식의 응답이 반환됩니다:

```json
{
  "timestamp": "2025-10-24T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "title",
      "message": "제목은 필수입니다"
    },
    {
      "field": "boardIds",
      "message": "존재하지 않는 Board ID입니다"
    },
    {
      "field": "keywordIds",
      "message": "존재하지 않는 Keyword ID가 포함되어 있습니다"
    }
  ]
}
```

## 검증 흐름

```
┌─────────────────┐
│   HTTP Request  │
└────────┬────────┘
         │
         v
┌─────────────────┐
│   Controller    │ @Valid 어노테이션
│   @Valid @RequestBody request
└────────┬────────┘
         │
         v
┌─────────────────┐
│   Validation    │ 자동 검증 수행
│   - @NotBlank   │
│   - @ValidBoardId
│   - @ValidKeywordIds
│   - @ValidEventPeriod
└────────┬────────┘
         │
         ├─ [실패] → MethodArgumentNotValidException (400)
         │
         └─ [성공] → Service 레이어로 진행
                     │
                     v
              ┌──────────────────┐
              │  Service Layer   │ 검증 로직 없음!
              │  비즈니스 로직만  │
              └──────────────────┘
```

## 장점

### 1. 관심사의 분리

- **Controller**: 요청/응답 처리
- **Validation**: DTO 검증 (어노테이션)
- **Service**: 비즈니스 로직
- **Repository**: 데이터 접근

### 2. 코드 간결화

| 항목            | 변경 전 | 변경 후    | 개선          |
|---------------|------|---------|-------------|
| Service 검증 로직 | 30줄  | 0줄      | **100% 감소** |
| DTO 검증 선언     | 0줄   | 10줄     | **명시적**     |
| Convertor 로직  | 150줄 | 0줄      | **완전 제거**   |
| 검증 위치         | 여러 곳 | DTO 한 곳 | **통합**      |

### 3. 일관성

- 모든 요청이 동일한 검증 프로세스를 거침
- 검증 규칙이 DTO에 명시적으로 선언됨
- 검증 로직 중복 제거

### 4. 테스트 용이성

```java
@Test
void testInvalidBoardId() {
    ArticleCreateRequest request = ArticleCreateRequest.builder()
        .title("테스트")
        .content("내용")
        .writerId("user1")
        .boardIds(9999L)  // 존재하지 않는 ID
        .build();

    // Validator를 직접 테스트 가능
    Set<ConstraintViolation<ArticleCreateRequest>> violations =
        validator.validate(request);

    assertFalse(violations.isEmpty());
    assertEquals("존재하지 않는 Board ID입니다",
        violations.iterator().next().getMessage());
}
```

## 추가된 Repository 메서드

### KeywordRepository

```java
// ID 리스트로 존재 개수 확인 (Validation용)
long countByIdIn(List<Long> ids);
```

## 제거된 파일/로직

### 제거됨

- ✅ `Convertor` 인터페이스 및 구현체 사용 제거
- ✅ `validateArticleInput()` 메서드
- ✅ `validateEventDates()` 메서드
- ✅ Service 레이어의 모든 수동 검증 로직

### 유지됨

- DataInitializer (캐싱용)
- Validator 인터페이스 (필요시 재사용 가능)

## API 변경사항

### 요청 파라미터 타입 변경

**변경 전**:

```http
GET /api/articles/search?board=자유게시판&keyword=java,spring
```

**변경 후**:

```http
GET /api/articles/search?boardIds=1&keyword=1&keyword=2
```

### 요청 Body 변경

**변경 전**:

```json
{
  "title": "제목",
  "content": "내용",
  "writerId": "user1",
  "keywordIds": [1, 2, 3],  // Integer
  "boardIds": 1              // Integer
}
```

**변경 후**:

```json
{
  "title": "제목",
  "content": "내용",
  "writerId": "user1",
  "keywordIds": [1, 2, 3],  // Long
  "boardIds": 1              // Long
}
```

## 성능 고려사항

### 검증 시점

- **변경 전**: Service 레이어 진입 후 검증 (불필요한 트랜잭션 시작 가능)
- **변경 후**: Controller 진입 시 검증 (트랜잭션 시작 전 차단)

### 데이터베이스 쿼리

- `@ValidBoardId`: 1개의 `existsById` 쿼리
- `@ValidKeywordIds`: 1개의 `countByIdIn` 쿼리 (배치 조회)

**최적화**:

- `countByIdIn`은 `findAllById`보다 빠름 (전체 엔티티 로드 불필요)
- `existsById`는 `findById`보다 빠름 (존재 여부만 확인)

## 향후 개선 사항

1. **캐시 활용**
   ```java
   @Cacheable("boards")
   public boolean existsById(Long id) { }
   ```

2. **배치 검증**
	- 여러 검증을 한 번의 쿼리로 처리

3. **커스텀 예외 핸들러**
   ```java
   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<ErrorResponse> handleValidationErrors(...) {
       // 커스텀 에러 응답 포맷
   }
   ```

4. **검증 그룹**
   ```java
   public interface CreateGroup {}
   public interface UpdateGroup {}

   @NotNull(groups = CreateGroup.class)
   private Long boardIds;
   ```

## 참고 자료

- [Spring Validation 공식 문서](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
- [Jakarta Bean Validation](https://beanvalidation.org/3.0/)
- [Custom Constraint](https://docs.jboss.org/hibernate/validator/7.0/reference/en-US/html_single/#validator-customconstraints)

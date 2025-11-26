# Validation 가이드

## 개요

어노테이션 기반 커스텀 Validation 시스템입니다.

## 커스텀 Validator

### 1. @ValidBoardId

Board ID 존재 여부를 검증합니다.

#### 사용법

```java

@NotNull(message = "Board ID는 필수입니다")
@ValidBoardId(nullable = false)
private Long boardIds;
```

#### 옵션

- `nullable`: true/false (기본값: true)
- `message`: 커스텀 에러 메시지

#### 동작

- DataInitializer.boardMap.containsKey()로 검증
- DB 쿼리 없이 O(1) 시간 복잡도
- 캐시 미스 시 validation 실패

### 2. @ValidKeywordIds

Keyword ID 리스트의 모든 ID가 존재하는지 검증합니다.

#### 사용법

```java

@ValidKeywordIds
private List<Long> keywordIds;
```

#### 동작

- null 또는 빈 리스트는 허용
- Stream API로 모든 ID 존재 여부 확인
- DataInitializer.keywordMap 활용
- DB 쿼리 없이 검증

### 3. @ValidEventPeriod

이벤트 시작일과 종료일의 관계를 검증합니다.

#### 사용법

```java

@ValidEventPeriod
public class ArticleCreateRequest {
	private LocalDateTime eventStartDate;
	private LocalDateTime eventEndDate;
}
```

#### 검증 규칙

1. 둘 다 null: 통과 (선택적 필드)
2. 하나만 null: 실패
3. 종료일 < 시작일: 실패
4. 종료일 >= 시작일: 통과

#### 커스텀 필드명

```java
@ValidEventPeriod(
		startDateField = "startDate",
		endDateField = "endDate"
)
```

## 사용 예시

### DTO 정의

```java

@Data
@ValidEventPeriod
public class ArticleCreateRequest {
	
	@NotBlank(message = "제목은 필수입니다")
	private String title;
	
	@NotBlank(message = "내용은 필수입니다")
	private String content;
	
	@NotNull(message = "Board ID는 필수입니다")
	@ValidBoardId(nullable = false)
	private Long boardIds;
	
	@ValidKeywordIds
	private List<Long> keywordIds;
	
	private LocalDateTime eventStartDate;
	private LocalDateTime eventEndDate;
}
```

### Controller에서 사용

```java

@PostMapping
public ResponseEntity<ArticleResponse> createArticle(
		@Valid @RequestBody ArticleCreateRequest request) {
	// @Valid가 자동으로 모든 검증 수행
	Article article = articleCreateService.createArticle(request);
	return ResponseEntity.ok(ArticleResponse.fromEntity(article));
}
```

### 에러 응답

validation 실패 시:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
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

## Before/After 비교

### Before (수동 검증)

```java
public Article createArticle(ArticleCreateRequest request) {
	// 수동 검증 코드 (30+ 줄)
	if (request.getBoardIds() == null) {
		throw new CustomException(ErrorCode.INVALID_BOARD_ID);
	}
	Board board = boardRepository.findById(request.getBoardIds())
			.orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
	
	if (request.getKeywordIds() != null) {
		for (Long keywordId : request.getKeywordIds()) {
			if (!keywordRepository.existsById(keywordId)) {
				throw new CustomException(ErrorCode.KEYWORD_NOT_FOUND);
			}
		}
	}
	
	if (request.getEventStartDate() != null && request.getEventEndDate() != null) {
		if (request.getEventEndDate().isBefore(request.getEventStartDate())) {
			throw new CustomException(ErrorCode.INVALID_EVENT_PERIOD);
		}
	}
	
	// 실제 비즈니스 로직...
}
```

### After (어노테이션 검증)

```java
public Article createArticle(@Valid ArticleCreateRequest request) {
	// 검증은 이미 완료됨
	// 바로 비즈니스 로직 시작
	Board board = boardRepository.findById(request.getBoardIds()).get();
	// ...
}
```

## 성능

- DB 쿼리 제거로 약 1000배 성능 향상
- 상세 내용은 [VALIDATION_PERFORMANCE_OPTIMIZATION.md](../VALIDATION_PERFORMANCE_OPTIMIZATION.md) 참조

## 작성일

2025-10-25

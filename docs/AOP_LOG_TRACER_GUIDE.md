# AOP Log Tracer 가이드

## 개요

@LogTrace 어노테이션을 사용한 메서드 실행 추적 및 성능 모니터링 시스템입니다.

## 주요 기능

1. **메서드 실행 시간 측정**
2. **파라미터 로깅** (옵션)
3. **결과 로깅** (옵션)
4. **TraceId 생성** (요청 추적)
5. **성능 경고** (1초, 3초 임계값)
6. **예외 로깅**

## 사용 방법

### 기본 사용

```java

@LogTrace
public Article createArticle(ArticleCreateRequest request) {
	// 비즈니스 로직
}
```

### 파라미터 로깅

```java

@LogTrace(logParameters = true)
public Article updateArticle(String articleId, ArticleCreateRequest request) {
	// 파라미터가 로그에 출력됨
}
```

### 결과 로깅

```java

@LogTrace(logResult = true)
public List<Article> searchArticles(ArticleSearchCriteria criteria) {
	// 반환값이 로그에 출력됨
}
```

### 커스텀 이름

```java

@LogTrace(value = "게시글 생성")
public Article createArticle(ArticleCreateRequest request) {
	// 로그에 "게시글 생성" 표시
}
```

## 로그 출력 예시

### 정상 실행

```
[INFO] [TraceId:a1b2c3d4] START | ArticleCreateService.createArticle
[INFO] [TraceId:a1b2c3d4] END   | ArticleCreateService.createArticle | 245ms
```

### 성능 경고

```
[WARN] [TraceId:a1b2c3d4] END   | ArticleReadService.searchArticles | 1,234ms (성능 경고)
[ERROR] [TraceId:a1b2c3d4] END  | BulkService.bulkDelete | 3,456ms (심각한 성능 저하)
```

### 예외 발생

```
[ERROR] [TraceId:a1b2c3d4] EXCEPTION | ArticleCreateService.createArticle | 123ms
Exception: CustomException: ARTICLE_NOT_FOUND
```

## 적용 범위

현재 Service 레이어 전체에 적용:

- ArticleCreateService
- ArticleReadService

## 성능 임계값

- 1초 이상: WARN 레벨 경고
- 3초 이상: ERROR 레벨 경고

## Best Practices

1. Service 레이어에만 적용 (Controller는 제외)
2. 민감한 정보는 logParameters=false 유지
3. 대용량 결과는 logResult=false 유지
4. TraceId로 전체 요청 흐름 추적

## 작성일

2025-10-25

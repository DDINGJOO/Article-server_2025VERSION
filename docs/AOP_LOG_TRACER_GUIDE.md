# AOP 로그 트레이서 가이드

## 개요

AOP 기반 로그 트레이서는 메서드 실행을 자동으로 추적하고 성능을 측정하는 기능을 제공합니다.

## 주요 기능

### 1. 자동 로깅

- ✅ 메서드 진입/종료 로그
- ✅ 실행 시간 측정 (ms 단위)
- ✅ 파라미터 정보 로깅 (선택)
- ✅ 반환값 정보 로깅 (선택)
- ✅ 예외 발생 시 자동 로깅
- ✅ TraceId를 통한 요청 추적

### 2. 성능 모니터링

- ⚠️ 1초 이상 소요 시 경고
- ⚠️ 3초 이상 소요 시 SLOW 경고

## 사용 방법

### 1. 기본 사용

```java
@LogTrace
public void myMethod() {
    // 메서드 실행 내용
}
```

**로그 출력 예시:**

```
[TraceId: a1b2c3d4] ╭─> ArticleCreateService.createRegularArticle()
[TraceId: a1b2c3d4] │   Parameters: ArticleCreateRequest(title=테스트, ...)
[TraceId: a1b2c3d4] ╰─< ArticleCreateService.createRegularArticle() [SUCCESS]
[TraceId: a1b2c3d4]     ⏱ Time: 45ms
```

### 2. 커스텀 설명 추가

```java
@LogTrace("일반 게시글 생성")
public RegularArticle createRegularArticle(ArticleCreateRequest request) {
    // ...
}
```

**로그 출력 예시:**

```
[TraceId: e5f6g7h8] ╭─> ArticleCreateService.createRegularArticle() [일반 게시글 생성]
[TraceId: e5f6g7h8] │   Parameters: ArticleCreateRequest(title=테스트, ...)
[TraceId: e5f6g7h8] ╰─< ArticleCreateService.createRegularArticle() [SUCCESS]
[TraceId: e5f6g7h8]     ⏱ Time: 123ms ⚠
```

### 3. 파라미터 로깅 제어

```java
// 파라미터 로깅 비활성화 (대용량 데이터 처리 시)
@LogTrace(value = "게시글 검색", logParameters = false)
public ArticleCursorPageResponse searchArticles(
    ArticleSearchCriteria criteria,
    ArticleCursorPageRequest pageRequest) {
    // ...
}
```

**로그 출력 예시:**

```
[TraceId: i9j0k1l2] ╭─> ArticleReadService.searchArticles() [게시글 검색]
[TraceId: i9j0k1l2] ╰─< ArticleReadService.searchArticles() [SUCCESS]
[TraceId: i9j0k1l2]     ⏱ Time: 89ms
```

### 4. 반환값 로깅

```java
// 반환값 로깅 활성화
@LogTrace(value = "게시글 조회", logParameters = true, logResult = true)
public Article fetchArticleById(String articleId) {
    // ...
}
```

**로그 출력 예시:**

```
[TraceId: m3n4o5p6] ╭─> ArticleReadService.fetchArticleById() [게시글 조회]
[TraceId: m3n4o5p6] │   Parameters: article-123
[TraceId: m3n4o5p6] ╰─< ArticleReadService.fetchArticleById() [SUCCESS]
[TraceId: m3n4o5p6]     ✓ Result: RegularArticle(id=article-123, title=...)
[TraceId: m3n4o5p6]     ⏱ Time: 12ms
```

### 5. 예외 처리

예외가 발생하면 자동으로 로깅됩니다:

```
[TraceId: q7r8s9t0] ╭─> ArticleCreateService.createRegularArticle() [일반 게시글 생성]
[TraceId: q7r8s9t0] │   Parameters: ArticleCreateRequest(title=null, ...)
[TraceId: q7r8s9t0] ╰─< ArticleCreateService.createRegularArticle() [EXCEPTION]
[TraceId: q7r8s9t0]     ⚠ Exception: CustomException: REQUIRED_FIELD_NULL
[TraceId: q7r8s9t0]     ⏱ Time: 3ms
```

### 6. 클래스 전체에 적용

```java
@LogTrace  // 클래스 레벨에 적용
@Service
public class ArticleCreateService {

    // 모든 public 메서드에 자동 적용
    public void method1() { }
    public void method2() { }
}
```

## 어노테이션 옵션

| 옵션              | 타입      | 기본값   | 설명            |
|-----------------|---------|-------|---------------|
| `value`         | String  | ""    | 로그에 표시할 추가 설명 |
| `logParameters` | boolean | true  | 파라미터 정보 로깅 여부 |
| `logResult`     | boolean | false | 반환값 정보 로깅 여부  |

## 성능 임계값

| 실행 시간   | 표시                   | 로그 레벨 |
|---------|----------------------|-------|
| < 1초    | ⏱ Time: XXms         | INFO  |
| 1초 ~ 3초 | ⏱ Time: XXms ⚠       | WARN  |
| ≥ 3초    | ⏱ Time: XXms ⚠ SLOW! | WARN  |

## 사용 권장사항

### ✅ 권장하는 경우

1. **Service 레이어 메서드**
	- 비즈니스 로직 실행 추적
	- 성능 모니터링이 필요한 메서드

2. **Repository 복잡한 쿼리 메서드**
	- 쿼리 성능 측정
	- 데이터베이스 병목 지점 파악

3. **외부 API 호출 메서드**
	- 외부 의존성 성능 추적
	- 타임아웃 감지

### ❌ 권장하지 않는 경우

1. **Getter/Setter**
	- 불필요한 로그 생성
	- 성능 오버헤드

2. **자주 호출되는 유틸리티 메서드**
	- 로그 과다 생성
	- 가독성 저하

3. **이미 상세 로그가 있는 메서드**
	- 중복 로깅

## 실제 적용 예시

### ArticleCreateService.java

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArticleCreateService {

    @LogTrace(value = "일반 게시글 생성", logParameters = true)
    public RegularArticle createRegularArticle(ArticleCreateRequest request) {
        // 비즈니스 로직
        return savedArticle;
    }

    @LogTrace(value = "게시글 수정", logParameters = true)
    public Article updateArticle(String articleId, ArticleCreateRequest request) {
        // 비즈니스 로직
        return article;
    }

    @LogTrace(value = "게시글 삭제", logParameters = true)
    public void deleteArticle(String articleId) {
        // 비즈니스 로직
    }
}
```

### ArticleReadService.java

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArticleReadService {

    @LogTrace(value = "게시글 단건 조회", logParameters = true)
    public Article fetchArticleById(String articleId) {
        // 조회 로직
        return article;
    }

    @LogTrace(value = "게시글 검색", logParameters = false, logResult = false)
    public ArticleCursorPageResponse searchArticles(
        ArticleSearchCriteria criteria,
        ArticleCursorPageRequest pageRequest) {
        // 대용량 검색 로직 - 파라미터/결과 로깅 비활성화
        return response;
    }
}
```

## 디버깅 팁

### 1. TraceId로 요청 추적

같은 TraceId를 가진 로그들은 하나의 요청 흐름입니다:

```bash
# 특정 TraceId 로그만 필터링
grep "TraceId: a1b2c3d4" application.log
```

### 2. 성능 병목 지점 찾기

```bash
# SLOW 경고가 있는 로그 찾기
grep "SLOW!" application.log

# 1초 이상 걸린 메서드 찾기
grep "⚠" application.log | grep "Time:"
```

### 3. 예외 추적

```bash
# 예외가 발생한 메서드 찾기
grep "EXCEPTION" application.log
```

## 구현 세부사항

### LogTrace.java (어노테이션)

- `@Target`: METHOD, TYPE 레벨에 적용 가능
- `@Retention`: RUNTIME (실행 시점에 AOP로 처리)

### LogTraceAspect.java (AOP Aspect)

- `@Around`: 메서드 실행 전후 처리
- UUID 기반 TraceId 생성 (8자리)
- System.currentTimeMillis()로 실행 시간 측정
- 파라미터 길이 제한: 100자 (truncated 표시)

## 향후 개선 사항

1. **ThreadLocal 기반 TraceId 관리**
	- 현재: 메서드별 독립적 TraceId
	- 개선: 요청 전체에서 동일한 TraceId 공유

2. **로그 레벨 제어**
	- 개발: DEBUG, 운영: INFO
	- 환경별 로그 상세도 조정

3. **메트릭 수집 연동**
	- Micrometer 연동
	- 실행 시간 통계 수집

4. **비동기 메서드 지원**
	- @Async 메서드 추적
	- CompletableFuture 지원

## 참고 자료

- [Spring AOP 공식 문서](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [AspectJ 어노테이션](https://www.eclipse.org/aspectj/doc/released/aspectj5rt-api/index.html)

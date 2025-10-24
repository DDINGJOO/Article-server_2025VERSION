# 엔티티 리팩토링 완료 보고서

## ✅ 완료된 작업

### 1. 구버전 엔티티 파일 삭제

다음 파일들이 삭제되었습니다:

- `entity/Article.java`
- `entity/Board.java`
- `entity/ArticleImage.java`

### 2. 신규 엔티티 패키지 구조

```
entity/
├── article/
│   └── Article.java                    (완전히 새로 작성)
├── board/
│   └── Board.java                      (완전히 새로 작성)
├── keyword/
│   ├── Keyword.java                    (완전히 새로 작성)
│   └── KeywordMappingTable.java        (완전히 새로 작성)
├── image/
│   └── ArticleImage.java               (개선 완료)
├── articleType/
│   ├── RegularArticle.java             (임포트 경로 수정)
│   ├── EventArticle.java               (임포트 경로 수정)
│   └── NoticeArticle.java              (임포트 경로 수정)
└── enums/
    └── Status.java                     (기존 유지)
```

## 🔧 주요 수정 사항

### 1. Article 엔티티 (entity/article/Article.java)

#### ✅ 수정된 문제점

1. **필드명 변경**: `keywords` → `keywordMappings` (명확성 향상)
2. **index 계산 로직 수정**:
	- ❌ 이전: `null이면 0, 아니면 size+1` (비일관적)
	- ✅ 현재: `images.size() + 1` (항상 일관적)
3. **addKeyword 메서드 완전 재작성**:
	- 중복 체크 추가
	- 양방향 관계 안전한 설정
	- Keyword의 package-private 메서드 사용으로 순환 참조 방지
	- 사용 빈도 자동 증가
4. **removeKeyword/removeKeywords 메서드 완전 재작성**:
	- 양방향 관계 안전한 정리
	- 사용 빈도 자동 감소
5. **NPE 방어 코드 추가**:
	- 모든 메서드에 null 체크
	- 컬렉션 null 체크 후 작업
6. **새로운 메서드 추가**:
	- `getBoardKeywords()`: 게시판 전용 키워드만 필터링
	- `getCommonKeywords()`: 공통 키워드만 필터링
	- `replaceKeywords()`: 키워드 일괄 교체
	- `incrementViewCount()`: 조회 수 증가
	- `isActive()`, `isDeleted()`, `isBlocked()`: 상태 확인
	- `isWrittenBy()`: 작성자 확인

### 2. Board 엔티티 (entity/board/Board.java)

#### ✅ 추가된 기능

1. **keywords 컬렉션 추가**: 게시판 전용 키워드 관리
2. **편의 메서드 추가**:
	- `addKeyword()`: 양방향 관계 안전 설정
	- `removeKeyword()`: 양방향 관계 안전 정리
	- `clearKeywords()`: 모든 키워드 제거
	- `findKeywordByName()`: 이름으로 키워드 조회
	- `getActiveKeywords()`: 활성 키워드만 조회
3. **비즈니스 로직 추가**:
	- `updateInfo()`: 게시판 정보 업데이트
	- `activate()`, `deactivate()`: 활성화 상태 관리
	- `updateDisplayOrder()`: 표시 순서 변경
	- `getArticleCount()`, `getKeywordCount()`: 카운트 조회
4. **NPE 방어**:
	- 모든 연관관계 메서드에 null 체크
	- @PrePersist에서 컬렉션 초기화
5. **순환 참조 방지**:
	- Article/Keyword와 연동 시 현재 참조 상태 확인

### 3. Keyword 엔티티 (entity/keyword/Keyword.java)

#### ✅ 핵심 개선사항

1. **board 필드 추가**: null = 공통 키워드, 값 존재 = 보드 전용 키워드
2. **usageCount, isActive 필드 추가**: 사용 빈도 추적 및 활성화 관리
3. **순환 참조 방지 메커니즘**:
   ```java
   // package-private 메서드 - Article에서만 호출
   void addMappingInternal(KeywordMappingTable mapping)
   void removeMappingInternal(KeywordMappingTable mapping)
   ```
4. **보드 관계 편의 메서드**:
	- `assignToBoard()`: 양방향 관계 안전 설정
	- `detachFromBoard()`: 양방향 관계 안전 정리
5. **비즈니스 로직**:
	- `isCommonKeyword()`: 공통 키워드 여부
	- `belongsToBoard()`: 특정 보드 소속 여부
	- `incrementUsageCount()`, `decrementUsageCount()`: 사용 빈도 관리
	- `activate()`, `deactivate()`: 활성화 상태 관리
6. **NPE 방어**: 모든 메서드에 null 체크

### 4. KeywordMappingTable 엔티티 (entity/keyword/KeywordMappingTable.java)

#### ✅ 개선사항

1. **createdAt 필드 추가**: 매핑 생성 시간 추적
2. **생성자 검증 강화**:
   ```java
   if (keyword.getId() == null) {
       throw new IllegalArgumentException("Keyword must be persisted before creating mapping");
   }
   ```
3. **편의 메서드 추가**:
	- `getArticleId()`: 복합키에서 게시글 ID 추출
	- `getKeywordId()`: 복합키에서 키워드 ID 추출
	- `detach()`: 양방향 관계 정리

### 5. ArticleImage 엔티티 (entity/image/ArticleImage.java)

#### ✅ 개선사항

1. **생성자 검증 강화**:
	- Article ID null 체크
	- Sequence 범위 체크 (1 이상)
	- ImageUrl/ImageId blank 체크
2. **편의 메서드 검증 추가**:
	- `updateImageUrl()`: blank 체크
	- `updateImageId()`: blank 체크
	- `updateSequence()`: 범위 체크
3. **JavaDoc 추가**: 모든 메서드에 문서화

## 🛡️ 안전성 개선

### 1. NPE 방어

모든 엔티티의 모든 메서드에 다음과 같은 패턴 적용:

```java
if (collection == null || entity == null) {
    return;
}
```

### 2. 순환 참조 방지

**문제**: Article ↔ Keyword 양방향 관계에서 무한 루프 가능성

**해결책**:

- Keyword에 package-private 메서드 추가:
	- `addMappingInternal()`: Article에서만 호출
	- `removeMappingInternal()`: Article에서만 호출
- 공개 메서드는 호출하지 않음

### 3. 검증 강화

생성자와 업데이트 메서드에 `IllegalArgumentException` 던지기:

- null 체크
- 범위 체크
- 비어있는 문자열 체크
- ID 존재 여부 체크

### 4. 양방향 관계 일관성

모든 편의 메서드에서 양방향 설정 보장:

```java
// Board.addArticle() 예시
if (!articles.contains(article)) {
    articles.add(article);
}
if (article.getBoard() != this) {
    article.setBoard(this);  // 양방향 설정
}
```

## 📊 테스트 시나리오

### 1. 키워드 관리 시나리오

```java
// 1. 보드 전용 키워드 생성
Board techBoard = new Board("기술");
Keyword javaKeyword = new Keyword("Java", techBoard);
keywordRepository.save(javaKeyword);

// 2. 공통 키워드 생성
Keyword urgentKeyword = new Keyword("긴급", null);
keywordRepository.save(urgentKeyword);

// 3. 게시글에 추가 (persist된 Keyword 필요)
article.addKeyword(javaKeyword);
article.addKeyword(urgentKeyword);

// 4. 필터링
List<Keyword> boardKeywords = article.getBoardKeywords();   // [Java]
List<Keyword> commonKeywords = article.getCommonKeywords(); // [긴급]

// 5. 사용 빈도 확인
assertEquals(1, javaKeyword.getUsageCount());
```

### 2. 양방향 관계 시나리오

```java
// 1. Board에서 Article 추가
board.addArticle(article);
assertEquals(board, article.getBoard());  // 양방향 확인

// 2. Article에서 Image 추가
article.addImage("img-001", "https://example.com/1.jpg");
assertEquals("https://example.com/1.jpg", article.getFirstImageUrl());

// 3. Image 제거 시 대표 이미지 재설정
article.removeImage(article.getImages().get(0));
assertNotNull(article.getFirstImageUrl());  // 다음 이미지로 설정됨
```

## 🚨 주의사항

### 1. Keyword는 반드시 persist 후 사용

```java
// ❌ 잘못된 사용
Keyword keyword = new Keyword("태그", board);
article.addKeyword(keyword);  // IllegalArgumentException!

// ✅ 올바른 사용
Keyword keyword = new Keyword("태그", board);
keywordRepository.save(keyword);  // persist 먼저
article.addKeyword(keyword);      // 그 다음 추가
```

### 2. 트랜잭션 필수

모든 양방향 관계 설정은 트랜잭션 내에서 수행해야 합니다:

```java
@Transactional
public void createArticleWithKeywords(Article article, List<Keyword> keywords) {
    articleRepository.save(article);
    keywords.forEach(article::addKeyword);
}
```

### 3. Lazy Loading 주의

연관 엔티티 접근 시 프록시 초기화를 고려해야 합니다:

```java
// @Transactional 범위 밖에서
article.getBoard().getName();  // LazyInitializationException 가능
```

## 📝 다음 단계

### 1. 데이터베이스 마이그레이션

`docs/ENTITY_REFACTORING_SUMMARY.md`의 SQL 스크립트 실행:

- keywords 테이블에 board_id, usage_count, is_active 컬럼 추가
- keyword_mapping_table에 created_at 컬럼 추가
- articles 테이블에 view_count 컬럼 추가 (필요시)
- 인덱스 및 제약 조건 추가

### 2. Repository 계층 수정

- 임포트 경로 변경: `entity.*` → `entity.article.*`, `entity.board.*` 등
- 키워드 조회 쿼리 추가:
	- 보드별 키워드 조회
	- 공통 키워드 조회
	- 특정 보드에서 사용 가능한 모든 키워드 조회

### 3. Service 계층 수정

- 키워드 생성 시 보드 지정 여부 결정
- 편의 메서드 활용으로 코드 간소화
- 트랜잭션 범위 확인

### 4. Controller 계층 수정

- 임포트 경로 수정
- 보드별/공통 키워드 조회 엔드포인트 추가

### 5. 테스트 작성

- 단위 테스트: 각 엔티티의 비즈니스 로직
- 통합 테스트: 양방향 관계 설정 및 정리
- E2E 테스트: 키워드 생성부터 게시글 추가까지

## 🎯 개선 효과

### 1. 코드 품질

- **타입 안전성**: IllegalArgumentException으로 잘못된 사용 조기 감지
- **NPE 방지**: 모든 메서드에 null 체크
- **순환 참조 제거**: package-private 메서드 패턴
- **일관성**: 양방향 관계 자동 동기화

### 2. 유지보수성

- **패키지 구조**: 도메인별 명확한 분리
- **JavaDoc**: 모든 public 메서드 문서화
- **명확한 네이밍**: keywordMappings, addMappingInternal 등

### 3. 기능성

- **키워드 유연성**: 보드별/공통 키워드 동시 지원
- **사용 빈도 추적**: 인기 키워드 분석 가능
- **활성화 관리**: 키워드 on/off 제어

### 4. 성능

- **Lazy Loading**: 필요시에만 연관 엔티티 로딩
- **인덱스 활용**: 모든 외래키와 자주 조회되는 컬럼에 인덱스
- **캐싱 최적화 준비**: usageCount로 인기 키워드 선조회 가능

## ✅ 체크리스트

- [x] 구버전 엔티티 파일 삭제
- [x] Article 엔티티 완전 재작성
- [x] Board 엔티티 완전 재작성
- [x] Keyword 엔티티 완전 재작성
- [x] KeywordMappingTable 엔티티 개선
- [x] ArticleImage 엔티티 개선
- [x] Article 서브클래스 임포트 수정
- [x] NPE 방어 코드 추가
- [x] 순환 참조 제거
- [x] 양방향 관계 일관성 보장
- [x] JavaDoc 작성
- [x] 검증 로직 추가
- [ ] 데이터베이스 마이그레이션
- [ ] Repository 계층 수정
- [ ] Service 계층 수정
- [ ] Controller 계층 수정
- [ ] 테스트 작성

---

**작성일**: 2025-10-24
**작성자**: Claude Code
**버전**: 2.0.0

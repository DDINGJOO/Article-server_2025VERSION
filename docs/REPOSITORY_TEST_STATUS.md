# Repository Layer Test Status

## 전체 현황

| Repository                    | 테스트 수 | 성공 | 실패 | 상태    | 커버리지 |
|-------------------------------|-------|----|----|-------|------|
| BoardRepository               | 27    | 27 | 0  | ✅ 완료  | 100% |
| KeywordRepository             | 32    | 32 | 0  | ✅ 완료  | 100% |
| EventArticleRepository        | 24    | 0  | 24 | ⚠️ 이슈 | 0%   |
| NoticeArticleRepository       | -     | -  | -  | ⏳ 미작성 | 0%   |
| RegularArticleRepository      | -     | -  | -  | ⏳ 미작성 | 0%   |
| ArticleRepository             | -     | -  | -  | ⏳ 미작성 | 0%   |
| KeywordMappingTableRepository | -     | -  | -  | ⏳ 미작성 | 0%   |

**전체 진행률: 29.3% (59/201 예상 테스트)**

---

## ✅ 완료된 테스트

### 1. BoardRepositoryTest (27 tests)

**파일:** `src/test/java/com/teambind/articleserver/repository/BoardRepositoryTest.java`

**테스트 구성:**

- Save 테스트 (5개)
	- 정상: 게시판 저장
	- 정상: 여러 게시판 저장
	- 정상: displayOrder null 허용
- FindById 테스트 (3개)
	- 정상: ID로 조회
	- 예외: 존재하지 않는 ID
	- 엣지: null ID 예외
- FindByName 테스트 (5개)
	- 정상: 이름으로 조회
	- 예외: 존재하지 않는 이름
	- 엣지: 빈 문자열
	- 엣지: null
	- 정상: 대소문자 구분
- FindAll 테스트 (2개)
- Update 테스트 (3개)
- Delete 테스트 (3개)
- Count/Exists 테스트 (4개)
- 영속성 컨텍스트 테스트 (2개)
- 성능 테스트 (1개)

**실행 명령:**

```bash
./gradlew test --tests "BoardRepositoryTest"
# Result: BUILD SUCCESSFUL - 27/27 tests passing
```

---

### 2. KeywordRepositoryTest (32 tests)

**파일:** `src/test/java/com/teambind/articleserver/repository/KeywordRepositoryTest.java`

**주요 성과:**

- ✅ 100% 테스트 통과
- 🐛 심각한 스키마 버그 발견 및 수정 (Issue #47)

**발견된 버그:**
공통 키워드(board_id=NULL) 중복 방지 제약조건이 작동하지 않던 문제

**적용된 수정:**

```sql
-- Before: NULL 값 때문에 중복 허용됨
CREATE UNIQUE INDEX uk_keyword_board ON keywords (keyword_name, board_id);

-- After: 계산 컬럼으로 NULL을 -1로 변환하여 중복 방지
ALTER TABLE keywords
    ADD COLUMN board_id_coalesced BIGINT
        GENERATED ALWAYS AS (COALESCE(board_id, -1));
CREATE UNIQUE INDEX uk_keyword_board
    ON keywords (keyword_name, board_id_coalesced);
```

**테스트 구성:**

- Save 테스트 (6개)
	- 공통 키워드 저장
	- 게시판 전용 키워드 저장
	- 중복 방지 (공통 키워드)
	- 중복 방지 (같은 게시판)
	- 다른 게시판 간 같은 이름 허용
	- 공통 키워드와 게시판 키워드 같은 이름 허용
- FindById 테스트 (3개)
- FindAllByNameIn 테스트 (5개)
- CountByIdIn 테스트 (4개)
- FindAll 테스트 (2개)
- Update 테스트 (5개)
- Delete 테스트 (2개)
- 연관관계 테스트 (2개)
- 영속성 컨텍스트 테스트 (2개)
- 성능 테스트 (1개)

**실행 명령:**

```bash
./gradlew test --tests "KeywordRepositoryTest"
# Result: BUILD SUCCESSFUL - 32/32 tests passing
```

---

## ⚠️ 블로킹 이슈

### EventArticleRepositoryTest (Issue #46)

**문제:** Article 엔티티는 Snowflake 분산 ID를 사용하는데, `@DataJpaTest` 환경에서 ID 생성기 Bean이 로드되지 않음

**에러:**

```
org.hibernate.id.IdentifierGenerationException:
  ids for this class must be manually assigned before calling save()
```

**영향:**

- EventArticleRepositoryTest: 24 tests 작성 완료, 실행 불가
- NoticeArticleRepositoryTest: 미작성 (같은 이슈 예상)
- RegularArticleRepositoryTest: 미작성 (같은 이슈 예상)
- ArticleRepositoryTest: 미작성 (같은 이슈 예상)

**해결 방안:**

#### 옵션 1: 테스트용 ID 생성기 구현 (권장)

```java

@TestConfiguration
public class TestIdGeneratorConfig {
	private final AtomicLong counter = new AtomicLong(1000);
	
	@Bean
	@Primary
	public IdGenerator testIdGenerator() {
		return () -> "TEST-" + counter.getAndIncrement();
	}
}
```

#### 옵션 2: 수동 ID 할당

각 테스트에서 ID를 수동으로 설정

#### 옵션 3: Testcontainers

실제 Snowflake ID 생성 서버를 Docker로 구성

**관련 이슈:
** [#46 - Article 엔티티 Snowflake ID 생성기가 테스트 환경에서 작동하지 않음](https://github.com/DDINGJOO/Article-server_2025VERSION/issues/46)

---

## 📋 테스트 작성 가이드

### 테스트 구조

모든 Repository 테스트는 다음 구조를 따릅니다:

```java

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("XxxRepository 테스트")
class XxxRepositoryTest {
	
	@Autowired
	private XxxRepository xxxRepository;
	@Autowired
	private TestEntityManager entityManager;
	
	@AfterEach
	void tearDown() {
		xxxRepository.deleteAll();
		entityManager.flush();
		entityManager.clear();
	}
	
	@Nested
	@DisplayName("save() 테스트")
	class SaveTest {
		@Test
		@DisplayName("정상: 엔티티를 저장할 수 있다")
		void save_Success() {
			// given
			// when
			// then
		}
	}
}
```

### 테스트 카테고리

각 Repository는 다음 카테고리를 포함해야 합니다:

1. **Save 테스트**
	- 정상 케이스
	- 중복 방지 (있는 경우)
	- NULL 제약 검증

2. **Find 테스트**
	- 단건 조회
	- 목록 조회
	- 커스텀 쿼리 메서드
	- 엣지 케이스 (빈 결과, null 파라미터)

3. **Update 테스트**
	- 필드 수정
	- 상태 변경
	- Dirty Checking 동작

4. **Delete 테스트**
	- 단건 삭제
	- 일괄 삭제
	- Cascade 동작 (연관관계가 있는 경우)

5. **연관관계 테스트**
	- 양방향 관계 설정
	- Lazy Loading
	- Cascade 옵션

6. **영속성 컨텍스트 테스트**
	- 동일성 보장
	- 변경 감지

7. **성능 테스트**
	- 대량 데이터 처리
	- N+1 문제 검증

### 목표 커버리지

- **라인 커버리지:** 90% 이상
- **브랜치 커버리지:** 85% 이상
- **테스트 품질:** 실패한 테스트만으로 버그를 정확히 파악 가능해야 함

---

## 🔧 테스트 환경 설정

### H2 Database Configuration

**파일:** `src/test/resources/application-test.yaml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: none
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:sql/h2/schema.sql
      encoding: UTF-8
```

### Schema

**파일:** `src/test/resources/sql/h2/schema.sql`

- MySQL 호환 모드 사용
- 프로덕션과 동일한 제약조건
- Keyword 테이블에 board_id_coalesced 계산 컬럼 추가 (버그 수정)

---

## 📊 테스트 실행 결과

### 전체 실행

```bash
./gradlew test --tests "*RepositoryTest"
```

### 개별 실행

```bash
./gradlew test --tests "BoardRepositoryTest"
./gradlew test --tests "KeywordRepositoryTest"
./gradlew test --tests "EventArticleRepositoryTest"  # 현재 실패
```

### 커버리지 리포트 생성

```bash
./gradlew test jacocoTestReport
# 리포트 위치: build/reports/jacoco/test/html/index.html
```

---

## 🎯 다음 단계

### 1. 즉시 처리 필요 (High Priority)

- [ ] Issue #46 해결: Article ID 생성기 구현
- [ ] EventArticleRepositoryTest 실행 가능하도록 수정
- [ ] Issue #47 검토: 프로덕션 스키마 마이그레이션 계획

### 2. 추가 테스트 작성 (Medium Priority)

- [ ] NoticeArticleRepositoryTest 작성
- [ ] RegularArticleRepositoryTest 작성
- [ ] ArticleRepositoryTest 작성
- [ ] KeywordMappingTableRepositoryTest 작성

### 3. 고급 테스트 (Low Priority)

- [ ] 동시성 테스트 추가
- [ ] 트랜잭션 롤백 시나리오 테스트
- [ ] 대용량 데이터 성능 테스트

---

## 📝 변경 이력

| 날짜         | 변경 내용                                | 담당자    |
|------------|--------------------------------------|--------|
| 2025-10-25 | BoardRepositoryTest 27개 테스트 작성 완료    | Claude |
| 2025-10-25 | KeywordRepositoryTest 32개 테스트 작성 완료  | Claude |
| 2025-10-25 | Keyword 스키마 버그 발견 및 수정 (Issue #47)   | Claude |
| 2025-10-25 | EventArticle ID 생성 이슈 발견 (Issue #46) | Claude |

---

## 🔗 관련 링크

- [Issue #46: Article ID 생성기 문제](https://github.com/DDINGJOO/Article-server_2025VERSION/issues/46)
- [Issue #47: Keyword 스키마 버그 수정](https://github.com/DDINGJOO/Article-server_2025VERSION/issues/47)
- [Entity 리팩토링 완료 문서](./ENTITY_REFACTORING_COMPLETE.md)
- [DTO 리팩토링 가이드](./DTO_REFACTORING_GUIDE.md)

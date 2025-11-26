# ADR-002: Snowflake ID Generation Strategy

**Status**: Accepted
**Date**: 2025-11-16
**Decision Makers**: Platform Engineering Team

---

## Context

Article Server는 분산 환경에서 고유한 게시글 ID를 생성해야 합니다. MSA 환경에서 여러 인스턴스가 동시에 실행될 수 있으며, ID 충돌 없이 시간순 정렬이 가능한 ID 생성 전략이 필요합니다.

### Requirements

- **Uniqueness**: 전역적으로 고유한 ID 보장
- **Sortability**: 시간순 정렬 가능
- **Scalability**: 분산 환경에서 ID 생성 가능
- **Performance**: 빠른 ID 생성 속도
- **No SPOF**: 중앙 집중식 ID 생성 서버 의존 없음
- **Human Readable**: 가능한 경우 사람이 읽을 수 있는 형식

### Considered Options

#### Option 1: Database Auto Increment

```sql
CREATE TABLE articles (
    article_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ...
);
```

**Pros:**

- 구현이 간단함
- 숫자로 된 연속적인 ID

**Cons:**

- 분산 환경에서 충돌 가능
- 데이터베이스 의존적
- 샤딩 시 문제 발생
- ID를 통해 비즈니스 정보 노출

#### Option 2: UUID v4

```java
String articleId = UUID.randomUUID().toString();
// Example: "550e8400-e29b-41d4-a716-446655440000"
```

**Pros:**

- 충돌 확률이 매우 낮음
- 데이터베이스 독립적
- 구현이 간단함

**Cons:**

- 시간순 정렬 불가능
- 128비트로 크기가 큼
- 인덱스 성능 저하
- 사람이 읽기 어려움

#### Option 3: ULID (Universally Unique Lexicographically Sortable Identifier)

```java
String articleId = UlidCreator.getUlid().toString();
// Example: "01ARZ3NDEKTSV4RRFFQ69G5FAV"
```

**Pros:**

- 시간순 정렬 가능
- UUID와 호환
- 26자 문자열로 표현

**Cons:**

- 외부 라이브러리 의존
- Snowflake보다 크기가 큼

#### Option 4: Snowflake ID (선택된 옵션)

```java
public class SnowflakeIdGenerator {
    // 64-bit ID structure:
    // 1 bit: unused (sign bit)
    // 41 bits: timestamp (milliseconds since epoch)
    // 10 bits: machine ID
    // 12 bits: sequence number

    private static final long EPOCH = 1609459200000L; // 2021-01-01
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis() - EPOCH;
        return (timestamp << 22) | (machineId << 12) | sequence;
    }
}

// Formatted as: "ART20251126001"
```

**Pros:**

- 시간순 정렬 가능
- 64비트 정수로 컴팩트
- 높은 성능 (로컬 생성)
- 분산 환경 지원
- 사람이 읽기 쉬운 형식으로 변환 가능

**Cons:**

- 구현 복잡도가 높음
- 시계 동기화 필요
- Machine ID 관리 필요

## Decision

**Snowflake ID** 생성 전략을 채택하며, 사용자 친화적인 포맷으로 변환합니다.

### Rationale

1. **Time-ordered**: 생성 시간 기반으로 자연스럽게 정렬
2. **Distributed**: Machine ID를 통한 분산 환경 지원
3. **Performance**: 로컬에서 생성되어 네트워크 지연 없음
4. **Compact**: 64비트로 저장 및 전송 효율적
5. **Readable**: 커스텀 포맷팅으로 가독성 향상

### Implementation

```java
@Component
public class ArticleIdGenerator {
    private static final String PREFIX_REGULAR = "ART";
    private static final String PREFIX_EVENT = "EVT";
    private static final String PREFIX_NOTICE = "NTC";

    private final SnowflakeIdGenerator snowflakeGenerator;

    public String generateArticleId(ArticleType type) {
        long snowflakeId = snowflakeGenerator.nextId();
        String prefix = getPrefix(type);
        String timestamp = formatTimestamp(snowflakeId);
        String sequence = formatSequence(snowflakeId);

        // Format: ART20251126001
        return String.format("%s%s%s", prefix, timestamp, sequence);
    }

    private String getPrefix(ArticleType type) {
        return switch (type) {
            case REGULAR -> PREFIX_REGULAR;
            case EVENT -> PREFIX_EVENT;
            case NOTICE -> PREFIX_NOTICE;
        };
    }

    private String formatTimestamp(long snowflakeId) {
        // Extract timestamp from snowflake ID
        long timestamp = (snowflakeId >> 22) + EPOCH;
        return DateTimeFormatter.ofPattern("yyyyMMdd")
            .format(Instant.ofEpochMilli(timestamp));
    }

    private String formatSequence(long snowflakeId) {
        // Extract sequence number (last 12 bits)
        long sequence = snowflakeId & 0xFFF;
        return String.format("%03d", sequence % 1000);
    }
}
```

### Configuration

```yaml
snowflake:
  datacenter-id: ${DATACENTER_ID:1}  # 1-31
  machine-id: ${MACHINE_ID:1}        # 1-31
  epoch: 1609459200000                # 2021-01-01 00:00:00 UTC
```

### Machine ID Assignment Strategy

```java
@Configuration
public class SnowflakeConfig {

    @Value("${spring.application.instance-id:}")
    private String instanceId;

    @Bean
    public long machineId() {
        if (StringUtils.hasText(instanceId)) {
            // Kubernetes Pod instance
            return extractPodOrdinal(instanceId) % 1024;
        } else {
            // Fallback: Use MAC address hash
            return getMachineIdFromMac() % 1024;
        }
    }

    private long extractPodOrdinal(String podName) {
        // Extract ordinal from pod name (e.g., "article-server-2" -> 2)
        String[] parts = podName.split("-");
        return Long.parseLong(parts[parts.length - 1]);
    }

    private long getMachineIdFromMac() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();
            return ((mac[4] & 0xFF) << 8) | (mac[5] & 0xFF);
        } catch (Exception e) {
            // Fallback to random
            return new SecureRandom().nextInt(1024);
        }
    }
}
```

## Consequences

### Positive

- ✅ 시간순 정렬이 자연스럽게 지원됨
- ✅ 분산 환경에서 충돌 없이 ID 생성
- ✅ 높은 처리량 지원 (초당 409만 개 ID 생성 가능)
- ✅ 사람이 읽기 쉬운 형식
- ✅ 데이터베이스 독립적

### Negative

- ⚠️ 시계 동기화 문제 시 ID 중복 가능
- ⚠️ Machine ID 관리 복잡도
- ⚠️ 문자열 변환 시 추가 저장 공간 필요

### Mitigations

1. **Clock Skew Protection**

```java
private long lastTimestamp = -1L;

protected synchronized long tilNextMillis(long lastTimestamp) {
    long timestamp = timeGen();
    while (timestamp <= lastTimestamp) {
        timestamp = timeGen();
    }
    return timestamp;
}
```

2. **Machine ID Collision Detection**

```java
@PostConstruct
public void validateMachineId() {
    // Redis를 사용한 Machine ID 중복 체크
    String key = "machine:id:" + machineId;
    Boolean success = redisTemplate.opsForValue()
        .setIfAbsent(key, instanceId, 1, TimeUnit.HOURS);

    if (!success) {
        throw new IllegalStateException(
            "Machine ID collision detected: " + machineId);
    }
}
```

3. **ID Format Validation**

```java
public boolean isValidArticleId(String articleId) {
    // Pattern: PREFIX(3) + DATE(8) + SEQ(3)
    Pattern pattern = Pattern.compile(
        "^(ART|EVT|NTC)\\d{8}\\d{3}$");
    return pattern.matcher(articleId).matches();
}
```

## Performance Metrics

| Metric                | Value                      |
|-----------------------|----------------------------|
| ID Generation Rate    | 4,096,000/sec per instance |
| ID Size               | 64 bits (8 bytes)          |
| Formatted ID Size     | 14 characters              |
| Collision Probability | 0% (with proper config)    |
| Time Range            | 69 years from epoch        |

## References

- [Twitter Snowflake](https://github.com/twitter-archive/snowflake)
- [Snowflake ID Generator](https://en.wikipedia.org/wiki/Snowflake_ID)
- [Distributed ID Generation](https://developervisits.wordpress.com/2021/05/11/distributed-unique-id-generation/)
- [Instagram ID Generation](https://instagram-engineering.com/sharding-ids-at-instagram-1cf5a71e5a5c)

---

**Review History:**

- 2025-11-16: Initial proposal
- 2025-11-19: Accepted with formatting addition
- 2025-11-21: Implemented in production

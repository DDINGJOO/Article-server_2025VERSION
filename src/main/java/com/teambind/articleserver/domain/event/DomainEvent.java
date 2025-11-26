package com.teambind.articleserver.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 도메인 이벤트 기본 인터페이스
 *
 * <p>모든 도메인 이벤트가 구현해야 하는 기본 계약을 정의합니다.
 */
public interface DomainEvent {

  /** 이벤트 ID 생성 헬퍼 메서드 */
  static String generateEventId() {
    return UUID.randomUUID().toString();
  }

  /** 이벤트 ID */
  String getEventId();

  /** 이벤트 발생 시각 */
  LocalDateTime getOccurredAt();

  /** 이벤트 타입 */
  String getEventType();

  /** Aggregate ID */
  String getAggregateId();

  /** 이벤트 버전 (이벤트 스키마 버전 관리용) */
  default int getEventVersion() {
    return 1;
  }
}

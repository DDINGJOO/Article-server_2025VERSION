package com.teambind.articleserver.domain.model;

import com.teambind.articleserver.domain.event.DomainEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root 추상 클래스
 *
 * <p>DDD의 Aggregate Root 패턴을 구현합니다. - 도메인 이벤트 관리 - 트랜잭션 경계 정의 - 일관성 경계 보장
 */
public abstract class AggregateRoot {

  private final transient List<DomainEvent> domainEvents = new ArrayList<>();

  /**
   * 도메인 이벤트 등록
   *
   * @param event 발생한 도메인 이벤트
   */
  protected void registerEvent(DomainEvent event) {
    if (event != null) {
      domainEvents.add(event);
    }
  }

  /**
   * 여러 도메인 이벤트 일괄 등록
   *
   * @param events 발생한 도메인 이벤트들
   */
  protected void registerEvents(List<DomainEvent> events) {
    if (events != null && !events.isEmpty()) {
      domainEvents.addAll(events);
    }
  }

  /**
   * 등록된 도메인 이벤트 조회 (읽기 전용)
   *
   * @return 불변 이벤트 리스트
   */
  public List<DomainEvent> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  /**
   * 도메인 이벤트 클리어
   *
   * <p>이벤트 발행 후 호출되어야 합니다.
   */
  public void clearDomainEvents() {
    domainEvents.clear();
  }

  /**
   * 특정 타입의 이벤트만 조회
   *
   * @param eventClass 이벤트 클래스 타입
   * @return 해당 타입의 이벤트 리스트
   */
  @SuppressWarnings("unchecked")
  public <T extends DomainEvent> List<T> getEventsOfType(Class<T> eventClass) {
    return domainEvents.stream().filter(eventClass::isInstance).map(event -> (T) event).toList();
  }

  /**
   * 이벤트 존재 여부 확인
   *
   * @return 이벤트 존재 여부
   */
  public boolean hasEvents() {
    return !domainEvents.isEmpty();
  }

  /**
   * Aggregate 유효성 검증
   *
   * <p>각 Aggregate는 자신의 불변 조건을 검증해야 합니다.
   *
   * @return 유효성 여부
   */
  public abstract boolean isValid();
}

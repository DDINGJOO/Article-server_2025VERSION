package com.teambind.articleserver.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 로그 추적 및 성능 측정을 위한 어노테이션
 *
 * <p>이 어노테이션이 붙은 메서드는 실행 시 다음 정보가 로그로 출력됩니다: - 메서드 진입/종료 정보 - 실행 시간 (ms) - 파라미터 정보 - 반환값 정보 - 예외 정보
 * (발생 시)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogTrace {
  /** 로그에 표시할 추가 설명 */
  String value() default "";

  /** 파라미터 정보 로깅 여부 (기본: true) */
  boolean logParameters() default true;

  /** 반환값 정보 로깅 여부 (기본: false) 대용량 데이터 반환 시 false로 설정 권장 */
  boolean logResult() default false;
}

package com.teambind.articleserver.aop;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * LogTrace 어노테이션이 붙은 메서드의 실행을 추적하는 AOP Aspect
 *
 * <p>주요 기능: 1. 메서드 실행 시간 측정 2. 메서드 진입/종료 로그 출력 3. 파라미터 및 반환값 로깅 4. 예외 발생 시 로깅 5. TraceId를 통한 요청 추적
 */
@Aspect
@Component
@Slf4j
public class LogTraceAspect {

  private static final String TRACE_ID_PREFIX = "[TraceId: ";
  private static final String TRACE_ID_SUFFIX = "]";
  private static final int MAX_PARAM_LENGTH = 100;

  @Around(
      "@annotation(com.teambind.articleserver.aop.LogTrace) || @within(com.teambind.articleserver.aop.LogTrace)")
  public Object traceLog(ProceedingJoinPoint joinPoint) throws Throwable {
    // TraceId 생성 (요청별 고유 ID)
    String traceId = UUID.randomUUID().toString().substring(0, 8);

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();

    // LogTrace 어노테이션 가져오기
    LogTrace logTrace = method.getAnnotation(LogTrace.class);
    if (logTrace == null) {
      logTrace = joinPoint.getTarget().getClass().getAnnotation(LogTrace.class);
    }

    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = method.getName();
    String description = (logTrace != null && !logTrace.value().isEmpty()) ? logTrace.value() : "";

    // 시작 로그
    long startTime = System.currentTimeMillis();
    String startPrefix = createPrefix(traceId, 0);

    StringBuilder startLog = new StringBuilder();
    startLog
        .append(startPrefix)
        .append("╭─> ")
        .append(className)
        .append(".")
        .append(methodName)
        .append("()");

    if (!description.isEmpty()) {
      startLog.append(" [").append(description).append("]");
    }

    // 파라미터 로깅
    if (logTrace != null && logTrace.logParameters()) {
      Object[] args = joinPoint.getArgs();
      if (args != null && args.length > 0) {
        String params =
            Arrays.stream(args).map(this::formatParameter).collect(Collectors.joining(", "));
        startLog.append("\n").append(startPrefix).append("│   Parameters: ").append(params);
      }
    }

    log.info(startLog.toString());

    // 메서드 실행
    Object result = null;
    Throwable exception = null;
    try {
      result = joinPoint.proceed();
      return result;
    } catch (Throwable e) {
      exception = e;
      throw e;
    } finally {
      // 종료 로그
      long endTime = System.currentTimeMillis();
      long executionTime = endTime - startTime;

      String endPrefix = createPrefix(traceId, 0);
      StringBuilder endLog = new StringBuilder();

      if (exception != null) {
        // 예외 발생 시
        endLog
            .append(endPrefix)
            .append("╰─< ")
            .append(className)
            .append(".")
            .append(methodName)
            .append("() [EXCEPTION]")
            .append("\n")
            .append(endPrefix)
            .append("    ⚠ Exception: ")
            .append(exception.getClass().getSimpleName())
            .append(": ")
            .append(exception.getMessage())
            .append("\n")
            .append(endPrefix)
            .append("    ⏱ Time: ")
            .append(executionTime)
            .append("ms");
        log.error(endLog.toString());
      } else {
        // 정상 종료 시
        endLog
            .append(endPrefix)
            .append("╰─< ")
            .append(className)
            .append(".")
            .append(methodName)
            .append("() [SUCCESS]");

        // 반환값 로깅
        if (logTrace != null && logTrace.logResult() && result != null) {
          endLog
              .append("\n")
              .append(endPrefix)
              .append("    ✓ Result: ")
              .append(formatParameter(result));
        }

        endLog
            .append("\n")
            .append(endPrefix)
            .append("    ⏱ Time: ")
            .append(executionTime)
            .append("ms");

        // 성능 경고 (3초 이상 소요 시)
        if (executionTime > 3000) {
          endLog.append(" ⚠ SLOW!");
          log.warn(endLog.toString());
        } else if (executionTime > 1000) {
          endLog.append(" ⚠");
          log.warn(endLog.toString());
        } else {
          log.info(endLog.toString());
        }
      }
    }
  }

  private String createPrefix(String traceId, int level) {
    return TRACE_ID_PREFIX + traceId + TRACE_ID_SUFFIX + " ";
  }

  private String formatParameter(Object param) {
    if (param == null) {
      return "null";
    }

    String str = param.toString();

    // 너무 긴 문자열은 잘라냄
    if (str.length() > MAX_PARAM_LENGTH) {
      return str.substring(0, MAX_PARAM_LENGTH) + "... (truncated)";
    }

    return str;
  }
}

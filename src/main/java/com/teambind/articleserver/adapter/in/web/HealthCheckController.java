package com.teambind.articleserver.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 *
 * Docker health check 및 로드 밸런서의 헬스 체크를 위한 엔드포인트 제공
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    /**
     * 기본 헬스 체크 엔드포인트
     *
     * @return 서버 상태 정보
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "article-server");

        return ResponseEntity.ok(response);
    }

    /**
     * 상세 헬스 체크 엔드포인트
     *
     * @return 상세 서버 상태 정보
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> healthDetail() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "article-server");

        // JVM 메모리 정보
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("max", runtime.maxMemory() / 1024 / 1024 + " MB");
        memory.put("total", runtime.totalMemory() / 1024 / 1024 + " MB");
        memory.put("free", runtime.freeMemory() / 1024 / 1024 + " MB");
        memory.put("used", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 + " MB");
        response.put("memory", memory);

        // 시스템 정보
        Map<String, Object> system = new HashMap<>();
        system.put("processors", runtime.availableProcessors());
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        response.put("system", system);

        return ResponseEntity.ok(response);
    }

    /**
     * Liveness 체크 엔드포인트 (Kubernetes용)
     * 애플리케이션이 살아있는지 확인
     *
     * @return OK 상태
     */
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ALIVE");
        return ResponseEntity.ok(response);
    }

    /**
     * Readiness 체크 엔드포인트 (Kubernetes용)
     * 애플리케이션이 트래픽을 받을 준비가 되었는지 확인
     *
     * @return READY 상태
     */
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, String>> readiness() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "READY");
        return ResponseEntity.ok(response);
    }
}
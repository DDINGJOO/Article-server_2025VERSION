package com.teambind.articleserver.performance.base;

import lombok.Builder;
import lombok.Data;

/** 테스트 데이터 생성 설정 */
@Data
@Builder
public class DataGenerationConfig {

  /** 전체 생성할 레코드 수 */
  @Builder.Default private int totalRecords = 100_000;

  /** 배치 처리 크기 */
  @Builder.Default private int batchSize = 10_000;

  /** 병렬 처리 스레드 수 */
  @Builder.Default private int parallelism = Runtime.getRuntime().availableProcessors();

  /** 플러시 간격 (N개 레코드마다 강제 flush) */
  @Builder.Default private int flushInterval = 1000;

  /** 데이터 분포 설정 */
  @Builder.Default private DataDistribution distribution = DataDistribution.REALISTIC;

  /** 워밍업 레코드 수 */
  @Builder.Default private int warmupRecords = 100;

  /** 측정 반복 횟수 */
  @Builder.Default private int measurementIterations = 1000;

  public enum DataDistribution {
    /** 균등 분포 */
    UNIFORM,

    /**
     * 실제 프로덕션과 유사한 분포 - RegularArticle: 90% - EventArticle: 5% - NoticeArticle: 5% - ACTIVE: 98%,
     * BLOCKED: 1%, DELETED: 1%
     */
    REALISTIC,

    /** 스트레스 테스트용 (최악의 경우) */
    STRESS
  }
}

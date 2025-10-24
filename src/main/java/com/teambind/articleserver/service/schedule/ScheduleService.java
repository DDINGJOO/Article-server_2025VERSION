package com.teambind.articleserver.service.schedule;

import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 스케줄 작업 서비스
 *
 * <p>정기적으로 실행되는 배치 작업 관리 ShedLock을 통해 분산 환경에서도 하나의 인스턴스만 실행되도록 보장
 */
@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {
  private final ArticleRepository articleRepository;

  /**
   * 삭제된 게시글 정리 작업
   *
   * 매일 새벽 4시 15분에 실행 DELETED 상태의 게시글을 DB에서 완전히 삭제
   *
   * ShedLock 설정: - lockAtMostFor: 최대 9분간 락 유지 (작업이 비정상 종료되어도 9분 후 다른 인스턴스가 실행 가능) -
   * lockAtLeastFor: 최소 1분간 락 유지 (작업이 빨리 끝나도 1분 동안은 다른 인스턴스가 실행 불가)
   */
  @Scheduled(cron = "0 15 4 * * *")
  @SchedulerLock(
      name = "cleanUpDeletedArticles",
      lockAtMostFor = "9m",
      lockAtLeastFor = "1m")
  public void cleanUp() {
    log.info("Starting cleanup of deleted articles");
    try {
      articleRepository.deleteByStatus(Status.DELETED);
      log.info("Successfully completed cleanup of deleted articles");
    } catch (Exception e) {
      log.error("Failed to cleanup deleted articles", e);
      throw e;
    }
  }
}

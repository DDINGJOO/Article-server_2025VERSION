package com.teambind.articleserver.adapter.out.persistence.repository;

import com.teambind.articleserver.adapter.out.persistence.entity.articleType.EventArticle;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventArticleRepository extends JpaRepository<EventArticle, String> {

  // 진행중인 이벤트 조회 (현재 시간이 시작일과 종료일 사이)
  @Query(
      "SELECT e FROM EventArticle e WHERE e.status = :status AND :now BETWEEN e.eventStartDate AND e.eventEndDate ORDER BY e.createdAt DESC")
  Page<EventArticle> findOngoingEvents(
      @Param("status") Status status, @Param("now") LocalDateTime now, Pageable pageable);

  // 종료된 이벤트 조회 (현재 시간이 종료일 이후)
  @Query(
      "SELECT e FROM EventArticle e WHERE e.status = :status AND e.eventEndDate < :now ORDER BY e.eventEndDate DESC")
  Page<EventArticle> findEndedEvents(
      @Param("status") Status status, @Param("now") LocalDateTime now, Pageable pageable);

  // 진행 예정 이벤트 조회 (현재 시간이 시작일 이전)
  @Query(
      "SELECT e FROM EventArticle e WHERE e.status = :status AND e.eventStartDate > :now ORDER BY e.eventStartDate ASC")
  Page<EventArticle> findUpcomingEvents(
      @Param("status") Status status, @Param("now") LocalDateTime now, Pageable pageable);

  // 모든 이벤트 조회
  Page<EventArticle> findByStatusOrderByCreatedAtDesc(Status status, Pageable pageable);
}

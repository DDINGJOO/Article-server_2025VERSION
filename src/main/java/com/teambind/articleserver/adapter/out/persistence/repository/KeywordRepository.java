package com.teambind.articleserver.adapter.out.persistence.repository;

import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

  // 메서드명이 실제 필드명과 다름 (keyword -> name) - 수정 필요
  @Query("SELECT k FROM Keyword k WHERE k.name IN :keywordNames")
  List<Keyword> findAllByKeywordIn(@Param("keywordNames") List<String> keywordNames);

  // 올바른 메서드명
  List<Keyword> findAllByNameIn(List<String> names);

  // ID 리스트로 존재 개수 확인 (Validation용)
  long countByIdIn(List<Long> ids);

  // board를 fetch join하여 모든 키워드 조회 (LazyInitializationException 방지)
  @Query("SELECT k FROM Keyword k LEFT JOIN FETCH k.board")
  List<Keyword> findAllWithBoard();
}

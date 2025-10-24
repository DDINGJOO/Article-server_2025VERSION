package com.teambind.articleserver.repository;

import com.teambind.articleserver.entity.keyword.Keyword;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
  List<Keyword> findAllByKeywordIn(List<String> keywordList);
}

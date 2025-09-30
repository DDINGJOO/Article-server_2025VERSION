package com.teambind.articleserver.repository;

import com.teambind.articleserver.entity.KeywordMappingTable;
import com.teambind.articleserver.entity.embeddable_id.KeywordMappingTableId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeywordMappingTableRepository extends JpaRepository<KeywordMappingTable, KeywordMappingTableId> {
}

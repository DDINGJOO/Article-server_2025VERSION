package com.teambind.articleserver.adapter.out.persistence.repository;

import com.teambind.articleserver.adapter.out.persistence.entity.embeddable_id.KeywordMappingTableId;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.KeywordMappingTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeywordMappingTableRepository
    extends JpaRepository<KeywordMappingTable, KeywordMappingTableId> {}

package com.teambind.articleserver.entity;

import com.teambind.articleserver.entity.embeddable_id.KeywordMappingTableId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "keyword_mapping_table")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class KeywordMappingTable {
	@EmbeddedId
	private KeywordMappingTableId id;
}

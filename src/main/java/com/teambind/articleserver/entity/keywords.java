package com.teambind.articleserver.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "keywords")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class keywords {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "keyword_id", nullable = false)
	private Long id;
	
	@Column(name = "keyword_name", nullable = false)
	private String keyword;
	
	@OneToOne(mappedBy = "keyword")
	private KeywordMappingTable keywordMappingTable;
	
}

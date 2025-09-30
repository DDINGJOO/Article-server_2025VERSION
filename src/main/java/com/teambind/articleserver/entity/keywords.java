package com.teambind.articleserver.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
	
	@OneToMany(mappedBy = "keyword", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	private List<KeywordMappingTable> mappings = new ArrayList<>();
	
	
	public void addMapping(KeywordMappingTable km) {
		mappings.add(km);
		km.setKeyword(this);
	}
	
	public void removeMapping(KeywordMappingTable km) {
		mappings.remove(km);
		km.setKeyword(null);
	}
	
}

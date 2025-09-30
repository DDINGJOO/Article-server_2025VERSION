package com.teambind.articleserver.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "boards")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Board {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	private Long id;
	
	@Column(name = "board_name", nullable = false)
	private String boardName;
	
}

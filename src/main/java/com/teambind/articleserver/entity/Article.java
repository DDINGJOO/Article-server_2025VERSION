package com.teambind.articleserver.entity;


import com.teambind.articleserver.entity.enums.Status;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "articles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Article {
	@Id
	@Column(name = "article_id", nullable = false)
	private String id;
	
	@Column(name = "title", nullable = false)
	private String title;
	
	@Column(name = "contents", nullable = false)
	private String contents;
	
	@Column(name = "writer_id", nullable = false)
	private String writerId;
	@Version
	private Long version;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "board_id", nullable = false)
	private Board board;
	
	@Column(name = "status", nullable = false)
	private Status status;
	
	@Column(name = "created_at", nullable = false)
	private Long createdAt;
	@Column(name = "updated_at", nullable = false)
	private Long updatedAt;
	
	
}

package com.teambind.articleserver.entity;

import jakarta.persistence.*;
import java.util.List;
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
  @Column(name = "board_id", nullable = false)
  private Long id;

  @Column(name = "board_name", nullable = false)
  private String boardName;

  @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Article> articles;

  public Board(Long boardId, String name) {
    this.id = boardId;
    this.boardName = name;
  }
}

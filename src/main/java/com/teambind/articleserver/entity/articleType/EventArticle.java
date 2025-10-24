package com.teambind.articleserver.entity.articleType;

import com.teambind.articleserver.entity.article.Article;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("EVENT")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class EventArticle extends Article {

  @Column(name = "event_start_date", nullable = false)
  private LocalDateTime eventStartDate;

  @Column(name = "event_end_date", nullable = false)
  private LocalDateTime eventEndDate;
}

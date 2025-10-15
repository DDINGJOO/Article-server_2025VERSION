package com.teambind.articleserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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

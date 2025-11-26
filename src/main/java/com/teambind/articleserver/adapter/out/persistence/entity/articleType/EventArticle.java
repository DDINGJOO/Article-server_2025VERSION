package com.teambind.articleserver.adapter.out.persistence.entity.articleType;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
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

  /**
   * 이벤트 기간 업데이트
   *
   * @param eventStartDate 이벤트 시작일
   * @param eventEndDate 이벤트 종료일
   */
  public void updateEventPeriod(LocalDateTime eventStartDate, LocalDateTime eventEndDate) {
    this.eventStartDate = eventStartDate;
    this.eventEndDate = eventEndDate;
  }
}

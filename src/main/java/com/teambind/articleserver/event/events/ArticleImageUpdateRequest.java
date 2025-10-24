package com.teambind.articleserver.event.events;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleImageUpdateRequest {
  List<ImageChangeEvent> imageChangeEvents;
}

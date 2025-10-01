package com.teambind.articleserver.event.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleImageUpdateRequest {
  private String imageId;
  private String imageUrl;
  private String referenceId;
}

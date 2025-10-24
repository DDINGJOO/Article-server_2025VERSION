package com.teambind.articleserver.event.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageChangeEvent {
  private String referenceId;
  private String imageId;
  private String imageUrl;
}

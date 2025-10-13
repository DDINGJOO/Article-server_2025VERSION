package com.teambind.articleserver.repository.projection;

import java.time.LocalDateTime;

public interface ArticleSimpleView {
  String getId();

  String getTitle();

  String getWriterId();

  Long getVersion();

  LocalDateTime getCreatedAt();
}

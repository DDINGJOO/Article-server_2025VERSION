package com.teambind.articleserver.adapter.out.persistence.projection;

import java.time.LocalDateTime;

public interface ArticleSimpleView {
  String getId();

  String getTitle();

  String getContent();

  String getWriterId();

  Long getVersion();

  LocalDateTime getCreatedAt();

  LocalDateTime getUpdatedAt();

  Long getBoardId();

  String getBoardName();

  String getArticleType();

  String getStatus();

  Long getViewCount();

  String getFirstImageUrl();
}

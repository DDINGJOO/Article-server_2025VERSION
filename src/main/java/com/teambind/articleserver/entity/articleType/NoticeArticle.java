package com.teambind.articleserver.entity.articleType;

import com.teambind.articleserver.entity.article.Article;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("NOTICE")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class NoticeArticle extends Article {
  // 공지사항도 현재는 추가 필드가 없음
  // 필요시 isPinned(상단고정) 등의 필드 추가 가능
}

package com.teambind.articleserver.entity.articleType;

import com.teambind.articleserver.entity.article.Article;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("REGULAR")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class RegularArticle extends Article {
  // 일반 게시글은 추가 필드가 없음
}

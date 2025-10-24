package com.teambind.articleserver.utils.generator.primay_key;

import org.springframework.stereotype.Component;

@Component
public interface PrimaryKetGenerator {

  String generateKey();
}

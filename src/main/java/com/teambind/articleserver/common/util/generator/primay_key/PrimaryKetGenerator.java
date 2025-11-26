package com.teambind.articleserver.common.util.generator.primay_key;

import org.springframework.stereotype.Component;

@Component
public interface PrimaryKetGenerator {

  String generateKey();
}

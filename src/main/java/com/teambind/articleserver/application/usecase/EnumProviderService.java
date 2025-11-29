package com.teambind.articleserver.application.usecase;

import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.application.port.in.enums.EnumProviderUseCase;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 열거형 정보 제공 서비스
 *
 * <p>Hexagonal Architecture의 Application Service입니다. 시스템에서 사용하는 열거형 값들을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnumProviderService implements EnumProviderUseCase {

  @Override
  public Map<String, List<String>> getAvailableEnums() {
    log.debug("Fetching available enums");

    Map<String, List<String>> enums = new HashMap<>();

    // Article Types
    enums.put("articleTypes", Arrays.asList("REGULAR", "EVENT", "NOTICE"));

    // Article Status
    enums.put(
        "articleStatus",
        Arrays.stream(Status.values()).map(Enum::name).collect(Collectors.toList()));

    // Event Status
    enums.put("eventStatus", Arrays.asList("UPCOMING", "ONGOING", "ENDED"));

    log.debug("Available enums: {}", enums);

    return enums;
  }
}

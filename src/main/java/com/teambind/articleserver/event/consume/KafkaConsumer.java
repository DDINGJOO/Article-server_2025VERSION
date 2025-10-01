package com.teambind.articleserver.event.consume;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class KafkaConsumer {
  private final ObjectMapper objectMapper;

  //	@KafkaListener(topics = "post-image-changed" , groupId = "article-consumer-group")
  //	public void profileImageChanger(String message) {
  //		try {
  //			List<> request = objectMapper.readValue(message, ProfileImageChanged.class);
  //			service.updateProfileImage(request.getReferenceId(), request.getImageUrl());
  //		} catch (Exception e) {
  //			// 역직렬화 실패 또는 처리 중 오류 발생 시 로깅/대응
  //			log.error("Failed to deserialize or process profile-create-request message: {}", message, e);
  //			// 필요하면 DLQ 전송이나 재시도 로직 추가
  //		}
  //	}
  //
  //	@KafkaListener(topics = "profile-create-request" , groupId = "profile-consumer-group")
  //	public void createUserProfile(String message) {
  //		try {
  //			ProfileCreateRequest request = objectMapper.readValue(message, ProfileCreateRequest.class);
  //			createUserProfile.createUserProfile(request.getUserId(), request.getProvider());
  //		} catch (Exception e) {
  //			// 역직렬화 실패 또는 처리 중 오류 발생 시 로깅/대응
  //			log.error("Failed to deserialize or process profile-create-request message: {}", message, e);
  //			// 필요하면 DLQ 전송이나 재시도 로직 추가
  //		}
  //	}
}

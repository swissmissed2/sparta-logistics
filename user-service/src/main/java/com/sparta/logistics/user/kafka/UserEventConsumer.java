package com.sparta.logistics.user.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.common.kafka.KafkaTopics;
import com.sparta.logistics.common.kafka.event.CompanyDeletedEvent;
import com.sparta.logistics.common.kafka.event.HubDeletedEvent;
import com.sparta.logistics.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.HUB_DELETED, groupId = "user-service")
    public void handleHubDeleted(String message) {
        HubDeletedEvent event;
        try {
            event = objectMapper.readValue(message, HubDeletedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("[Kafka][수동처리 필요] hub.deleted 역직렬화 실패 — message={}", message, e);
            return;
        }
        try {
            userService.softDeleteUsersByHubId(event.getHubId(), event.getDeletedBy());
            log.info("[Kafka] hub.deleted 처리 완료 — hubId={}", event.getHubId());
        } catch (Exception e) {
            log.error("[Kafka][수동처리 필요] hub.deleted 처리 실패 — hubId={}", event.getHubId(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "company.deleted", groupId = "user-service")
    public void handleCompanyDeleted(String message) {
        CompanyDeletedEvent event;
        try {
            event = objectMapper.readValue(message, CompanyDeletedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("[Kafka][수동처리 필요] company.deleted 역직렬화 실패 — message={}", message, e);
            return;
        }
        try {
            userService.softDeleteUsersByCompanyId(event.getCompanyId(), event.getDeletedBy());
            log.info("[Kafka] company.deleted 처리 완료 — companyId={}", event.getCompanyId());
        } catch (Exception e) {
            log.error("[Kafka][수동처리 필요] company.deleted 처리 실패 — companyId={}", event.getCompanyId(), e);
            throw new RuntimeException(e);
        }
    }
}

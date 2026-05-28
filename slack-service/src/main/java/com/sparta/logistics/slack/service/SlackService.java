package com.sparta.logistics.slack.service;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.slack.dto.request.SlackMessageCreateRequest;
import com.sparta.logistics.slack.dto.request.SlackMessageSearchCondition;
import com.sparta.logistics.slack.dto.request.SlackMessageUpdateRequest;
import com.sparta.logistics.slack.dto.response.SlackMessageResponse;
import com.sparta.logistics.slack.entity.SlackMessage;
import com.sparta.logistics.slack.exception.SlackErrorCode;
import com.sparta.logistics.slack.repository.SlackMessageRepository;
import com.sparta.logistics.slack.sender.SlackSendResult;
import com.sparta.logistics.slack.sender.SlackSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackService {

    private final SlackMessageRepository slackMessageRepository;
    private final SlackSender slackSender;

    @Transactional
    public SlackMessageResponse createSlackMessage(SlackMessageCreateRequest request, UUID senderId) {
        SlackMessage slackMessage = SlackMessage.builder()
                .receiverSlackId(request.receiverSlackId())
                .message(request.message())
                .messageType(request.messageType())
                .relatedType(request.relatedType())
                .relatedId(request.relatedId())
                .senderId(senderId)
                .build();

        SlackMessage savedMessage = slackMessageRepository.save(slackMessage);
        try{
            SlackSendResult sendResult = slackSender.send(
                savedMessage.getId(),
                savedMessage.getReceiverSlackId(),
                savedMessage.getMessage(),
                savedMessage.getMessageType(),
                savedMessage.getRelatedType(),
                savedMessage.getRelatedId()
            );
            savedMessage.markAsSent(sendResult.slackTs(), sendResult.slackChannelId());
        } catch (Exception e){
            savedMessage.markAsFailed();
        }
        return toResponse(savedMessage);
    }

    public Page<SlackMessageResponse> getSlackMessages(
            SlackMessageSearchCondition condition,
            Pageable pageable
    ) {
        return slackMessageRepository.search(
                condition.receiverSlackId(),
                condition.messageType(),
                condition.status(),
                condition.relatedType(),
                condition.relatedId(),
                pageable
        ).map(this::toResponse);
    }

    @Transactional
    public SlackMessageResponse updateSlackMessage(UUID messageId, SlackMessageUpdateRequest request) {
        SlackMessage slackMessage = findSlackMessage(messageId);

        try {
            slackMessage.updateMessage(request.message());
        } catch (IllegalStateException e) {
            throw new BusinessException(SlackErrorCode.SLACK_MESSAGE_NOT_UPDATABLE);
        }

        // Auditing 필드(updatedAt)를 응답에 최신 값으로 반영하기 위해 flush
        slackMessageRepository.flush();

        return toResponse(slackMessage);
    }

    private SlackMessage findSlackMessage(UUID messageId) {
        return slackMessageRepository.findByIdAndDeletedAtIsNull(messageId)
                .orElseThrow(() -> new BusinessException(SlackErrorCode.SLACK_MESSAGE_NOT_FOUND));
    }

    private SlackMessageResponse toResponse(SlackMessage slackMessage) {
        return new SlackMessageResponse(
                slackMessage.getId(),
                slackMessage.getReceiverSlackId(),
                slackMessage.getMessage(),
                slackMessage.getMessageType(),
                slackMessage.getStatus(),
                slackMessage.getRelatedType(),
                slackMessage.getRelatedId(),
                slackMessage.getSenderId(),
                slackMessage.getRetryCount(),
                slackMessage.getSentAt(),
                slackMessage.getSlackTs(),
                slackMessage.getSlackChannelId(),
                slackMessage.getCreatedAt(),
                slackMessage.getUpdatedAt()
        );
    }
}

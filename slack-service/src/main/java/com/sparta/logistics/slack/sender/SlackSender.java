package com.sparta.logistics.slack.sender;

import com.sparta.logistics.slack.enums.MessageType;
import com.sparta.logistics.slack.enums.RelatedType;

import java.util.UUID;

public interface SlackSender {
  SlackSendResult send(
      UUID messageId,
      String receiverSlackId,
      String message,
      MessageType messageType,
      RelatedType relatedType,
      UUID relatedId
  );
}

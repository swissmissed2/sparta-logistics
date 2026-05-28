package com.sparta.logistics.slack.sender;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.sparta.logistics.slack.enums.MessageType;
import com.sparta.logistics.slack.enums.RelatedType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Primary
@Component
public class SlackApiSender implements SlackSender{

  @Value("${slack.bot.token}")
  private String token;

  @Override
  public SlackSendResult send(UUID messageId, String receiverSlackId, String message, MessageType messageType, RelatedType relatedType, UUID relatedId){

    try{
      Slack slack = Slack.getInstance();
      MethodsClient methods = slack.methods(token);

      ChatPostMessageRequest request = ChatPostMessageRequest.builder()
          .channel(receiverSlackId)
          .text(message)
          .build();

      ChatPostMessageResponse response = methods.chatPostMessage(request);

      if (response.isOk()){
        return new SlackSendResult(response.getTs(), response.getChannel());
      } else {
        throw new RuntimeException("슬랙 메시지 발송 실패: " + response.getError());
      }

    } catch (Exception e){
      throw new RuntimeException("슬랙 API 통신 중 서버 에러 발생", e);
    }
  }
}

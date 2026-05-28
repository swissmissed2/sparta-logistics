package com.sparta.logistics.slack.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackKafkaConsumer {

  //private final GeminiService geminiService //AI  가공 서비스 의존성 주입
  //private final SlackService slackService; //슬랙 발송 서비스 의존성 주입

  @KafkaListener(topics = "delivery.created", groupId = "slack-service-group")
  public void consumeDeliveryCreated(DeliveryCreatedEvent event){
    log.info("카프카로부터 배송 생성 이벤트 수신 완료: {}", event.deliveryId());

    try{
      //1.수신한 데이터를 바탕으로 Gemini 프롬프트 구성 및 최종 발송 시한 계산 요청
      //String aiMessage = geminiService.generateDeadlineMessage(event); [cite: 2594]

      //2.가공된 메시지를 슬랙 API를 이용해 발송
      //slackService.sendSlackNotification(event.receiverSlackId(), aiMessage); [cite: 2589]

      //3.AI 로그 및 슬랙 메시지 이력 DB 저장 [cite: 2590, 2596]
    } catch (Exception e) {
      log.error("배송 이벤트 처리 중 에러 발생", e);
      //필요 시 재시도 로직이나 예외 로그 저장 [cite: 2552]
    }
  }

}

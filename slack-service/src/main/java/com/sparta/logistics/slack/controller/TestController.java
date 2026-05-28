package com.sparta.logistics.slack.controller;

import com.sparta.logistics.slack.client.GeminiApiClient;
import com.sparta.logistics.slack.enums.MessageType;
import com.sparta.logistics.slack.enums.RelatedType;
import com.sparta.logistics.slack.sender.SlackApiSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TestController {

  private final GeminiApiClient geminiApiClient;
  private final SlackApiSender slackApiSender;

  @Value("${slack.receiver.test-id}")
  private String receiverSlackId;

  @GetMapping("/api/test/ai-slack")
  public String testAiAndSlack() {
    // 1. 요구사항에 있는 예시 데이터를 텍스트로 임의 구성 (나중에는 카프카에서 JSON으로 받아옴)
    String dummyData = """
        주문 번호 : 1
        주문자 정보 : 김말숙 / msk@seafood.world
        주문 시간 : 2025-12-08 10:00:00
        상품 정보 : 마른 오징어 50박스
        요청 사항 : 12월 12일 3시까지는 보내주세요!
        발송지 : 경기 북부 센터
        경유지 : 대전광역시 센터, 부산광역시 센터
        도착지 : 부산시 사하구 낙동대로 1번길 1 해산물월드
        배송담당자 : 고길동 / kdk@sparta.world
        """;

    // 2. Gemini API 호출해서 텍스트 생성
    String aiMessage = geminiApiClient.generateDeliveryDeadlineMessage(dummyData);

    // 3. 슬랙으로 발송 (receiverSlackId에는 본인의 슬랙 아이디나 테스트 채널명, 예: "#일반" 입력)
    slackApiSender.send(
        UUID.randomUUID(),
        receiverSlackId,
        aiMessage,
        MessageType.AI_DELIVERY_DEADLINE,
        RelatedType.DELIVERY,
        UUID.randomUUID()
    );

    return "AI 슬랙 발송 성공! 생성된 메시지: \n" + aiMessage;
  }
}
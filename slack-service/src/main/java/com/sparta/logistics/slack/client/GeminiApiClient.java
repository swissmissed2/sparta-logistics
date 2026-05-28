package com.sparta.logistics.slack.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

  private final WebClient webClient;

  @Value("${gemini.api.key}")
  private String geminiApiKey;

  @SuppressWarnings("unchecked")
  public String generateDeliveryDeadlineMessage(String promptData){
    //1. 프롬프트 템플릿 구성
    String systemPrompt = """
        당신은 물류 배송 전문가입니다. 제공된 데이터를 분석하여 '발송 허브 담당자'가 납기일자에 맞출 수 있도록 '최종 발송 시한'을 계산하고 슬랙 메시지를 생성해주세요.
        
        ### 요구사항(조건):
        1. 배송 담당자의 근무 시간은 '09:00 ~ 18:00'입니다. 이동 거리(경유지 포함)와 근무 시간을 논리적으로 고려하여, 고객의 납기 요청에 맞출 수 있는 '최종 발송 시한(날짜 및 시간)'을 정확히 도출하세요.
        2. '안녕하세요', '분석 결과입니다' 등 불필요한 서론과 결론은 절대 작성하지 마세요.
        3. 제공된 데이터를 바탕으로 반드시 아래의 [메시지 출력 포맷]과 100% 동일한 양식으로만 답변을 출력하세요.
        
        ### [메시지 출력 포맷]
        주문 번호 : {주문번호}
        주문자 정보: {주문자명} / {주문자이메일}
        주문 시간 : {주문 시간}
        상품 정보 : {상품명} {수량}
        요청 사항: {요청 사항}
        발송지: {발송지}
        경유지 : {경유지 (없으면 '없음')}
        도착지 : {도착지}
        배송 담당자: {배송담당자명} / {배송담당자이메일}
        
        위 내용을 기반으로 도출된 최종 발송 시한은 {계산된_월_일} {오전/오후} {시간}시 입니다.
        ### 배송 정보:
        """ + promptData;

    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> parts = new HashMap<>();
    parts.put("text", systemPrompt);
    Map<String, Object> contents = new HashMap<>();
    contents.put("parts", List.of(parts));
    requestBody.put("contents", List.of(contents));

    //2. Gemini API 요청 스펙에 맞게 JSON Body 구성
    try{
    //3. WebClient로 비동기/동기 API 호출
      Map<String, Object> response = webClient.post()
          .uri(uriBuilder -> uriBuilder.queryParam("key", geminiApiKey).build())
          .bodyValue(requestBody)
          .retrieve()
          .bodyToMono(Map.class)
          .block(); //동기(block)처리, 필요시 Mono 비동기로 유지

    //4.응답 JSON에서 텍스트 파싱
      return parseGeminiResponse(response);
    } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
      log.error("Gemini API 거절 상세 이유: {}", e.getResponseBodyAsString());
      throw new RuntimeException("AI 메시지 생성 실패"+ e.getResponseBodyAsString(), e);
    } catch (Exception e){
      log.error("Gemini API 호출 중 에러 발생" , e);
      throw new RuntimeException("AI 메시지 생성 실패", e);
    }
  }

  //Gemini API의 JSON 응답 구조에서 실제 답변 텍스트만 추출하는 메서드
  @SuppressWarnings("unchecked")
  private String parseGeminiResponse(Map<String, Object> response){
    try{
      List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
      Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
      List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
      return (String) parts.get(0).get("text");
    } catch (Exception e) {
      log.error("Gemini 응답 파싱 실패: {}", response);
      return "배송 정보 알림 생성을 실패했습니다. 기본 메시지로 대체합니다.";
    }
  }

}

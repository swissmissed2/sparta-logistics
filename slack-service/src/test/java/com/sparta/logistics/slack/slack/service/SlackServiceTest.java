package com.sparta.logistics.slack.slack.service;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.slack.dto.request.SlackMessageCreateRequest;
import com.sparta.logistics.slack.dto.request.SlackMessageUpdateRequest;
import com.sparta.logistics.slack.dto.response.SlackMessageResponse;
import com.sparta.logistics.slack.entity.SlackMessage;
import com.sparta.logistics.slack.enums.MessageType;
import com.sparta.logistics.slack.enums.SlackMessageStatus;
import com.sparta.logistics.slack.exception.SlackErrorCode;
import com.sparta.logistics.slack.repository.SlackMessageRepository;
import com.sparta.logistics.slack.sender.SlackSendResult;
import com.sparta.logistics.slack.sender.FakeSlackSender;
import com.sparta.logistics.slack.service.SlackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class SlackServiceTest {

  @InjectMocks
  private SlackService slackService;

  @Mock
  private SlackMessageRepository slackMessageRepository;

  @Mock
  private FakeSlackSender fakeSlackSender;

  @Test
  @DisplayName("FakeSlackSender 발송 실패 시 예외가 전파되지 않고 FAILED 상태로 반환")
  void createSlackMessageFail(){
    UUID senderId = UUID.randomUUID();
    SlackMessageCreateRequest request = new SlackMessageCreateRequest(
        "U12345", "테스트 메시지", MessageType.MANUAL, null, null
    );

    SlackMessage savedMessage = SlackMessage.builder()
        .id(UUID.randomUUID())
        .message("테스트 메시지")
        .status(SlackMessageStatus.PENDING)
        .build();

    given(slackMessageRepository.save(any(SlackMessage.class))).willReturn(savedMessage);

    //발송 시 강제로 예외 발생
    given(fakeSlackSender.send(any(), any(), any(), any(), any(), any()))
        .willThrow(new RuntimeException("Fake 슬랙 발송 에러 발생"));

    SlackMessageResponse response = slackService.createSlackMessage(request, senderId);

    assertThat(response.status()).isEqualTo(SlackMessageStatus.FAILED);
    assertThat(savedMessage.getStatus()).isEqualTo(SlackMessageStatus.FAILED);

  }

  @Test
  @DisplayName("메시지 발송 성공 시 상태가 SENT로 변경되어 반환")
  void createSlackMessageSuccess(){
    UUID senderId = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();

    SlackMessageCreateRequest request = new SlackMessageCreateRequest(
        "U12345", "테스트메시지", MessageType.MANUAL, null, null
    );

    SlackMessage savedMessage = SlackMessage.builder()
        .id(messageId)
        .message("테스트 메시지")
        .status(SlackMessageStatus.PENDING)
        .build();

    given(slackMessageRepository.save(any(SlackMessage.class))).willReturn(savedMessage);

    SlackSendResult fakeResult = new SlackSendResult("fake-ts-" + messageId, "fake-channel");

    given(fakeSlackSender.send(any(), any(), any(), any(), any(), any())).willReturn(fakeResult);

    SlackMessageResponse response = slackService.createSlackMessage(request, senderId);

    // 1) 응답 DTO와 엔티티의 상태가 SENT로 바뀌었는지 검증
    assertThat(response.status()).isEqualTo(SlackMessageStatus.SENT);
    assertThat(savedMessage.getStatus()).isEqualTo(SlackMessageStatus.SENT);

    //2) FakeSlackSender가 반환한 값(ts, channel)이 엔티티에 잘 세팅되었는지 추가 검증
    assertThat(savedMessage.getSlackTs()).isEqualTo("fake-ts-" + messageId);
    assertThat(savedMessage.getSlackChannelId()).isEqualTo("fake-channel");
  }

  @Test
  @DisplayName("이미 발송된 메시지(status =SENT)를 수정하려고 하면 예외 발생")
  void updateMessageFailWhenSent(){
    UUID messageId = UUID.randomUUID();
    SlackMessage sentMessage = SlackMessage.builder()
        .id(messageId)
        .message("초기 메시지")
        .status(SlackMessageStatus.SENT)
        .build();

    //DB 조회 상황 가정
    given(slackMessageRepository.findByIdAndDeletedAtIsNull(messageId)).willReturn(java.util.Optional.of(sentMessage));

    SlackMessageUpdateRequest request = new SlackMessageUpdateRequest("수정된 내용");

    assertThatThrownBy(() -> slackService.updateSlackMessage(messageId, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> {
          BusinessException businessEx = (BusinessException) exception;
          assertThat(businessEx.getErrorCode()).isEqualTo(SlackErrorCode.SLACK_MESSAGE_NOT_UPDATABLE);
        });
  }

  @Test
  @DisplayName("발송 실패한 메시지를 수정하려고 하면 BusinessException 발생")
  void updateMessageFailWhenFailed(){
    UUID messageId = UUID.randomUUID();
    SlackMessage failedMessage = SlackMessage.builder()
        .id(messageId)
        .message("초기 메시지")
        .status(SlackMessageStatus.FAILED)
        .build();

    given(slackMessageRepository.findByIdAndDeletedAtIsNull(messageId)).willReturn(java.util.Optional.of(failedMessage));

    SlackMessageUpdateRequest request = new SlackMessageUpdateRequest("수정된 내용");

    assertThatThrownBy(() -> slackService.updateSlackMessage(messageId, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> {
          BusinessException businessEx = (BusinessException) exception;
          assertThat(businessEx.getErrorCode()).isEqualTo(SlackErrorCode.SLACK_MESSAGE_NOT_UPDATABLE);
        });
  }
}

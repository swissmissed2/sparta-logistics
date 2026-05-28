package com.sparta.logistics.slack.enums;

public enum MessageType {
    MANUAL, //수동 발송 메시지
    DEADLINE_ALERT, //일반 마감 알림
    DAILY_ROUTINE, //일일 반복 알림
    AI_DELIVERY_DEADLINE //AI가 계산한 배송 발송 시한 알림
}

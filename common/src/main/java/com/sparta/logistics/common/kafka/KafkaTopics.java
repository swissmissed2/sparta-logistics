package com.sparta.logistics.common.kafka;

public final class KafkaTopics {

    private KafkaTopics() {}

    // Choreography Saga : 주문 생성
    public static final String ORDER_CREATED               = "order.created";
    public static final String STOCK_RESERVED              = "stock.reserved";
    public static final String STOCK_RESERVATION_FAILED    = "stock.reservation.failed";
    public static final String DELIVERY_CREATED            = "delivery.created";
    public static final String DELIVERY_CREATION_FAILED    = "delivery.creation.failed";

    // Orchestration Saga : 주문 취소
    public static final String CANCEL_DELIVERY_COMMAND     = "cancel.delivery.command";
    public static final String DELIVERY_CANCELLED_ACK      = "delivery.cancelled.ack";
    public static final String RESTORE_STOCK_COMMAND       = "restore.stock.command";
    public static final String STOCK_RESTORED_ACK          = "stock.restored.ack";

    // 재고 스냅샷 동기화
    public static final String HUB_STOCK_UPDATED           = "hub.stock.updated";

    // AI 발송 시한 + 배송 시작
    public static final String AI_DEADLINE_CALCULATED      = "ai.deadline.calculated";
    public static final String DELIVERY_STARTED            = "delivery.started";

    // Orchestration Saga 보상 : 주문 취소
    public static final String DELIVERY_CANCELLATION_FAILED = "delivery.cancellation.failed";
    public static final String STOCK_RESTORATION_FAILED     = "stock.restoration.failed";

    // 허브 이벤트
    public static final String HUB_DELETED = "hub.deleted";
}

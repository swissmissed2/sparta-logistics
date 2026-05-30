package com.sparta.logistics.delivery.service;

import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.entity.DeliveryOrderItemEntity;
import com.sparta.logistics.delivery.kafka.producer.DeliveryEventPublisher;
import com.sparta.logistics.delivery.repository.DeliveryLogRepository;
import com.sparta.logistics.delivery.repository.DeliveryOrderItemRepository;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import com.sparta.logistics.delivery.repository.DeliveryRouteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceOrderItemCleanupTest {

    @Mock DeliveryRepository deliveryRepository;
    @Mock DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock DeliveryEventPublisher eventPublisher;
    @Mock DeliveryLogRepository deliveryLogRepository;
    @Mock DeliveryRouteRepository deliveryRouteRepository;

    @InjectMocks DeliveryService service;

    @Test
    void delivery_started_발행_후_OrderItem_삭제() {
        UUID deliveryId = UUID.randomUUID();
        DeliveryEntity entity = new DeliveryEntity(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "주소", "slack");
        ReflectionTestUtils.setField(entity, "id", deliveryId);

        DeliveryOrderItemEntity item = new DeliveryOrderItemEntity(
                entity, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(entity));
        when(deliveryOrderItemRepository.findByDelivery_Id(deliveryId)).thenReturn(List.of(item));
        doNothing().when(deliveryOrderItemRepository).deleteByDelivery_Id(deliveryId);

        ArgumentCaptor<TransactionSynchronization> syncCaptor =
                ArgumentCaptor.forClass(TransactionSynchronization.class);

        try (MockedStatic<TransactionSynchronizationManager> mocked =
                     mockStatic(TransactionSynchronizationManager.class)) {
            mocked.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(inv -> null);

            service.updateFinalDispatchDeadline(deliveryId, null);

            mocked.verify(() -> TransactionSynchronizationManager.registerSynchronization(
                    syncCaptor.capture()));
        }

        // afterCommit 수동 실행
        syncCaptor.getValue().afterCommit();

        verify(eventPublisher).publishStarted(any(), any(), any());
        verify(deliveryOrderItemRepository).deleteByDelivery_Id(deliveryId);
    }
}

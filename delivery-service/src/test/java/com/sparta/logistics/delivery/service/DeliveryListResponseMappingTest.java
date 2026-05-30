package com.sparta.logistics.delivery.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.delivery.dto.DeliveryListResponse;
import com.sparta.logistics.delivery.dto.DeliverySearchCond;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.repository.DeliveryLogRepository;
import com.sparta.logistics.delivery.repository.DeliveryOrderItemRepository;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import com.sparta.logistics.delivery.repository.DeliveryRouteRepository;
import com.sparta.logistics.delivery.kafka.producer.DeliveryEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryListResponseMappingTest {

    @Mock DeliveryRepository deliveryRepository;
    @Mock DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock DeliveryEventPublisher eventPublisher;
    @Mock DeliveryLogRepository deliveryLogRepository;
    @Mock DeliveryRouteRepository deliveryRouteRepository;

    @InjectMocks DeliveryService service;

    @Test
    void 배송_목록_조회시_허브ID와_담당자ID가_null_아님() {
        UUID sourceHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        DeliveryEntity entity = new DeliveryEntity(UUID.randomUUID(), UUID.randomUUID(),
                sourceHubId, destinationHubId, "주소", "slack");
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(entity, "companyDeliveryManagerId", managerId);

        when(deliveryRepository.findAllByCondition(any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<DeliveryListResponse> result = service.getDeliveryList(
                UUID.randomUUID(), Role.MASTER, null,
                PageRequest.of(0, 10), new DeliverySearchCond());

        DeliveryListResponse response = result.getContent().get(0);
        assertThat(response.getSourceHubName()).isEqualTo(sourceHubId.toString());
        assertThat(response.getDestinationHubName()).isEqualTo(destinationHubId.toString());
        assertThat(response.getDeliveryManagerName()).isEqualTo(managerId.toString());
    }

    @Test
    void 배송_목록_조회시_허브ID와_담당자ID가_없으면_null_반환() {
        DeliveryEntity entity = new DeliveryEntity(UUID.randomUUID(), UUID.randomUUID(),
                null, null, "주소", "slack");
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());

        when(deliveryRepository.findAllByCondition(any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<DeliveryListResponse> result = service.getDeliveryList(
                UUID.randomUUID(), Role.MASTER, null,
                PageRequest.of(0, 10), new DeliverySearchCond());

        DeliveryListResponse response = result.getContent().get(0);
        assertThat(response.getSourceHubName()).isNull();
        assertThat(response.getDestinationHubName()).isNull();
        assertThat(response.getDeliveryManagerName()).isNull();
    }
}

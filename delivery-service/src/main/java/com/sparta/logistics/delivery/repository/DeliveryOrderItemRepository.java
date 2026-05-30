package com.sparta.logistics.delivery.repository;

import com.sparta.logistics.delivery.entity.DeliveryOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryOrderItemRepository extends JpaRepository<DeliveryOrderItemEntity, UUID> {
    List<DeliveryOrderItemEntity> findByDelivery_Id(UUID deliveryId);

    void deleteByDelivery_Id(UUID deliveryId);
}
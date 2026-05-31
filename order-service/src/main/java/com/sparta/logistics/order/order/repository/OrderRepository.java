package com.sparta.logistics.order.order.repository;

import com.sparta.logistics.order.order.entity.Order;
import com.sparta.logistics.order.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, LocalDateTime threshold);

    @Query("""
            SELECT o.id FROM Order o
            WHERE (o.requesterCompanyId = :companyId OR o.receiverCompanyId = :companyId)
              AND o.deletedAt IS NULL
            """)
    List<UUID> findOrderIdsByCompanyId(@Param("companyId") UUID companyId);

    @Query("""
            SELECT COUNT(o) > 0 FROM Order o
            WHERE o.id = :orderId
              AND (o.requesterCompanyId = :companyId OR o.receiverCompanyId = :companyId)
              AND o.deletedAt IS NULL
            """)
    boolean existsByOrderIdAndCompanyId(@Param("orderId") UUID orderId, @Param("companyId") UUID companyId);

    @Query("""
            SELECT o FROM Order o
            WHERE (:requesterUserId IS NULL OR o.requesterUserId = :requesterUserId)
              AND (:requesterCompanyId IS NULL OR o.requesterCompanyId = :requesterCompanyId)
              AND (:receiverCompanyId IS NULL OR o.receiverCompanyId = :receiverCompanyId)
              AND (:status IS NULL OR o.status = :status)
              AND (:dueDateFrom IS NULL OR o.dueDate >= :dueDateFrom)
              AND (:dueDateTo IS NULL OR o.dueDate <= :dueDateTo)
            """)
    Page<Order> search(
            @Param("requesterUserId") UUID requesterUserId,
            @Param("requesterCompanyId") UUID requesterCompanyId,
            @Param("receiverCompanyId") UUID receiverCompanyId,
            @Param("status") OrderStatus status,
            @Param("dueDateFrom") LocalDateTime dueDateFrom,
            @Param("dueDateTo") LocalDateTime dueDateTo,
            Pageable pageable
    );
}

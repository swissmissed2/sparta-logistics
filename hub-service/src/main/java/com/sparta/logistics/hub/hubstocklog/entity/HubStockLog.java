package com.sparta.logistics.hub.hubstocklog.entity;

import com.sparta.logistics.common.domain.BaseEntity;
import com.sparta.logistics.hub.hubstock.entity.HubStock;
import com.sparta.logistics.hub.hubstock.enums.HubStockChangeType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "p_hub_stock_log", schema = "schema_hub")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HubStockLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_stock_id", nullable = false)
    private HubStock hubStock;

    @Column
    private UUID orderItemId;

    @Column
    private UUID deliveryId;

    @Column(nullable = false)
    private Integer changeQuantity;

    @Column(nullable = false)
    private Integer beforeQuantity;

    @Column(nullable = false)
    private Integer afterQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HubStockChangeType changeType;

    public static HubStockLog create(HubStock hubStock,
                                     Integer changeQuantity,
                                     Integer beforeQuantity,
                                     Integer afterQuantity,
                                     HubStockChangeType changeType) {
        return HubStockLog.builder()
                .hubStock(hubStock)
                .changeQuantity(changeQuantity)
                .beforeQuantity(beforeQuantity)
                .afterQuantity(afterQuantity)
                .changeType(changeType)
                .build();
    }
}

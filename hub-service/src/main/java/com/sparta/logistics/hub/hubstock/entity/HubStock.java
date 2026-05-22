package com.sparta.logistics.hub.hubstock.entity;

import com.sparta.logistics.common.domain.BaseEntity;
import com.sparta.logistics.hub.hub.entity.Hub;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "p_hub_stock",
        schema = "schema_hub"
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HubStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @Column(nullable = false)
    private UUID productId;

    @Builder.Default
    @Column(nullable = false)
    private Integer available = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer reserved = 0;

    @Version
    private Long version;


    public static HubStock create(Hub hub, UUID productId, Integer initialQuantity) {
        return HubStock.builder()
                .hub(hub)
                .productId(productId)
                .available(initialQuantity)
                .build();
    }

    public void adjustAvailable(Integer changeQuantity) {
        this.available += changeQuantity;
    }
}

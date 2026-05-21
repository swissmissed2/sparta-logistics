package com.sparta.logistics.hub.hubroute.entity;


import com.sparta.logistics.common.domain.BaseEntity;
import com.sparta.logistics.hub.hub.entity.Hub;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "p_hub_route",
        schema = "schema_hub",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hub_route_source_destination",
                        columnNames = {"source_hub_id", "destination_hub_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HubRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_hub_id", nullable = false)
    private Hub sourceHub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_hub_id", nullable = false)
    private Hub destinationHub;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal distance;

    @Column(nullable = false)
    private Integer duration;

    public static HubRoute create(Hub sourceHub, Hub destinationHub, BigDecimal distance, Integer duration) {
        return HubRoute.builder()
                .sourceHub(sourceHub)
                .destinationHub(destinationHub)
                .distance(distance)
                .duration(duration)
                .build();
    }

    public void update(BigDecimal distance, Integer duration) {
        this.distance = distance;
        this.duration = duration;
    }

    public void delete(UUID userId) {
        super.softDelete(userId);
    }
}

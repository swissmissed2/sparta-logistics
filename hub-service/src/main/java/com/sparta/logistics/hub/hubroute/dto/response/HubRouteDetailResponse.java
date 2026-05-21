package com.sparta.logistics.hub.hubroute.dto.response;

import com.sparta.logistics.hub.hubroute.entity.HubRoute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubRouteDetailResponse {

    private UUID routeId;
    private UUID sourceHubId;
    private String sourceHubName;
    private UUID destinationHubId;
    private String destinationHubName;
    private BigDecimal distance;
    private Integer duration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static HubRouteDetailResponse from(HubRoute hubRoute) {
        return HubRouteDetailResponse.builder()
                .routeId(hubRoute.getId())
                .sourceHubId(hubRoute.getSourceHub().getId())
                .sourceHubName(hubRoute.getSourceHub().getName())
                .destinationHubId(hubRoute.getDestinationHub().getId())
                .destinationHubName(hubRoute.getDestinationHub().getName())
                .distance(hubRoute.getDistance())
                .duration(hubRoute.getDuration())
                .createdAt(hubRoute.getCreatedAt())
                .updatedAt(hubRoute.getUpdatedAt())
                .build();
    }
}

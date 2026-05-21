package com.sparta.logistics.hub.hubroute.dto.response;

import com.sparta.logistics.hub.hubroute.entity.HubRoute;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class HubRouteUpdateResponse {

    private UUID routeId;
    private BigDecimal distance;
    private Integer duration;
    private LocalDateTime updatedAt;

    public static HubRouteUpdateResponse from(HubRoute hubRoute) {
        return HubRouteUpdateResponse.builder()
                .routeId(hubRoute.getId())
                .distance(hubRoute.getDistance())
                .duration(hubRoute.getDuration())
                .updatedAt(hubRoute.getUpdatedAt())
                .build();
    }
}

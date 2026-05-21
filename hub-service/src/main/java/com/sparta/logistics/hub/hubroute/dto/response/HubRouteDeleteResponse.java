package com.sparta.logistics.hub.hubroute.dto.response;

import com.sparta.logistics.hub.hubroute.entity.HubRoute;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class HubRouteDeleteResponse {

    private UUID routeId;
    private LocalDateTime deletedAt;

    public static HubRouteDeleteResponse from(HubRoute hubRoute) {
        return HubRouteDeleteResponse.builder()
                .routeId(hubRoute.getId())
                .deletedAt(hubRoute.getDeletedAt())
                .build();
    }
}

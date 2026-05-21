package com.sparta.logistics.hub.hub.dto.response;

import com.sparta.logistics.hub.hub.entity.Hub;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class HubDeleteResponse {

    private UUID hubId;
    private LocalDateTime deletedAt;

    public static HubDeleteResponse from(Hub hub) {
        return HubDeleteResponse.builder()
                .hubId(hub.getId())
                .deletedAt(hub.getDeletedAt())
                .build();
    }
}

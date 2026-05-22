package com.sparta.logistics.hub.hubstocklog.controller;

import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.hub.hubstock.enums.HubStockChangeType;
import com.sparta.logistics.hub.hubstocklog.dto.response.HubStockLogListResponse;
import com.sparta.logistics.hub.hubstocklog.service.HubStockLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hubs/{hubId}/stocks")
public class HubStockLogController {

    private final HubStockLogService hubStockLogService;

    @GetMapping("/{stockId}/logs")
    public ResponseEntity<ApiResponse<Page<HubStockLogListResponse>>> getHubStockLogList(
            @PathVariable UUID hubId,
            @PathVariable UUID stockId,
            @RequestParam(required = false) HubStockChangeType changeType,
            Pageable pageable) {

        Page<HubStockLogListResponse> response =
                hubStockLogService.getHubStockLogList(hubId, stockId, changeType, pageable);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

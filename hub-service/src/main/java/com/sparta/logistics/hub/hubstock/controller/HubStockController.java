package com.sparta.logistics.hub.hubstock.controller;

import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.hub.hubstock.dto.request.AdjustHubStockRequest;
import com.sparta.logistics.hub.hubstock.dto.request.CreateHubStockRequest;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockAdjustResponse;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockCreateResponse;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockListResponse;
import com.sparta.logistics.hub.hubstock.service.HubStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hubs/{hubId}/stocks")
@RequiredArgsConstructor
public class HubStockController {

    private final HubStockService hubStockService;

    @PostMapping
    public ResponseEntity<ApiResponse<HubStockCreateResponse>> createHubStock(
            @PathVariable UUID hubId,
            @RequestBody @Valid CreateHubStockRequest request) {

        HubStockCreateResponse response = hubStockService.createHubStock(hubId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("허브 재고가 생성되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HubStockListResponse>>> getHubStockList(
            @PathVariable UUID hubId,
            @RequestParam(required = false) UUID productId,
            Pageable pageable) {

        Page<HubStockListResponse> response = hubStockService.getHubStockList(hubId, productId, pageable);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{stockId}")
    public ResponseEntity<ApiResponse<HubStockAdjustResponse>> adjustHubStock(
            @PathVariable UUID hubId,
            @PathVariable UUID stockId,
            @RequestBody @Valid AdjustHubStockRequest request) {

        HubStockAdjustResponse response = hubStockService.adjustHubStock(hubId, stockId, request);

        return ResponseEntity.ok(ApiResponse.ok("허브 재고가 수정되었습니다.", response));
    }
}

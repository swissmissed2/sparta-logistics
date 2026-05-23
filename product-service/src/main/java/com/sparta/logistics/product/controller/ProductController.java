package com.sparta.logistics.product.controller;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.product.dto.request.CreateRequest;
import com.sparta.logistics.product.dto.request.SearchCondition;
import com.sparta.logistics.product.dto.request.UpdateRequest;
import com.sparta.logistics.product.dto.response.DeleteResponse;
import com.sparta.logistics.product.dto.response.ProductResponse;
import com.sparta.logistics.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Product", description = "상품 관리 API")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private static final String USER_ID_HEADER      = "X-User-Id";
    private static final String USER_ROLE_HEADER     = "X-User-Role";
    private static final String USER_HUB_HEADER      = "X-User-Hub-Id";
    private static final String USER_COMPANY_HEADER  = "X-User-Company-Id";

    private final ProductService productService;

    // -------------------------------------------------------
    // POST /api/v1/products — 상품 생성
    // -------------------------------------------------------
    @Operation(summary = "상품 생성", description = "MASTER, HUB_MANAGER, COMPANY_MANAGER 가능")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(USER_ROLE_HEADER) Role userRole,
            @RequestHeader(value = USER_HUB_HEADER, required = false) UUID userHubId,
            @RequestHeader(value = USER_COMPANY_HEADER, required = false) UUID userCompanyId) {

        ProductResponse response =
                productService.createProduct(request, userId, userRole, userHubId, userCompanyId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("상품이 생성되었습니다.", response));
    }

    // -------------------------------------------------------
    // GET /api/v1/products — 목록 조회/검색
    // -------------------------------------------------------
    @Operation(summary = "상품 목록 조회/검색", description = "모든 로그인 사용자 가능")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        Pageable pageable = buildPageable(page, validatePageSize(size), sort);
        SearchCondition condition = new SearchCondition(name, companyId, hubId, status);
        Page<ProductResponse> result = productService.searchProducts(condition, pageable);

        return ResponseEntity.ok(ApiResponse.ok("요청이 성공적으로 처리되었습니다.", result));
    }

    // -------------------------------------------------------
    // GET /api/v1/products/{productId} — 단건 조회
    // -------------------------------------------------------
    @Operation(summary = "상품 단건 조회", description = "모든 로그인 사용자 가능")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable UUID productId) {

        return ResponseEntity.ok(
                ApiResponse.ok("요청이 성공적으로 처리되었습니다.",
                        productService.getProduct(productId)));
    }

    // -------------------------------------------------------
    // PUT /api/v1/products/{productId} — 수정
    // -------------------------------------------------------
    @Operation(summary = "상품 수정", description = "MASTER, HUB_MANAGER(담당 허브), COMPANY_MANAGER(본인 업체)")
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateRequest request,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(USER_ROLE_HEADER) Role userRole,
            @RequestHeader(value = USER_HUB_HEADER, required = false) UUID userHubId,
            @RequestHeader(value = USER_COMPANY_HEADER, required = false) UUID userCompanyId) {

        ProductResponse response = productService.updateProduct(productId, request, userRole,
                userHubId, userCompanyId);

        return ResponseEntity.ok(ApiResponse.ok("상품 정보가 수정되었습니다.", response));
    }

    // -------------------------------------------------------
    // DELETE /api/v1/products/{productId} — 논리 삭제
    // -------------------------------------------------------
    @Operation(summary = "상품 삭제 (Soft Delete)", description = "MASTER, HUB_MANAGER(담당 허브)")
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteProduct(
            @PathVariable UUID productId,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(USER_ROLE_HEADER) Role userRole,
            @RequestHeader(value = USER_HUB_HEADER, required = false) UUID userHubId) {

        DeleteResponse response =
                productService.deleteProduct(productId, userId, userRole, userHubId);

        return ResponseEntity.ok(ApiResponse.ok("상품이 삭제되었습니다.", response));
    }

    // -------------------------------------------------------
    // DELETE /api/v1/products/by-company/{companyId} — 내부 통신용
    // -------------------------------------------------------
    @Operation(summary = "업체 삭제 시 연관 상품 일괄 삭제", description = "Company Service 내부 통신 전용")
    @DeleteMapping("/by-company/{companyId}")
    public ResponseEntity<Void> deleteByCompanyId(@PathVariable UUID companyId) {
        productService.deleteAllByCompanyId(companyId);
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------
    // Utility
    // -------------------------------------------------------
    private int validatePageSize(int size) {
        if (size == 30 || size == 50) return size;
        return 10;
    }

    private Pageable buildPageable(int page, int size, String sort) {
        try{
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            return PageRequest.of(page, size, Sort.by(direction, field));
        } catch (Exception e) {
            return PageRequest.of(page, size, Sort.by(
                    Sort.Direction.DESC, "createdAt"));
        }
    }
}

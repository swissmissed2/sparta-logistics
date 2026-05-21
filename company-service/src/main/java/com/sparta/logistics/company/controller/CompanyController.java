package com.sparta.logistics.company.controller;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.company.dto.request.CreateRequest;
import com.sparta.logistics.company.dto.request.SearchCondition;
import com.sparta.logistics.company.dto.request.UpdateRequest;
import com.sparta.logistics.company.dto.response.CompanyResponse;
import com.sparta.logistics.company.dto.response.DeleteResponse;
import com.sparta.logistics.company.service.CompanyService;
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

@Tag(name = "Company", description = "업체 관리 API")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private static final String USER_ID_HEADER    = "X-User-Id";
    private static final String USER_ROLE_HEADER  = "X-User-Role";
    private static final String USER_HUB_HEADER   = "X-User-Hub-Id";
    private static final String USER_COMPANY_HEADER = "X-User-Company-Id";
    private final CompanyService companyService;

    // -------------------------------------------------------
    // POST /api/v1/companies — 업체 생성
    // -------------------------------------------------------
    @Operation(summary = "업체 생성", description = "MASTER, HUB_MANAGER(담당 허브)만 가능")
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany (
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(USER_ROLE_HEADER) Role userRole,
            @RequestHeader(value = USER_HUB_HEADER, required = false) UUID userHubId) {

        CompanyResponse response =
                companyService.createCompany(request, userRole, userHubId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("업체가 생성되었습니다.", response));
    }

    // -------------------------------------------------------
    // GET /api/v1/companies — 목록 조회/검색
    // -------------------------------------------------------
    @Operation(summary = "업체 목록 조회/검색", description = "모든 로그인 사용자 가능")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CompanyResponse>>> searchCompanies(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        // 페이지 크기 10/30.50만 허용, 이외는 10으로 고정
        int validatedSize = validatePageSize(size);

        Pageable pageable = buildPageable(page, validatedSize, sort);
        SearchCondition condition = new SearchCondition(name, type, hubId, status);
        Page<CompanyResponse> result = companyService.searchCompanies(condition, pageable);

        return ResponseEntity.ok(ApiResponse.ok("요청이 성공적으로 처리되었습니다.", result));
    }

    // -------------------------------------------------------
    // GET /api/v1/companies/{companyId} — 단건 조회
    // -------------------------------------------------------
    @Operation(summary = "업체 단건 조회", description = "모든 로그인 사용자 가능")
    @GetMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompany(
            @PathVariable UUID companyId) {
        return ResponseEntity.ok(
                ApiResponse.ok("요청이 성공적으로 처리되었습니다.", companyService.getCompany(companyId)));
    }

    // -------------------------------------------------------
    // GET /api/v1/companies/{companyId}/exists — 내부 서비스 통신용
    // -------------------------------------------------------
    @Operation(summary = "업체 존재 여부 확인", description = "서비스 내부 통신용 (Product Service FeignClient)")
    @GetMapping("/{companyId}/exists")
    public ResponseEntity<Void> checkCompanyExists(@PathVariable UUID companyId) {
        if (companyService.existsById(companyId)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // -------------------------------------------------------
    // PUT /api/v1/companies/{companyId} — 수정
    // -------------------------------------------------------
    @Operation(summary = "업체 수정")
    @PutMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody UpdateRequest request,
            @RequestHeader(USER_ROLE_HEADER) Role userRole,
            @RequestHeader(value = USER_HUB_HEADER, required = false) UUID userHubId,
            @RequestHeader(value = USER_COMPANY_HEADER, required = false) UUID userCompanyId) {

        CompanyResponse response = companyService.updateCompany(
                companyId, request, userRole,userHubId, userCompanyId);

        return ResponseEntity.ok(ApiResponse.ok("업체 정보가 수정되었습니다.", response));
    }

    // -------------------------------------------------------
    // DELETE /api/v1/companies/{companyId} — 논리 삭제
    // -------------------------------------------------------
    @Operation(summary = "업체 삭제 (Soft Delete)")
    @DeleteMapping("/{companyId}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteCompany(
            @PathVariable UUID companyId,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(USER_ROLE_HEADER) Role userRole,
            @RequestHeader(value = USER_HUB_HEADER, required = false) UUID userHubId) {

        DeleteResponse response = companyService.deleteCompany(
                companyId, userId, userRole, userHubId);

        return ResponseEntity.ok(ApiResponse.ok("업체가 삭제되었습니다.", response));
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

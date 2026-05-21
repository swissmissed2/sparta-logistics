package com.sparta.logistics.company.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeleteResponse(UUID companyId, LocalDateTime deletedAt) {}

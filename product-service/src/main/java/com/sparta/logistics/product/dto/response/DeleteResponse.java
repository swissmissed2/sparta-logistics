package com.sparta.logistics.product.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeleteResponse(UUID productId, LocalDateTime deletedAt) {}

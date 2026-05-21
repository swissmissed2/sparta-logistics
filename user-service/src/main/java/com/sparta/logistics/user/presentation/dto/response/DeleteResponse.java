package com.sparta.logistics.user.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeleteResponse( // 삭제 응답
        UUID userId,
        LocalDateTime deleteAt
) {

}

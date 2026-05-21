package com.sparta.logistics.user.application.command;

import lombok.Builder;

@Builder
public record LoginCommand(
        String username,
        String password
) {

}

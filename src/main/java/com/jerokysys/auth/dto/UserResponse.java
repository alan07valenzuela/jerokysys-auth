package com.jerokysys.auth.dto;

import com.jerokysys.auth.domain.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        Role role,
        Boolean active,
        LocalDateTime createdAt
) {
}

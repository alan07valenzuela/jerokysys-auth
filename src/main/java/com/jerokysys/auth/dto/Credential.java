package com.jerokysys.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record Credential(

        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "password is required")
        String password
) {
}
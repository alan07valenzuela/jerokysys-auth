package com.jerokysys.auth.dto;

import com.jerokysys.auth.domain.Role;
import jakarta.validation.constraints.Size;

/**
 * Todos los campos son opcionales: solo se actualiza lo que llega no nulo.
 */
public record UpdateUserRequest(

        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password,

        Role role,

        Boolean active
) {
}

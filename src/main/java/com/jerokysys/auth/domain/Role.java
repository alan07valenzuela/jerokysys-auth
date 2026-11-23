package com.jerokysys.auth.domain;

import java.util.List;

public enum Role {
    ADMIN,
    PROFESSOR,
    SECRETARY;

    public boolean isSelfRegistrable() {
        return this != ADMIN;
    }

    /**
     * Permisos que la UI y el resto de servicios consumen desde el claim
     * {@code permissions} del JWT. Es la unica fuente de verdad de lo que un rol
     * puede hacer; el gateway y los microservicios siguen autorizando por rol.
     */
    public List<String> permissions() {
        return switch (this) {
            case ADMIN -> List.of("users:manage", "academic:read", "academic:write",
                    "invoices:read", "invoices:write", "personas:read", "personas:write");
            case PROFESSOR -> List.of("academic:read", "academic:write");
            case SECRETARY -> List.of("academic:read", "invoices:read", "invoices:write",
                    "personas:read", "personas:write");
        };
    }
}

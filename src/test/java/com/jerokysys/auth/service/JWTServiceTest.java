package com.jerokysys.auth.service;

import com.jerokysys.auth.domain.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JWTServiceTest {

    private static final String SECRET = "nQic8KmHIa0kK7cpRVjLCVHHqMNBsZVmc3191egDTnE=";

    private final JWTService service = new JWTService(
            SECRET, "HS256", 10, "test", "", "username", "role", "permissions");

    @Test
    void includesPermissionsClaim() {
        String token = service.generate("pepe", "PROFESSOR",
                List.of("academic:read", "academic:write"));

        Claims claims = service.parse(token);

        assertThat(claims.get("permissions")).isEqualTo(List.of("academic:read", "academic:write"));
    }

    @Test
    void rolePermissionsMatchSpec() {
        assertThat(Role.ADMIN.permissions()).containsExactlyInAnyOrder(
                "users:manage", "academic:read", "academic:write",
                "invoices:read", "invoices:write", "personas:read", "personas:write");
        assertThat(Role.PROFESSOR.permissions()).containsExactlyInAnyOrder(
                "academic:read", "academic:write");
        assertThat(Role.SECRETARY.permissions()).containsExactlyInAnyOrder(
                "academic:read", "invoices:read", "invoices:write",
                "personas:read", "personas:write");
    }
}

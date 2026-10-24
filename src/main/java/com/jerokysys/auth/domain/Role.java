package com.jerokysys.auth.domain;

import lombok.Getter;

import java.util.Set;

@Getter
public enum Role {
    ADMIN("Administrador"),
    PROFESSOR("Profesor"),
    SECRETARY("Secretaria");


    private static final Set<Role> SELF_REGISTRABLE = Set.of(PROFESSOR, SECRETARY);

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public boolean isSelfRegistrable() {
        return SELF_REGISTRABLE.contains(this);
    }
}

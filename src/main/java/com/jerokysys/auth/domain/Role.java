package com.jerokysys.auth.domain;

public enum Role {
    ADMIN,
    PROFESSOR,
    SECRETARY;

    public boolean isSelfRegistrable() {
        return this != ADMIN;
    }
}

package com.jerokysys.auth.controller;

import com.jerokysys.auth.dto.AuthenticatedUser;
import com.jerokysys.auth.dto.Credential;
import com.jerokysys.auth.service.JWTService;
import com.jerokysys.auth.service.LoginAttempts;
import com.jerokysys.auth.service.UserStore;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/login")
public class AuthController {
    private final UserStore userStore;
    private final JWTService jwtService;
    private final LoginAttempts loginAttempts;

    public AuthController(UserStore userStore, JWTService jwtService, LoginAttempts loginAttempts) {
        this.userStore = userStore;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
    }

    @PostMapping
    public ResponseEntity<String> login(@Valid @RequestBody Credential credential) {
        String key = credential.username().trim().toLowerCase();

        long retryAfter = loginAttempts.retryAfterSeconds(key);
        if (retryAfter > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retryAfter))
                    .build();
        }

        Optional<AuthenticatedUser> user = userStore.authenticate(credential.username(), credential.password());
        if (user.isEmpty()) {
            loginAttempts.recordFailure(key);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        loginAttempts.recordSuccess(key);
        String token = jwtService.generate(user.get().username(), user.get().role());
        return ResponseEntity.ok(token);
    }

}

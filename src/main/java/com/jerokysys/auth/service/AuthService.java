package com.jerokysys.auth.service;

import com.jerokysys.auth.domain.User;
import com.jerokysys.auth.dto.Credential;
import com.jerokysys.auth.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final LoginAttempts loginAttempts;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JWTService jwtService,
                       LoginAttempts loginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
    }

    /** Segundos de bloqueo restantes; 0 si el usuario puede intentar. */
    public long retryAfterSeconds(String username) {
        return loginAttempts.retryAfterSeconds(normalize(username));
    }

    /**
     * Autentica y emite el token. Devuelve vacio si las credenciales no son
     * validas, y registra el intento (fallo o exito) en ambos casos.
     */
    @Transactional(readOnly = true)
    public Optional<String> login(Credential credential) {
        String key = normalize(credential.username());
        Optional<User> authenticated = userRepository.findByUsernameIgnoreCase(key)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .filter(user -> passwordEncoder.matches(credential.password(), user.getPasswordHash()));

        if (authenticated.isEmpty()) {
            loginAttempts.recordFailure(key);
            return Optional.empty();
        }

        loginAttempts.recordSuccess(key);
        return authenticated.map(user -> jwtService.generate(user.getUsername(), user.getRole().name()));
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}

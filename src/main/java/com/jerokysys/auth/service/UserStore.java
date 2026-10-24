package com.jerokysys.auth.service;

import com.jerokysys.auth.dto.AuthenticatedUser;
import com.jerokysys.auth.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserStore {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserStore(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> authenticate(String username, String password) {
        if (username == null || password == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(user -> new AuthenticatedUser(user.getUsername(), user.getRole().name()));
    }
}
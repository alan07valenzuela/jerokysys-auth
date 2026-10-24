package com.jerokysys.auth.service;

import com.jerokysys.auth.domain.User;
import com.jerokysys.auth.dto.CreateUserRequest;
import com.jerokysys.auth.dto.UpdateUserRequest;
import com.jerokysys.auth.dto.UserResponse;
import com.jerokysys.auth.repo.UserRepository;
import com.jerokysys.commons.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registro publico. El rol lo elige el cliente, pero solo puede ser
     * uno auto-registrable (PROFESSOR o SECRETARY). ADMIN se rechaza.
     */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (!request.role().isSelfRegistrable()) {
            throw new BusinessException("INVALID_ROLE",
                    "Only PROFESSOR or SECRETARY can be self-registered");
        }

        String username = request.username().trim();
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new BusinessException("CONFLICT", "Username already exists: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getOrThrow(id);

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }

        return toResponse(userRepository.save(user));
    }

    /** Borrado logico: BaseEntity.deleted lo saca de todas las consultas. */
    @Transactional
    public void delete(Long id) {
        User user = getOrThrow(id);
        user.setDeleted(true);
        user.setActive(false);
        userRepository.save(user);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ENTITY_NOT_FOUND",
                        "User not found with id: " + id));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt());
    }
}

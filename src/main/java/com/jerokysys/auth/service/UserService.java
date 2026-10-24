package com.jerokysys.auth.service;

import com.jerokysys.auth.domain.User;
import com.jerokysys.auth.dto.CreateUserRequest;
import com.jerokysys.auth.dto.UpdateUserRequest;
import com.jerokysys.auth.dto.UserResponse;
import com.jerokysys.auth.repo.UserRepository;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "username", "role", "active", "createdAt");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

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

        String username = normalizeUsername(request.username());
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
    public PageResult<UserResponse> findAll(PagedFilter filter) {
        int page = filter.getPage() == null ? 0 : Math.max(filter.getPage(), 0);
        int size = filter.getSize() == null ? DEFAULT_PAGE_SIZE
                : Math.min(Math.max(filter.getSize(), 1), MAX_PAGE_SIZE);
        String sortField = filter.getSort() != null && SORTABLE_FIELDS.contains(filter.getSort())
                ? filter.getSort() : "id";
        Sort.Direction direction = "DESC".equalsIgnoreCase(filter.getDirection())
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Page<UserResponse> users = userRepository
                .findAll(PageRequest.of(page, size, Sort.by(direction, sortField)))
                .map(UserService::toResponse);

        return PageResult.<UserResponse>builder()
                .content(users.getContent())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .page(users.getNumber())
                .size(users.getSize())
                .build();
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

        return toResponse(user);
    }

    /** Borrado logico: BaseEntity.deleted lo saca de todas las consultas. */
    @Transactional
    public void delete(Long id) {
        User user = getOrThrow(id);
        user.setDeleted(true);
        user.setActive(false);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ENTITY_NOT_FOUND",
                        "User not found with id: " + id));
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
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

package com.jerokysys.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jerokysys.auth.config.OpenApiConfig;
import com.jerokysys.auth.dto.CreateUserRequest;
import com.jerokysys.auth.dto.UpdateUserRequest;
import com.jerokysys.auth.dto.UserResponse;
import com.jerokysys.auth.service.UserService;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.Response;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Response<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        return Response.created(userService.create(request), "User created");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    public Response<PageResult<UserResponse>> findAll(@ModelAttribute PagedFilter filter) {
        return Response.success(userService.findAll(filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    public Response<UserResponse> findById(@PathVariable Long id) {
        return Response.success(userService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    public Response<UserResponse> update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateUserRequest request) {
        return Response.success(userService.update(id, request), "User updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    public Response<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Response.success("User deleted");
    }
}

package com.jerokysys.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.jerokysys.auth.security.JwtAuthenticationFilter;
import com.jerokysys.auth.security.SecurityErrors;
import com.jerokysys.auth.service.JWTService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JWTService jwtService)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Registro publico: cualquiera puede crear una cuenta
                        // PROFESSOR o SECRETARY (ADMIN se rechaza en el service).
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        // Login y documentacion.
                        .requestMatchers("/login", "/error",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Lo demas exige un ADMIN autenticado.
                        .anyRequest().hasRole("ADMIN"))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class)
                // 401 sin token / 403 sin rol, siempre en JSON y sin trazas.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                SecurityErrors.write(response, 401, "UNAUTHORIZED",
                                        "Authentication required"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                SecurityErrors.write(response, 403, "FORBIDDEN",
                                        "Access denied")))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

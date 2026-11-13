/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web;

import eu.ecodex.connector.domain.model.user.ConnectorRoleName;
import eu.ecodex.connector.infrastructure.adapter.out.auth.accesstoken.ConnectorJwtAuthenticationFilter;
import eu.ecodex.connector.infrastructure.config.property.ConnectorCorsProperties;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configures the web security for the application, defining security filters, CORs settings,
 * password encoding, and authentication management.
 *
 * <p>This configuration sets up:</p>
 * - A custom security filter chain integrating JWT authentication. - Cross-Origin Resource Sharing
 * (CORS) settings based on application properties. - Stateless session management. - Role-based
 * access control and endpoint-specific permissions. - Exception handling for unauthorized and
 * access-denied responses.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {
    private final ConnectorJwtAuthenticationFilter jwtAuthenticationFilter;

    public WebSecurityConfiguration(ConnectorJwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configures the security filter chain for the application by defining HTTP security rules.
     *
     * @param http the {@code HttpSecurity} object used to configure the security settings
     *
     * @return the {@code SecurityFilterChain} object representing the configured security filter
     *     chain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(request -> request
                // Allow internal ERROR (400, 404, 405, 415 ,500) dispatch (forward to /error)
                // without authentication. Without this, Spring Security 6+ also applies
                // authorization to that dispatch. As it has no user the request is rejected with
                // 403
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                // Specific admin-only
                .requestMatchers("/api/v1/admin/users/**")
                .hasRole(ConnectorRoleName.ADMIN.name())
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/business-domains")
                .hasRole(ConnectorRoleName.ADMIN.name())
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/processing-modes")
                .hasRole(ConnectorRoleName.ADMIN.name())
                // All authenticated users
                .requestMatchers("/api/v1/admin/**", "/api/v1/auth/me")
                .authenticated()
                .requestMatchers(
                    "/api/v1/attachments/upload",
                    "/api/v1/messages/outbound",
                    "/api/v1/messages/evidence-trigger",
                    "/api/v1/evidences/{uuid}/download",
                    "/api/v1/services",
                    "/api/v1/processing-modes/{identifier}/services",
                    "/api/v1/processing-modes/{identifier}/actions",
                    "/api/v1/processing-modes/{identifier}/parties",
                    "/api/v1/link-partners",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    // SOAP
                    "/services/backend",
                    // actuator
                    "/actuator/**",
                    // swagger ui
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**"
                )
                .permitAll()
                .anyRequest()
                .authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((req, resp, ex)
                    -> resp.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, resp, ex)
                    -> resp.sendError(HttpServletResponse.SC_FORBIDDEN)))
            .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(ConnectorCorsProperties corsProperties) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(corsProperties.allowedMethods());
        configuration.setAllowedHeaders(corsProperties.allowedHeaders());
        configuration.setAllowCredentials(corsProperties.allowCredentials());
        configuration.setMaxAge(corsProperties.maxAge());

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }
}

package com.itnews.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the IT News API.
 *
 * Highlights:
 * - CSRF disabled (stateless REST API using JWT or no session)
 * - CORS restricted to the configured allowed origin
 * - Security response headers (X-Content-Type, X-Frame, CSP)
 * - Admin endpoints restricted to localhost/loopback requests
 * - Rate limiting applied via {@link RateLimitFilter}
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String allowedOrigin;
    private final String adminSecret;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
            @Value("${app.cors.allowed-origin:http://localhost:3000}") String allowedOrigin,
            @Value("${app.admin.secret:}") String adminSecret,
            RateLimitFilter rateLimitFilter
    ) {
        this.allowedOrigin = allowedOrigin;
        this.adminSecret = adminSecret;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Add rate-limit filter before authentication
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

            // Stateless – no sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Disable CSRF – not needed for stateless REST API
            .csrf(csrf -> csrf.disable())

            // CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Admin endpoints: localhost 또는 X-Admin-Secret 헤더
                .requestMatchers("/api/admin/**").access(
                    (authentication, context) -> {
                        HttpServletRequest req = context.getRequest();
                        boolean isLocalhost = isLocalhostRequest(req);
                        String secret = req.getHeader("X-Admin-Secret");
                        boolean hasSecret = adminSecret != null && !adminSecret.isBlank()
                                && adminSecret.equals(secret);
                        return new org.springframework.security.authorization.AuthorizationDecision(
                                isLocalhost || hasSecret);
                    }
                )
                // Public endpoints
                .requestMatchers(HttpMethod.GET, "/api/news", "/api/news/search", "/api/unsubscribe").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/subscribe").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Everything else requires authentication
                .anyRequest().denyAll()
            )

            // Security headers
            .headers(headers -> headers
                .contentTypeOptions(contentType -> {}) // X-Content-Type-Options: nosniff
                .frameOptions(frame -> frame.deny())   // X-Frame-Options: DENY
                .referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: https:; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none';"
                    ))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private boolean isLocalhostRequest(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return "127.0.0.1".equals(remoteAddr)
                || "::1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || "localhost".equalsIgnoreCase(request.getRemoteHost());
    }
}

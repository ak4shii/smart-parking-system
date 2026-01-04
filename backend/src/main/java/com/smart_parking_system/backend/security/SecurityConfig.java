package com.smart_parking_system.backend.security;

import com.smart_parking_system.backend.filter.JWTTokenValidationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final List<String> publicPaths;

    @Value("${security.enabled:true}")
    private boolean securityEnabled;

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    @ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security with authentication enabled");
        return http.csrf((csrfConfig) -> csrfConfig
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Disable CSRF for API endpoints since we're using JWT tokens
                .ignoringRequestMatchers("/api/**"))
                .cors((corsConfig) -> corsConfig.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests((requests) -> {
                    requests.requestMatchers("/api/auth/**").permitAll();
                    requests.requestMatchers("/csrf-token").permitAll();
                    requests.requestMatchers("/ws/**").permitAll();
                    requests.requestMatchers("/api/admins/**").hasRole("ADMIN");
                    requests.anyRequest().authenticated();
                })
                .addFilterBefore(new JWTTokenValidationFilter(publicPaths), BasicAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling((exceptionHandling) -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Forbidden\"}");
                        }))
                .build();
    }

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    @ConditionalOnProperty(name = "security.enabled", havingValue = "false")
    public SecurityFilterChain disabledSecurityFilterChain(HttpSecurity http) throws Exception {
        log.warn("⚠️ WARNING: Security is DISABLED! All endpoints are publicly accessible. This should NEVER be used in production!");
        return http.csrf((csrfConfig) -> csrfConfig.disable())
                .cors((corsConfig) -> corsConfig.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests((requests) -> requests.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationProvider authenticationProvider) {
        var providerManager = new ProviderManager(authenticationProvider);
        return providerManager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("*"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setExposedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }
}
package com.smart_parking_system.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PublicPathConfig {

    @Bean
    public List<String> publicPaths() {
        return List.of(
                "/api/auth/**",
                "/api/csrf-token",
                "/api/entry-logs/upload-image",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/ws/**");
    }
}

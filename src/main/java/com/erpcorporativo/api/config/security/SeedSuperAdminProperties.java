package com.erpcorporativo.api.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed.super-admin")
public record SeedSuperAdminProperties(
        String name,
        String email,
        String password
) {
}

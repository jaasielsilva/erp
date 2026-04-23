package com.erpcorporativo.api.dto.auth;

public record CurrentUserResponse(
        String email,
        String role
) {
}

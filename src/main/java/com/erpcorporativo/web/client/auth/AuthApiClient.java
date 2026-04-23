package com.erpcorporativo.web.client.auth;

import com.erpcorporativo.api.dto.auth.CurrentUserResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthApiClient {

    public CurrentUserResponse getCurrentUser(String email, String role) {
        return new CurrentUserResponse(email, role);
    }
}

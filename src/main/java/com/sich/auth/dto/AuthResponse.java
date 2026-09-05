package com.sich.auth.dto;

import com.sich.common.enums.UserType;
import com.sich.user.UserEntity;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserType userType) {

    public static AuthResponse bearer(String token, long expiresInMs, UserEntity user) {
        return new AuthResponse(token, "Bearer", expiresInMs, user.getUserType());
    }
}

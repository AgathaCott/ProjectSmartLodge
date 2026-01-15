package com.example.booking.constants;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String SCOPE_CLAIM = "scope";
    public static final String USERNAME_CLAIM = "username";
    public static final String TOKEN_TYPE = "Bearer";
    public static final long TOKEN_EXPIRATION_SECONDS = 3600;
    public static final String SCOPE_ADMIN_AUTHORITY = "SCOPE_ADMIN";
}

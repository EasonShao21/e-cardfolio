package com.ecardfolio.account;

public record AuthResponse(Long id, String email, String displayName) {
    public static AuthResponse from(UserAccount user) { return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName()); }
}

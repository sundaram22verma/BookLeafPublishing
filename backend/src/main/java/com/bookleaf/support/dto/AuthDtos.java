package com.bookleaf.support.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(max = 72) String password) {}

    public record SignupRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password) {}

    public record AdminSignupRequest(
            @NotBlank @Size(max = 64) String inviteToken,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password) {}

    public record UserInfo(Long id, String name, String email, String role) {}

    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserInfo user) {}
}

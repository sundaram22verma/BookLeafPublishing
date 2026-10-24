package com.bookleaf.support.service;

import com.bookleaf.support.dto.AdminDtos.AdminInviteResponse;
import com.bookleaf.support.dto.AdminDtos.CreateAdminInviteRequest;
import com.bookleaf.support.entity.AdminInvite;
import com.bookleaf.support.exception.ResourceNotFoundException;
import com.bookleaf.support.repository.AdminInviteRepository;
import com.bookleaf.support.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminInviteService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRATION_HOURS = 48;

    private final AdminInviteRepository invites;
    private final UserRepository users;

    public AdminInviteResponse create(Long adminId, CreateAdminInviteRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("An account with this email already exists");
        }

        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(EXPIRATION_HOURS);
        invites.save(AdminInvite.builder()
                .tokenHash(hashToken(token))
                .email(email)
                .createdByAdmin(users.getReferenceById(adminId))
                .expiresAt(expiresAt)
                .build());
        return new AdminInviteResponse(token, expiresAt);
    }

    public void consume(String token, String email) {
        LocalDateTime now = LocalDateTime.now();
        int consumed = invites.consume(hashToken(token), normalizeEmail(email), now);
        if (consumed != 1) {
            throw new IllegalArgumentException("This admin invitation is invalid, expired, already used, or for a different email.");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

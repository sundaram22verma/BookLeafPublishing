package com.bookleaf.support.service;

import com.bookleaf.support.config.JwtService;
import com.bookleaf.support.dto.AuthDtos.AdminSignupRequest;
import com.bookleaf.support.dto.AuthDtos.SignupRequest;
import com.bookleaf.support.entity.Role;
import com.bookleaf.support.entity.User;
import com.bookleaf.support.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceAdminSignupTest {

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtService jwt;

    @Mock
    private LoginAttemptService loginAttempts;

    @Mock
    private AdminInviteService adminInvites;

    @InjectMocks
    private AuthService authService;

    @Test
    void registersAnAdminOnlyAfterConsumingAnInvitation() {
        when(users.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(encoder.encode("password-123")).thenReturn("encoded-password");
        when(users.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(jwt.generateToken(any(User.class))).thenReturn("signed-token");
        when(jwt.expiresInSeconds()).thenReturn(3600L);

        var response = authService.signupAdmin(new AdminSignupRequest(
                "one-time-token", "New Admin", " Admin@Example.com ", "password-123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        verify(adminInvites).consume("one-time-token", "admin@example.com");
        assertEquals(Role.ADMIN, userCaptor.getValue().getRole());
        assertEquals("encoded-password", userCaptor.getValue().getPasswordHash());
        assertEquals("ADMIN", response.user().role());
        assertEquals("signed-token", response.token());
    }

    @Test
    void doesNotCreateAccountWhenInvitationCannotBeConsumed() {
        doThrow(new IllegalArgumentException("Invalid invitation"))
                .when(adminInvites).consume("invalid-token", "admin@example.com");

        assertThrows(IllegalArgumentException.class, () -> authService.signupAdmin(
                new AdminSignupRequest("invalid-token", "New Admin", "admin@example.com", "password-123")));

        verify(users, never()).save(any(User.class));
    }

    @Test
    void publicSignupAlwaysCreatesAnAuthor() {
        when(users.existsByEmailIgnoreCase("author@example.com")).thenReturn(false);
        when(encoder.encode("password-123")).thenReturn("encoded-password");
        when(users.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            user.setId(43L);
            return user;
        });
        when(jwt.generateToken(any(User.class))).thenReturn("signed-token");
        when(jwt.expiresInSeconds()).thenReturn(3600L);

        var response = authService.signup(new SignupRequest(
                "New Author", " Author@Example.com ", "password-123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertEquals(Role.AUTHOR, userCaptor.getValue().getRole());
        assertEquals("AUTHOR", response.user().role());
        verifyNoInteractions(adminInvites);
    }
}

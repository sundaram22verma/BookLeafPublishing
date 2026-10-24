package com.bookleaf.support.service;

import com.bookleaf.support.dto.AdminDtos.CreateAdminInviteRequest;
import com.bookleaf.support.entity.AdminInvite;
import com.bookleaf.support.entity.User;
import com.bookleaf.support.repository.AdminInviteRepository;
import com.bookleaf.support.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInviteServiceTest {

    @Mock
    private AdminInviteRepository invites;

    @Mock
    private UserRepository users;

    @InjectMocks
    private AdminInviteService service;

    @Test
    void createsEmailBoundInviteAndStoresOnlyTokenHash() throws Exception {
        when(users.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(users.getReferenceById(7L)).thenReturn(User.builder().id(7L).build());
        when(invites.save(any(AdminInvite.class))).thenAnswer(call -> call.getArgument(0));
        LocalDateTime before = LocalDateTime.now().plusHours(48);

        var response = service.create(7L, new CreateAdminInviteRequest(" Admin@Example.com "));

        ArgumentCaptor<AdminInvite> inviteCaptor = ArgumentCaptor.forClass(AdminInvite.class);
        verify(invites).save(inviteCaptor.capture());
        AdminInvite savedInvite = inviteCaptor.getValue();
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(response.token().getBytes(StandardCharsets.UTF_8)));

        assertEquals("admin@example.com", savedInvite.getEmail());
        assertEquals(expectedHash, savedInvite.getTokenHash());
        assertNotEquals(response.token(), savedInvite.getTokenHash());
        assertEquals(savedInvite.getExpiresAt(), response.expiresAt());
        assertTrue(response.expiresAt().isAfter(before));
        assertEquals(7L, savedInvite.getCreatedByAdmin().getId());
    }

    @Test
    void refusesInvitesForExistingAccounts() {
        when(users.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.create(7L, new CreateAdminInviteRequest("admin@example.com")));

        verifyNoInteractions(invites);
    }

    @Test
    void atomicallyConsumesOnlyTheMatchingEmailInvite() {
        when(invites.consume(anyString(), eq("admin@example.com"), any(LocalDateTime.class)))
                .thenReturn(1);

        assertDoesNotThrow(() -> service.consume("one-time-token", " Admin@Example.com "));

        verify(invites).consume(anyString(), eq("admin@example.com"), any(LocalDateTime.class));
    }

    @Test
    void rejectsExpiredUsedOrEmailMismatchedInvites() {
        when(invites.consume(anyString(), eq("admin@example.com"), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> service.consume("one-time-token", "admin@example.com"));
    }
}

package com.bookleaf.support.controller;

import com.bookleaf.support.config.UserPrincipal;
import com.bookleaf.support.dto.AdminDtos.AdminInviteResponse;
import com.bookleaf.support.dto.AdminDtos.CreateAdminInviteRequest;
import com.bookleaf.support.service.AdminInviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/invites")
@RequiredArgsConstructor
public class AdminInviteController {

    private final AdminInviteService invites;

    @Operation(summary = "Create a one-time, email-bound admin invitation valid for 48 hours")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminInviteResponse create(@AuthenticationPrincipal UserPrincipal admin,
                                      @Valid @RequestBody CreateAdminInviteRequest request) {
        return invites.create(admin.getId(), request);
    }
}

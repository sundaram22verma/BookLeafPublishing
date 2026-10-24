//package com.bookleaf.support.controller;
//
//import com.bookleaf.support.config.UserPrincipal;
//import com.bookleaf.support.dto.AuthDtos.*;
//import com.bookleaf.support.service.AuthService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//@Tag(name = "Auth")
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final AuthService authService;
//
//    @Operation(summary = "Email/password login; returns a JWT")
//    @PostMapping("/login")
//    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
//        return authService.login(request);
//    }
//
//    @Operation(summary = "Create an author account")
//    @PostMapping("/signup")
//    @ResponseStatus(HttpStatus.CREATED)
//    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
//        return authService.signup(request);
//    }
//
//    @Operation(summary = "Current user", security = @SecurityRequirement(name = "bearerAuth"))
//    @GetMapping("/me")
//    public UserInfo me(@AuthenticationPrincipal UserPrincipal principal) {
//        return authService.me(principal);
//    }
//}



package com.bookleaf.support.controller;

import com.bookleaf.support.config.UserPrincipal;
import com.bookleaf.support.dto.AuthDtos.*;
import com.bookleaf.support.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Email/password login; returns a JWT")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    @Operation(summary = "Create an author account")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "Create an admin account using an email-bound invitation")
    @PostMapping("/admin-signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signupAdmin(@Valid @RequestBody AdminSignupRequest request) {
        return authService.signupAdmin(request);
    }

    @Operation(summary = "Current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal);
    }
}

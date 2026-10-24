//package com.bookleaf.support.service;
//
//import com.bookleaf.support.config.JwtService;
//import com.bookleaf.support.config.UserPrincipal;
//import com.bookleaf.support.dto.AuthDtos.*;
//import com.bookleaf.support.entity.Role;
//import com.bookleaf.support.entity.User;
//import com.bookleaf.support.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class AuthService {
//
//    private final UserRepository users;
//    private final PasswordEncoder encoder;
//    private final JwtService jwt;
//
//    @Transactional(readOnly = true)
//    public AuthResponse login(LoginRequest req) {
//        // Same error for unknown email and wrong password: no account enumeration.
//        User user = users.findByEmailIgnoreCase(req.email().trim())
//                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
//        if (!encoder.matches(req.password(), user.getPasswordHash())) {
//            throw new BadCredentialsException("Invalid email or password");
//        }
//        return toResponse(user);
//    }
//
//    /** Self-service signup always creates an AUTHOR; admins are provisioned out-of-band. */
//    @Transactional
//    public AuthResponse signup(SignupRequest req) {
//        String email = req.email().trim().toLowerCase();
//        if (users.existsByEmailIgnoreCase(email)) {
//            throw new IllegalStateException("An account with this email already exists");
//        }
//        User user = users.save(User.builder().name(req.name().trim()).email(email)
//                .passwordHash(encoder.encode(req.password())).role(Role.AUTHOR).build());
//        return toResponse(user);
//    }
//
//    public UserInfo me(UserPrincipal principal) {
//        return new UserInfo(principal.getId(), principal.getName(), principal.getEmail(), principal.getRole().name());
//    }
//
//    private AuthResponse toResponse(User u) {
//        return new AuthResponse(jwt.generateToken(u), "Bearer", jwt.expiresInSeconds(),
//                new UserInfo(u.getId(), u.getName(), u.getEmail(), u.getRole().name()));
//    }
//}


package com.bookleaf.support.service;

import com.bookleaf.support.config.JwtService;
import com.bookleaf.support.config.UserPrincipal;
import com.bookleaf.support.dto.AuthDtos.*;
import com.bookleaf.support.entity.Role;
import com.bookleaf.support.entity.User;
import com.bookleaf.support.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService loginAttempts;
    private final AdminInviteService adminInvites;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req, String clientIp) {
        loginAttempts.assertAllowed(clientIp, req.email()); // 429 after repeated failures
        // Same error for unknown email and wrong password: no account enumeration.
        User user = users.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            loginAttempts.recordFailure(clientIp, req.email());
            throw new BadCredentialsException("Invalid email or password");
        }
        loginAttempts.recordSuccess(clientIp, req.email());
        return toResponse(user);
    }

    /** Self-service signup always creates an AUTHOR; admins are provisioned out-of-band. */
    @Transactional
    public AuthResponse signup(SignupRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("An account with this email already exists");
        }
        User user = users.save(User.builder().name(req.name().trim()).email(email)
                .passwordHash(encoder.encode(req.password())).role(Role.AUTHOR).build());
        return toResponse(user);
    }

    @Transactional
    public AuthResponse signupAdmin(AdminSignupRequest req) {
        String email = req.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("An account with this email already exists");
        }
        adminInvites.consume(req.inviteToken(), email);
        User user = users.save(User.builder().name(req.name().trim()).email(email)
                .passwordHash(encoder.encode(req.password())).role(Role.ADMIN).build());
        return toResponse(user);
    }

    public UserInfo me(UserPrincipal principal) {
        return new UserInfo(principal.getId(), principal.getName(), principal.getEmail(), principal.getRole().name());
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generateToken(u), "Bearer", jwt.expiresInSeconds(),
                new UserInfo(u.getId(), u.getName(), u.getEmail(), u.getRole().name()));
    }
}

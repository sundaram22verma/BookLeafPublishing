//package com.bookleaf.support.config;
//
//import com.bookleaf.support.exception.ErrorResponse;
//import com.bookleaf.support.repository.UserRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.MediaType;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Value("${bookleaf.cors.allowed-origins}")
//    private String allowedOrigins;
//
//    @Bean
//    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UserDetailsService userDetailsService,
//                                    ObjectMapper mapper) throws Exception {
//        http
//                .csrf(AbstractHttpConfigurer::disable) // stateless bearer-token API, no cookies
//                .cors(Customizer.withDefaults())
//                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()
//                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
//                        .requestMatchers("/api/author/**").hasRole("AUTHOR")
//                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
//                        .anyRequest().authenticated())
//                .exceptionHandling(ex -> ex
//                        .authenticationEntryPoint((req, res, e) ->
//                                writeError(mapper, req, res, 401, "Unauthorized", "Authentication required or token invalid/expired"))
//                        .accessDeniedHandler((req, res, e) ->
//                                writeError(mapper, req, res, 403, "Forbidden", "You do not have permission to access this resource")))
//                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService), UsernamePasswordAuthenticationFilter.class);
//        return http.build();
//    }
//
//    @Bean
//    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
//
//    /** Also stops Spring Boot from generating its default in-memory user. */
//    @Bean
//    UserDetailsService userDetailsService(UserRepository users) {
//        return email -> users.findByEmailIgnoreCase(email)
//                .map(UserPrincipal::new)
//                .orElseThrow(() -> new UsernameNotFoundException("No user " + email));
//    }
//
//    @Bean
//    CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration cfg = new CorsConfiguration();
//        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
//        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
//        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", cfg);
//        return source;
//    }
//
//    private static void writeError(ObjectMapper mapper, HttpServletRequest req, HttpServletResponse res,
//                                   int status, String error, String message) throws IOException {
//        res.setStatus(status);
//        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
//        mapper.writeValue(res.getOutputStream(), ErrorResponse.of(status, error, message, req.getRequestURI(), null));
//    }
//}


//package com.bookleaf.support.config;
//
//import com.bookleaf.support.exception.ErrorResponse;
//import com.bookleaf.support.repository.UserRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.DispatcherType;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.MediaType;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Value("${bookleaf.cors.allowed-origins}")
//    private String allowedOrigins;
//
//    @Bean
//    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UserDetailsService userDetailsService,
//                                    ObjectMapper mapper) throws Exception {
//        http
//                .csrf(AbstractHttpConfigurer::disable) // stateless bearer-token API, no cookies
//                .cors(Customizer.withDefaults())
//                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//                        // SSE/async completion re-dispatches the request without the stateless security context;
//                        // the original request was already authorised by the rules below.
//                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
//                        .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()
//                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
//                        .requestMatchers("/api/author/**").hasRole("AUTHOR")
//                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
//                        .anyRequest().authenticated())
//                .exceptionHandling(ex -> ex
//                        .authenticationEntryPoint((req, res, e) ->
//                                writeError(mapper, req, res, 401, "Unauthorized", "Authentication required or token invalid/expired"))
//                        .accessDeniedHandler((req, res, e) ->
//                                writeError(mapper, req, res, 403, "Forbidden", "You do not have permission to access this resource")))
//                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService), UsernamePasswordAuthenticationFilter.class);
//        return http.build();
//    }
//
//    @Bean
//    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
//
//    /** Also stops Spring Boot from generating its default in-memory user. */
//    @Bean
//    UserDetailsService userDetailsService(UserRepository users) {
//        return email -> users.findByEmailIgnoreCase(email)
//                .map(UserPrincipal::new)
//                .orElseThrow(() -> new UsernameNotFoundException("No user " + email));
//    }
//
//    @Bean
//    CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration cfg = new CorsConfiguration();
//        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
//        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
//        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", cfg);
//        return source;
//    }
//
//    private static void writeError(ObjectMapper mapper, HttpServletRequest req, HttpServletResponse res,
//                                   int status, String error, String message) throws IOException {
//        res.setStatus(status);
//        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
//        mapper.writeValue(res.getOutputStream(), ErrorResponse.of(status, error, message, req.getRequestURI(), null));
//    }
//}


package com.bookleaf.support.config;

import com.bookleaf.support.exception.ErrorResponse;
import com.bookleaf.support.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${bookleaf.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UserDetailsService userDetailsService,
                                    ObjectMapper mapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // stateless bearer-token API, no cookies
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // SSE/async completion re-dispatches the request without the stateless security context;
                        // the original request was already authorised by the rules below.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/signup", "/api/auth/admin-signup").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/author/**").hasRole("AUTHOR")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(mapper, req, res, 401, "Unauthorized", "Authentication required or token invalid/expired"))
                        .accessDeniedHandler((req, res, e) ->
                                writeError(mapper, req, res, 403, "Forbidden", "You do not have permission to access this resource")))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    /** Also stops Spring Boot from generating its default in-memory user. */
    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmailIgnoreCase(email)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user " + email));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private static void writeError(ObjectMapper mapper, HttpServletRequest req, HttpServletResponse res,
                                   int status, String error, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), ErrorResponse.of(status, error, message, req.getRequestURI(), null));
    }
}
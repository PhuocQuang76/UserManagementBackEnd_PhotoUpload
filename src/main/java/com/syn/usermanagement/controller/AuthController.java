package com.syn.usermanagement.controller;

import com.syn.usermanagement.dto.LoginRequest;
import com.syn.usermanagement.dto.LoginResponse;
import com.syn.usermanagement.dto.RegisterRequest;
import com.syn.usermanagement.entity.User;
import com.syn.usermanagement.repository.UserRepository;
import com.syn.usermanagement.security.CustomUserDetailsService;
import com.syn.usermanagement.security.JwtUtils;
import com.syn.usermanagement.service.TokenBlacklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

// Add this:
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;

    private String serverName;

    @PostConstruct
    public void logServerInfo() {
        try {
            serverName = InetAddress.getLocalHost().getHostName();
            log.info("=================================");
            log.info("AuthController initialized on: {}", serverName);
            log.info("Server IP: {}", InetAddress.getLocalHost().getHostAddress());
            log.info("=================================");
        } catch (Exception e) {
            log.error("Could not get hostname", e);
            serverName = "unknown";
        }
    }

    /**
     * Login endpoint
     * http://localhost:8080/api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request handled by: {} for email: {}", serverName, loginRequest.getEmail());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Get user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Get full user entity
            User user = userDetailsService.loadUserEntityByEmail(loginRequest.getEmail());

            // Generate JWT token
            String token = jwtUtils.generateToken(userDetails);

            // Return response with token and user info
            LoginResponse response = new LoginResponse(
                    token,
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().name()
            );

            log.info("Login successful for user: {} on server: {}", loginRequest.getEmail(), serverName);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.error("Login failed for email: {} on server: {}", loginRequest.getEmail(), serverName);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Register endpoint
     * http://localhost:8080/api/auth/register
     * {
     *     "name": "Test User",
     *     "email": "test@example.com",
     *     "password": "password123"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration request handled by: {} for email: {}", serverName, registerRequest.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Email already exists: {} on server: {}", registerRequest.getEmail(), serverName);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Email already exists");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // Create new user
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword())); // Encrypt password
        user.setRole(User.Role.USER); // Default role

        // Save user
        User savedUser = userRepository.save(user);

        // Generate token for immediate login
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtils.generateToken(userDetails);

        // Return response
        LoginResponse response = new LoginResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        log.info("Registration successful for user: {} on server: {}", registerRequest.getEmail(), serverName);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Validate token endpoint (optional - useful for frontend)
     * http://localhost:8080/api/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        log.info("Token validation request handled by: {}", serverName);

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
            }

            String token = authHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtUtils.validateToken(token, userDetails)) {
                User user = userDetailsService.loadUserEntityByEmail(email);
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "role", user.getRole().name()
                ));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));

        } catch (Exception e) {
            log.error("Token validation failed on server: {}", serverName, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        log.info("Get current user request handled by: {}", serverName);

        try {
            String token = authHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            User user = userDetailsService.loadUserEntityByEmail(email);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("name", user.getName());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole().name());
            userInfo.put("photoUrl", user.getPhotoUrl());

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            log.error("Get current user failed on server: {}", serverName, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
        }
    }

    /**
     * Logout endpoint - Invalidates token
     * http://localhost:8080/api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        log.info("Logout request handled by: {}", serverName);

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid authorization header"));
            }

            String token = authHeader.substring(7);

            // Add token to blacklist
            tokenBlacklistService.blacklistToken(token);

            log.info("Logout successful on server: {}", serverName);
            return ResponseEntity.ok(Map.of(
                    "message", "Logged out successfully",
                    "success", true
            ));

        } catch (Exception e) {
            log.error("Logout failed on server: {}", serverName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Logout failed: " + e.getMessage()));
        }
    }
}




//package com.syn.usermanagement.controller;
//import com.syn.usermanagement.dto.LoginRequest;
//import com.syn.usermanagement.dto.LoginResponse;
//import com.syn.usermanagement.dto.RegisterRequest;
//import com.syn.usermanagement.entity.User;
//import com.syn.usermanagement.repository.UserRepository;
//import com.syn.usermanagement.security.CustomUserDetailsService;
//import com.syn.usermanagement.security.JwtUtils;
//import com.syn.usermanagement.service.TokenBlacklistService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:4200")
////@CrossOrigin(origins = "http://localhost:4200")
//public class AuthController {
//
//    private final AuthenticationManager authenticationManager;
//    private final CustomUserDetailsService userDetailsService;
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtUtils jwtUtils;
//    private final TokenBlacklistService tokenBlacklistService;
//
//
//    /**
//     * Login endpoint
//     * http://localhost:8080/api/auth/login
//     */
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
//        try {
//            // Authenticate user
//            Authentication authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            loginRequest.getEmail(),
//                            loginRequest.getPassword()
//                    )
//            );
//
//            // Get user details
//            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
//
//            // Get full user entity
//            User user = userDetailsService.loadUserEntityByEmail(loginRequest.getEmail());
//
//            // Generate JWT token
//            String token = jwtUtils.generateToken(userDetails);
//
//            // Return response with token and user info
//            LoginResponse response = new LoginResponse(
//                    token,
//                    user.getId(),
//                    user.getName(),
//                    user.getEmail(),
//                    user.getRole().name()
//            );
//
//            return ResponseEntity.ok(response);
//
//        } catch (BadCredentialsException e) {
//            Map<String, String> error = new HashMap<>();
//            error.put("message", "Invalid email or password");
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
//        }
//    }
//
//    /**
//     * Register endpoint
//     * http://localhost:8080/api/auth/register
//     * {
//     *     "name": "Test User",
//     *     "email": "test@example.com",
//     *     "password": "password123"
//     * }
//     */
//    @PostMapping("/register")
//    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
//        // Check if email already exists
//        if (userRepository.existsByEmail(registerRequest.getEmail())) {
//            Map<String, String> error = new HashMap<>();
//            error.put("message", "Email already exists");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
//        }
//
//        // Create new user
//        User user = new User();
//        user.setName(registerRequest.getName());
//        user.setEmail(registerRequest.getEmail());
//        user.setPassword(passwordEncoder.encode(registerRequest.getPassword())); // Encrypt password
//        user.setRole(User.Role.USER); // Default role
//
//        // Save user
//        User savedUser = userRepository.save(user);
//
//        // Generate token for immediate login
//        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
//        String token = jwtUtils.generateToken(userDetails);
//
//        // Return response
//        LoginResponse response = new LoginResponse(
//                token,
//                savedUser.getId(),
//                savedUser.getName(),
//                savedUser.getEmail(),
//                savedUser.getRole().name()
//        );
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    /**
//     * Validate token endpoint (optional - useful for frontend)
//     * http://localhost:8080/api/auth/validate
//     */
//    @GetMapping("/validate")
//    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
//        try {
//            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
//            }
//
//            String token = authHeader.substring(7);
//            String email = jwtUtils.extractUsername(token);
//            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
//
//            if (jwtUtils.validateToken(token, userDetails)) {
//                User user = userDetailsService.loadUserEntityByEmail(email);
//                return ResponseEntity.ok(Map.of(
//                        "valid", true,
//                        "id", user.getId(),
//                        "name", user.getName(),
//                        "email", user.getEmail(),
//                        "role", user.getRole().name()
//                ));
//            }
//
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
//        }
//    }
//
//    /**
//     * Get current user info
//     */
//    @GetMapping("/me")
//    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
//        try {
//            String token = authHeader.substring(7);
//            String email = jwtUtils.extractUsername(token);
//            User user = userDetailsService.loadUserEntityByEmail(email);
//
//            Map<String, Object> userInfo = new HashMap<>();
//            userInfo.put("id", user.getId());
//            userInfo.put("name", user.getName());
//            userInfo.put("email", user.getEmail());
//            userInfo.put("role", user.getRole().name());
//            userInfo.put("photoUrl", user.getPhotoUrl());
//
//            return ResponseEntity.ok(userInfo);
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
//        }
//    }
//
//    /**
//     * Logout endpoint - Invalidates the token
//     * http://localhost:8080/api/auth/logout
//     */
//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
//        try {
//            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                return ResponseEntity.badRequest()
//                        .body(Map.of("message", "Invalid authorization header"));
//            }
//
//            String token = authHeader.substring(7);
//
//            // Add token to blacklist
//            tokenBlacklistService.blacklistToken(token);
//
//            return ResponseEntity.ok(Map.of(
//                    "message", "Logged out successfully",
//                    "success", true
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("message", "Logout failed: " + e.getMessage()));
//        }
//    }
//}
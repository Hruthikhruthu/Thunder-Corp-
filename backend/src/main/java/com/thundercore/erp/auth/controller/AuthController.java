package com.thundercore.erp.auth.controller;

import com.thundercore.erp.auth.dto.AuthRequest;
import com.thundercore.erp.auth.dto.AuthResponse;
import com.thundercore.erp.auth.entity.User;
import com.thundercore.erp.auth.repository.UserRepository;
import com.thundercore.erp.auth.security.JwtUtil;
import com.thundercore.erp.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
/**
 * AuthController exposes authentication and user provisioning endpoints.
 *
 * <p>Login is public and returns a signed JWT for the React client. User
 * registration is restricted to SUPER_ADMIN users so operational accounts are
 * created through an authenticated administrative workflow.</p>
 */
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    /**
     * Authenticates credentials and issues a stateless JWT for API access.
     * Works for ALL roles: SUPER_ADMIN, MANAGER, STAFF.
     *
     * @param authRequest validated email/password payload
     * @return JWT plus the user identity needed by the frontend session state
     */
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getEmail(),
                            authRequest.getPassword()
                    )
            );
        } catch (DisabledException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Account is disabled. Please contact your administrator."));
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", authRequest.getEmail());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Invalid email or password"));
        } catch (Exception e) {
            log.error("Authentication error for {}: {}", authRequest.getEmail(), e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication failed: " + e.getMessage()));
        }

        // Authentication succeeded — load user details and generate JWT
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());
        final String jwt = jwtUtil.generateToken(userDetails);
        User user = userRepository.findByEmail(authRequest.getEmail()).orElseThrow();

        log.info("Successful login: {} (role={})", user.getEmail(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(
                jwt,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName()
        ));
    }

    @PostMapping("/refresh")
    /**
     * Issues a fresh JWT for an already-authenticated user.
     * Used by the frontend to extend sessions without re-entering credentials.
     */
    public ResponseEntity<?> refreshToken(Authentication authentication) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(authentication.getName());
        String jwt = jwtUtil.generateToken(userDetails);
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(new AuthResponse(
                jwt,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName()
        ));
    }

    @PostMapping("/logout")
    /** Stateless logout — client discards the JWT. */
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    /**
     * Creates an active user account with a BCrypt-hashed password.
     * Only SUPER_ADMIN can create new users.
     *
     * @param user user profile and raw password supplied by an administrator
     * @return standardized success/error response
     */
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is already in use!"));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("STAFF");
        } else {
            user.setRole(user.getRole().toUpperCase());
        }
        userRepository.save(user);
        log.info("New user registered: {} (role={})", user.getEmail(), user.getRole());
        return ResponseEntity.ok(ApiResponse.success("User registered successfully!", null));
    }
}

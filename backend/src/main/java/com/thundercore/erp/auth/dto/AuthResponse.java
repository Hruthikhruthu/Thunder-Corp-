package com.thundercore.erp.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * AuthResponse is the frontend session contract returned after login.
 *
 * <p>The token is used on subsequent Axios requests and STOMP CONNECT frames;
 * the profile fields drive navigation labels and role-aware UI behavior.</p>
 */
public class AuthResponse {
    private String token;
    private Long id;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
}

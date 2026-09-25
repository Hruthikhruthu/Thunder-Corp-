package com.thundercore.erp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * AuthRequest is the login payload accepted by /api/auth/login.
 *
 * <p>Validation runs before authentication so malformed requests fail with a
 * clear 400 response instead of reaching the credential provider.</p>
 */
public class AuthRequest {
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

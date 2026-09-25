package com.thundercore.erp.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
/**
 * JwtUtil owns token generation, parsing, validation, and signing-key handling.
 *
 * <p>The same utility is used by REST authentication and the STOMP CONNECT
 * interceptor so browser API calls and WebSocket sessions share one identity
 * model.</p>
 */
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Reads the subject claim, which is the authenticated user's email address.
     *
     * @param token compact JWT string
     * @return email stored as token subject
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Returns the token expiration timestamp for validation and diagnostics. */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Resolves any supported claim after verifying token integrity.
     *
     * @param token compact JWT string
     * @param claimsResolver function that extracts a specific claim
     * @param <T> resolved claim type
     * @return extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Creates a signed JWT for the authenticated user.
     *
     * @param userDetails Spring Security principal from the user repository
     * @return compact token sent to the React client
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Confirms that the token belongs to the supplied user and has not expired.
     *
     * @param token compact JWT string
     * @param userDetails user loaded from persistence
     * @return true when subject and expiration are valid
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = decodeSecret(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Accepts either a base64 secret or a plain configured secret.
     *
     * <p>Plain-text secrets are converted to a SHA-256 HMAC key so local demos
     * and production deployments can both provide valid signing material.</p>
     */
    private byte[] decodeSecret(String configuredSecret) {
        try {
            byte[] decoded = Decoders.BASE64.decode(configuredSecret);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to a deterministic SHA-256 key for plain-text secrets.
        }

        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(configuredSecret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available for JWT key generation", e);
        }
    }
}

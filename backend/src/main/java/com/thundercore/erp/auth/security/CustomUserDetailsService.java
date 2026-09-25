package com.thundercore.erp.auth.security;

import com.thundercore.erp.auth.entity.User;
import com.thundercore.erp.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
/**
 * CustomUserDetailsService adapts ThunderCore users to Spring Security.
 *
 * <p>The email is treated as username, active controls account enablement, and
 * the persisted role is converted into the ROLE_* authority format expected by
 * @PreAuthorize checks.</p>
 */
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    /**
     * Loads a user identity by email for login, JWT validation, and STOMP
     * connection authentication.
     *
     * @param username email address supplied to Spring Security
     * @return Spring Security user details with role authority
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getActive(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}

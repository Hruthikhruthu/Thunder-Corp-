package com.thundercore.erp;

import com.thundercore.erp.auth.entity.User;
import com.thundercore.erp.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * DataInitializer seeds the three default demo accounts on first startup.
 *
 * <p>Existing users are left untouched so passwords changed at runtime are not
 * overwritten on every restart. Only missing accounts are created.</p>
 */
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfMissing("admin@thundercore.com",   "Admin@123",   "Super",      "Admin",   "SUPER_ADMIN");
        createUserIfMissing("manager@thundercore.com", "Manager@123", "Operations", "Manager", "MANAGER");
        createUserIfMissing("staff@thundercore.com",   "Staff@123",   "Frontline",  "Staff",   "STAFF");
    }

    /**
     * Creates a user only when no record with that email exists.
     * Existing users are never modified so runtime password changes survive restarts.
     */
    private void createUserIfMissing(String email, String rawPassword,
                                     String firstName, String lastName, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Demo user already exists, skipping: {}", email);
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
        log.info("Demo user created: {} (role={})", email, role);
    }
}

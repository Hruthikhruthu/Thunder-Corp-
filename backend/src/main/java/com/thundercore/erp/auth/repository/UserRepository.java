package com.thundercore.erp.auth.repository;

import com.thundercore.erp.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
/**
 * UserRepository centralizes persistence queries for authentication, RBAC, and
 * role-targeted notifications.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /** Finds the login identity by its unique email address. */
    Optional<User> findByEmail(String email);

    /** Returns active recipients whose roles match notification targeting. */
    List<User> findByRoleInAndActiveTrue(Collection<String> roles);
}

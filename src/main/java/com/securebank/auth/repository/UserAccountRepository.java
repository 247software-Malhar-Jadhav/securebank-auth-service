package com.securebank.auth.repository;

import com.securebank.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserAccount}.
 *
 * <p>Pattern: <b>Repository</b> — abstracts persistence behind a collection-like
 * interface. Spring generates the implementation from the method names at runtime.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}

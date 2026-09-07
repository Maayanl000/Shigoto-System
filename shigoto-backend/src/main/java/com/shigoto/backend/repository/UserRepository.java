package com.shigoto.backend.repository;

import com.shigoto.backend.entity.User;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Declares application-specific persistence queries for user data.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user account by its normalized email address.
     * @param email the email address
     * @return the user registered with the email, if present
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether an account is already registered with the supplied email.
     * @param email the email address
     * @return {@code true} when the email belongs to an existing user; otherwise {@code false}
     */
    boolean existsByEmail(String email);

    /**
     * Lists company users with the requested role in display-name order.
     * @param role the required user role
     * @param company the company that scopes the operation
     * @return matching company users ordered by first and last name
     */
    List<User> findByRoleAndCompanyOrderByFirstNameAscLastNameAsc(Role role, Company company);

    /**
     * Finds a user by identifier within a company boundary.
     * @param id the entity identifier
     * @param company the company that scopes the operation
     * @return the company member, if present
     */
    Optional<User> findByIdAndCompany(Long id, Company company);
}

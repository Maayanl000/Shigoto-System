package com.shigoto.backend.repository;

import com.shigoto.backend.entity.User;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // שולף את המשתמש לפי אימייל
    Optional<User> findByEmail(String email);

    // בודק אם קיים משתמש עם האימייל הזה (מחזיר true או false)
    boolean existsByEmail(String email);

    List<User> findByRoleAndCompanyOrderByFirstNameAscLastNameAsc(Role role, Company company);

    Optional<User> findByIdAndCompany(Long id, Company company);
}

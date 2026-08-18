package com.shigoto.backend.repository;

import com.shigoto.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // שולף את המשתמש לפי אימייל
    Optional<User> findByEmail(String email);

    // בודק אם קיים משתמש עם האימייל הזה (מחזיר true או false)
    boolean existsByEmail(String email);
}

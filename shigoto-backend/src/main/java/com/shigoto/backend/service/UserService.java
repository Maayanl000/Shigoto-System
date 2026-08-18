package com.shigoto.backend.service;

import com.shigoto.backend.entity.User;
import com.shigoto.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

//receives a user and signs it up in the system and returns it to the controller
    public User registerUser(User user) {
        // 1. חוק עסקי: בדיקה האם האימייל כבר קיים במערכת
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists!");
        }

        // 2. לוגיקה עסקית: כאן בעתיד נוסיף קוד שיצפין את הסיסמה לפני השמירה

        // 3. שמירת המשתמש במסד הנתונים דרך שכבת הנתונים
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
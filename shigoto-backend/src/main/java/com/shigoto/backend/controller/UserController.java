package com.shigoto.backend.controller;

import com.shigoto.backend.entity.User;
import com.shigoto.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        // קוראים ל-Service כדי שיבצע את הלוגיקה וישמור במסד הנתונים
        User savedUser = userService.registerUser(user);

        // מחזירים תשובת HTTP סטנדרטית של 200 (OK) יחד עם המשתמש השמור
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
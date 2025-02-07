package com.manu.springboot_backend.controller;

import com.manu.springboot_backend.model.User;
import com.manu.springboot_backend.repository.UserRepository;
import com.manu.springboot_backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already in use"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User registered successfully", "user", savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isEmpty() || !passwordEncoder.matches(user.getPassword(), existingUser.get().getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email or password"));
        }

        String token = jwtUtil.generateToken(existingUser.get());
        return ResponseEntity.ok(Map.of("token", token, "user", existingUser.get()));
    }
}

package com.ems.employee_management.controller;

import com.ems.employee_management.entity.User;
import com.ems.employee_management.repository.UserRepository;
import com.ems.employee_management.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().isBlank() || user.getPassword() == null || user.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username is already registered");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("employeeId", user.getEmployee() == null ? null : user.getEmployee().getId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('LEAVE_APPROVER')")
    public ResponseEntity<java.util.List<Map<String, Object>>> users() {
        var result = userRepository.findAll().stream().map(user -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("role", user.getRole());
            item.put("employeeId", user.getEmployee() == null ? null : user.getEmployee().getId());
            return item;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        String token = jwtUtil.generateToken(loginRequest.getUsername());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }
}

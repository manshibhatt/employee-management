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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("employeeId", user.getEmployee() == null ? null : user.getEmployee().getId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<Map<String, Object>>> users() {
        return ResponseEntity.ok(userRepository.findAllByOrderByIdDesc().stream().map(this::toUserSummary).toList());
    }

    @GetMapping("/employee-logins")
    @PreAuthorize("hasAnyRole('LEAVE_APPROVER', 'ADMIN')")
    public ResponseEntity<java.util.List<Map<String, Object>>> employeeLogins() {
        return ResponseEntity.ok(userRepository.findAllByOrderByIdDesc().stream().map(this::toUserSummary).toList());
    }

    private Map<String, Object> toUserSummary(User user) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", user.getId());
        item.put("username", user.getUsername());
        item.put("role", user.getRole());
        item.put("employeeId", user.getEmployee() == null ? null : user.getEmployee().getId());
        return item;
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestBody UserRoleUpdateRequest request,
            Authentication authentication) {
        if (request == null || request.role() == null
                || !(request.role().equals("ROLE_USER") || request.role().equals("ROLE_LEAVE_APPROVER"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose Employee or Leave Approver");
        }

        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        User actor = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin account not found"));

        if ("ROLE_ADMIN".equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin roles can only be assigned directly by a database administrator");
        }

        if (target.getId().equals(actor.getId()) && !request.role().equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change your own role");
        }
        if ("ROLE_LEAVE_APPROVER".equals(target.getRole())
                && "ROLE_USER".equals(request.role())
                && userRepository.countByRole("ROLE_LEAVE_APPROVER") <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one leave approver must remain");
        }

        target.setRole(request.role());
        User saved = userRepository.save(target);
        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("username", saved.getUsername());
        result.put("role", saved.getRole());
        result.put("employeeId", saved.getEmployee() == null ? null : saved.getEmployee().getId());
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

    public record UserRoleUpdateRequest(String role) {}
}

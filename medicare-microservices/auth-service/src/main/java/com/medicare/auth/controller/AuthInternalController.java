package com.medicare.auth.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medicare.auth.model.Patient;
import com.medicare.auth.model.User;
import com.medicare.auth.repository.PatientRepository;
import com.medicare.auth.repository.UserRepository;

/**
 * Internal REST API endpoints consumed by other microservices via OpenFeign.
 * These are NOT exposed to the public API Gateway.
 */
@RestController
@RequestMapping("/api/internal/auth")
public class AuthInternalController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public AuthInternalController(UserRepository userRepository, PatientRepository patientRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@RequestParam("username") String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("fullName", user.getFullName());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("role", user.getRole().name());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/patient")
    public ResponseEntity<Map<String, Object>> getPatientByUsername(@RequestParam("username") String username) {
        Optional<Patient> patientOpt = patientRepository.findByPatientUsername(username);
        if (patientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Patient patient = patientOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("id", patient.getId());
        result.put("fullName", patient.getFullName());
        result.put("contact", patient.getContact());
        result.put("email", patient.getEmail());
        result.put("patientUsername", patient.getPatientUsername());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("patientCount", patientRepository.count());
        stats.put("userCount", userRepository.count());
        return ResponseEntity.ok(stats);
    }
}

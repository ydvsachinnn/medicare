package com.medicare.clinical.client;

import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OpenFeign client for inter-service communication with Auth Service.
 * Resolves via Eureka service name "AUTH-SERVICE".
 */
@FeignClient(name = "AUTH-SERVICE")
public interface AuthServiceClient {

    @GetMapping("/api/internal/auth/user")
    Map<String, Object> getUserByUsername(@RequestParam("username") String username);

    @GetMapping("/api/internal/auth/patient")
    Map<String, Object> getPatientByUsername(@RequestParam("username") String username);

    @GetMapping("/api/internal/auth/stats")
    Map<String, Long> getStats();
}

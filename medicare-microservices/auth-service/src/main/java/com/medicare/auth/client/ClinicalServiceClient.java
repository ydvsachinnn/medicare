package com.medicare.auth.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OpenFeign client for inter-service communication with Clinical Service.
 * Resolves via Eureka service name "CLINICAL-SERVICE".
 */
@FeignClient(name = "CLINICAL-SERVICE", fallbackFactory = ClinicalServiceClientFallback.class)
public interface ClinicalServiceClient {

    @GetMapping("/api/internal/stats")
    Map<String, Long> getStats();

    @GetMapping("/api/internal/notifications")
    List<Map<String, Object>> getPatientNotifications(@RequestParam("username") String username);

    @GetMapping("/api/internal/appointments")
    List<Map<String, Object>> getPatientAppointments(@RequestParam("username") String username);

    @GetMapping("/api/internal/emergency-appointments")
    List<Map<String, Object>> getEmergencyAppointments();
}

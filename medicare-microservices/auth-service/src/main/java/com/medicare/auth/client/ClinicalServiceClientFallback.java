package com.medicare.auth.client;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback factory for ClinicalServiceClient.
 * Returns safe defaults when the clinical-service is unavailable.
 */
@Component
public class ClinicalServiceClientFallback implements FallbackFactory<ClinicalServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(ClinicalServiceClientFallback.class);

    @Override
    public ClinicalServiceClient create(Throwable cause) {
        log.warn("[FEIGN FALLBACK] Clinical-service unavailable: {}", cause.getMessage());
        return new ClinicalServiceClient() {
            @Override
            public Map<String, Long> getStats() {
                return Map.of("medicineCount", 0L, "reportCount", 0L, "prescriptionCount", 0L);
            }

            @Override
            public List<Map<String, Object>> getPatientNotifications(String username) {
                return List.of();
            }

            @Override
            public List<Map<String, Object>> getPatientAppointments(String username) {
                return List.of();
            }

            @Override
            public List<Map<String, Object>> getEmergencyAppointments() {
                return List.of();
            }
        };
    }
}

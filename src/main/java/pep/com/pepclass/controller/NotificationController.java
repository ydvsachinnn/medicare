package pep.com.pepclass.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pep.com.pepclass.model.MedicationNotification;
import pep.com.pepclass.repository.MedicationNotificationRepository;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final MedicationNotificationRepository notificationRepository;

    public NotificationController(MedicationNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<MedicationNotification>> getActiveNotifications(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.badRequest().build();
        }
        List<MedicationNotification> dueList = notificationRepository.findByPatientUsernameAndStatus(authentication.getName(), "DUE");
        return ResponseEntity.ok(dueList);
    }

    @PostMapping("/{id}/take")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Map<String, Object>> markAsTaken(@PathVariable("id") String id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        Optional<MedicationNotification> opt = notificationRepository.findById(id);
        if (opt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Notification not found");
            return ResponseEntity.status(404).body(response);
        }

        MedicationNotification notification = opt.get();
        if (!notification.getPatientUsername().equalsIgnoreCase(authentication.getName())) {
            response.put("success", false);
            response.put("message", "Unauthorized access");
            return ResponseEntity.status(403).body(response);
        }

        notification.setStatus("TAKEN");
        notification.setTakenAt(LocalDateTime.now());
        notificationRepository.save(notification);

        response.put("success", true);
        response.put("message", "Medication marked as taken");
        response.put("notificationId", id);
        return ResponseEntity.ok(response);
    }
}

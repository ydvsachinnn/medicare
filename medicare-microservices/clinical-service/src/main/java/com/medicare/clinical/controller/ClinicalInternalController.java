package com.medicare.clinical.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medicare.clinical.model.Appointment;
import com.medicare.clinical.model.MedicationNotification;
import com.medicare.clinical.repository.AppointmentRepository;
import com.medicare.clinical.repository.MedicalReportRepository;
import com.medicare.clinical.repository.MedicationNotificationRepository;
import com.medicare.clinical.repository.MedicineRepository;
import com.medicare.clinical.repository.PrescriptionRepository;

/**
 * Internal REST API endpoints consumed by other microservices via OpenFeign.
 * These are NOT exposed to the public API Gateway.
 */
@RestController
@RequestMapping("/api/internal")
public class ClinicalInternalController {

    private final MedicineRepository medicineRepository;
    private final MedicalReportRepository medicalReportRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicationNotificationRepository notificationRepository;

    public ClinicalInternalController(
            MedicineRepository medicineRepository,
            MedicalReportRepository medicalReportRepository,
            PrescriptionRepository prescriptionRepository,
            AppointmentRepository appointmentRepository,
            MedicationNotificationRepository notificationRepository) {
        this.medicineRepository = medicineRepository;
        this.medicalReportRepository = medicalReportRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("medicineCount", medicineRepository.count());
        stats.put("reportCount", medicalReportRepository.count());
        stats.put("prescriptionCount", prescriptionRepository.count());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<MedicationNotification>> getPatientNotifications(@RequestParam("username") String username) {
        List<MedicationNotification> dueList = notificationRepository.findByPatientUsernameAndStatus(username, "DUE");
        return ResponseEntity.ok(dueList);
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<Appointment>> getPatientAppointments(@RequestParam("username") String username) {
        List<Appointment> appts = appointmentRepository.findByPatientUsername(username);
        return ResponseEntity.ok(appts);
    }

    @GetMapping("/emergency-appointments")
    public ResponseEntity<List<Appointment>> getEmergencyAppointments() {
        List<Appointment> emergencies = appointmentRepository.findAll().stream()
                .filter(a -> a.isEmergency() && "PENDING".equalsIgnoreCase(a.getStatus()))
                .toList();
        return ResponseEntity.ok(emergencies);
    }
}

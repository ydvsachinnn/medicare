package com.medicare.clinical.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.medicare.clinical.model.Appointment;
import com.medicare.clinical.model.Patient;
import com.medicare.clinical.model.Role;
import com.medicare.clinical.model.User;
import com.medicare.clinical.repository.AppointmentRepository;
import com.medicare.clinical.repository.PatientRepository;
import com.medicare.clinical.repository.UserRepository;

@Controller
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public AppointmentController(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/appointments/book")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'CHAIRMAN')")
    public String bookAppointmentForm(
            @RequestParam(value = "doctorId", required = false) String doctorIdParam,
            @RequestParam(value = "patientId", required = false) String patientIdParam,
            Model model,
            Authentication authentication) {

        Appointment appointment = new Appointment();

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username).orElse(null));

        if (doctorIdParam != null && !doctorIdParam.isBlank()) {
            appointment.setDoctorId(doctorIdParam);
        }
        if (patientIdParam != null && !patientIdParam.isBlank()) {
            patientRepository.findById(patientIdParam).ifPresent(p -> {
                appointment.setPatientUsername(p.getPatientUsername() != null ? p.getPatientUsername() : p.getFullName());
                appointment.setPatientName(p.getFullName());
                appointment.setPatientPhone(p.getContact());
            });
        } else if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
            appointment.setPatientUsername(username);
            appointment.setPatientName(currentUser.getFullName());
            appointment.setPatientPhone(currentUser.getPhone());
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("doctors", userRepository.findByRole(Role.DOCTOR));
        model.addAttribute("patients", patientRepository.findAll());
        model.addAttribute("userRole", currentUser != null ? currentUser.getRole().name() : "PATIENT");
        return "book-appointment";
    }

    @PostMapping("/appointments/book")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'CHAIRMAN')")
    public String saveAppointment(
            @Valid @ModelAttribute("appointment") Appointment appointment,
            BindingResult result,
            Model model,
            Authentication authentication) {

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username).orElse(null));

        if (result.hasErrors()) {
            model.addAttribute("doctors", userRepository.findByRole(Role.DOCTOR));
            model.addAttribute("patients", patientRepository.findAll());
            model.addAttribute("userRole", currentUser != null ? currentUser.getRole().name() : "PATIENT");
            return "book-appointment";
        }

        if (appointment.getId() != null && appointment.getId().isBlank()) {
            appointment.setId(null);
        }

        // If patient name/username not set, resolve from logged in user or patient repo
        if (appointment.getPatientUsername() == null || appointment.getPatientUsername().isBlank()) {
            appointment.setPatientUsername(username);
        }

        if (appointment.getPatientName() == null || appointment.getPatientName().isBlank()) {
            Patient patient = patientRepository.findByPatientUsername(appointment.getPatientUsername()).orElse(null);
            if (patient != null) {
                appointment.setPatientName(patient.getFullName());
                appointment.setPatientPhone(patient.getContact());
            } else if (currentUser != null) {
                appointment.setPatientName(currentUser.getFullName());
                appointment.setPatientPhone(currentUser.getPhone());
            }
        }

        if (appointment.getDoctorId() != null && !appointment.getDoctorId().isBlank()) {
            userRepository.findById(appointment.getDoctorId()).ifPresent(doc -> {
                appointment.setDoctorName(doc.getFullName());
            });
        }

        appointment.setStatus("PENDING");
        appointmentRepository.save(appointment);

        if (currentUser != null && (currentUser.getRole() == Role.DOCTOR || currentUser.getRole() == Role.CHAIRMAN)) {
            return "redirect:/appointments?booked=true";
        }
        return "redirect:/my-appointments?booked=true";
    }

    @GetMapping("/my-appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientAppointments(Model model, Authentication authentication) {
        String username = authentication.getName();
        List<Appointment> appointments = appointmentRepository.findByPatientUsername(username);
        model.addAttribute("appointments", appointments);
        return "my-appointments";
    }

    @PostMapping({"/appointments/{id}/cancel", "/my-appointments/{id}/cancel"})
    @PreAuthorize("hasRole('PATIENT')")
    public String cancelAppointment(@PathVariable("id") String id, Authentication authentication) {
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment != null && appointment.getPatientUsername().equalsIgnoreCase(authentication.getName())) {
            appointment.setStatus("CANCELLED");
            appointmentRepository.save(appointment);
        }
        return "redirect:/my-appointments";
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String listAppointments(Model model, Authentication authentication) {
        List<Appointment> appointments = appointmentRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("appointments", appointments);
        return "appointments";
    }

    @PostMapping("/appointments/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String updateAppointmentStatus(
            @PathVariable("id") String id,
            @RequestParam("status") String status,
            @RequestParam(value = "notes", required = false) String notes) {

        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment != null) {
            appointment.setStatus(status);
            if (notes != null && !notes.isBlank()) {
                appointment.setNotes(notes);
            }
            appointmentRepository.save(appointment);
        }
        return "redirect:/appointments";
    }
}

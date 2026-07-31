package pep.com.pepclass.controller;

import jakarta.validation.Valid;
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

import pep.com.pepclass.model.MedicationNotification;
import pep.com.pepclass.model.Prescription;
import pep.com.pepclass.model.Role;
import pep.com.pepclass.model.User;
import pep.com.pepclass.repository.MedicationNotificationRepository;
import pep.com.pepclass.repository.MedicineRepository;
import pep.com.pepclass.repository.PatientRepository;
import pep.com.pepclass.repository.PrescriptionRepository;
import pep.com.pepclass.repository.UserRepository;

@Controller
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;
    private final MedicationNotificationRepository notificationRepository;

    public PrescriptionController(
            PrescriptionRepository prescriptionRepository,
            PatientRepository patientRepository,
            MedicineRepository medicineRepository,
            UserRepository userRepository,
            MedicationNotificationRepository notificationRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.medicineRepository = medicineRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/prescriptions")
    public String prescriptions(Model model, Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))) {
            return "redirect:/my-prescriptions";
        }
        model.addAttribute("prescriptions", prescriptionRepository.findAll());
        return "prescriptions";
    }

    @GetMapping("/prescriptions/new")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String newPrescription(@RequestParam(value = "patientId", required = false) String patientId, Model model) {
        Prescription prescription = new Prescription();
        if (patientId != null && !patientId.isBlank()) {
            patientRepository.findById(patientId).ifPresent(prescription::setPatient);
        }
        model.addAttribute("prescription", prescription);
        model.addAttribute("patients", patientRepository.findAll());
        return "prescription-form";
    }

    @PostMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String savePrescription(
            @Valid @ModelAttribute("prescription") Prescription prescription,
            BindingResult result,
            Model model,
            Authentication authentication) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientRepository.findAll());
            return "prescription-form";
        }
        if (prescription.getId() != null && prescription.getId().isBlank()) {
            prescription.setId(null);
        }
        if (prescription.getPrescribedBy() == null || prescription.getPrescribedBy().isBlank()) {
            prescription.setPrescribedBy(authentication.getName());
        }

        if (prescription.getPatient() != null && prescription.getPatient().getId() != null) {
            patientRepository.findById(prescription.getPatient().getId()).ifPresent(p -> {
                prescription.setPatient(p);
            });
        }

        Prescription saved = prescriptionRepository.save(prescription);

        if (saved.getId() != null) {
            notificationRepository.deleteByPrescriptionId(saved.getId());
        }

        String patientUsername = saved.getPatient() != null ? saved.getPatient().getPatientUsername() : null;
        if (patientUsername == null || patientUsername.isBlank()) {
            if (saved.getPatient() != null && saved.getPatient().getEmail() != null && !saved.getPatient().getEmail().isBlank()) {
                patientUsername = saved.getPatient().getEmail();
            } else if (saved.getPatient() != null && saved.getPatient().getFullName() != null) {
                patientUsername = saved.getPatient().getFullName();
            }
        }
        String patientName = saved.getPatient() != null ? saved.getPatient().getFullName() : "Patient";
        String foodTiming = saved.getFoodTiming() != null ? saved.getFoodTiming() : "After Food";
        String doctor = saved.getPrescribedBy();

        if (patientUsername != null && !patientUsername.isBlank()) {
            if (saved.getMorning() != null && !saved.getMorning().isBlank()) {
                notificationRepository.save(new MedicationNotification(
                        patientUsername, patientName, saved.getId(),
                        saved.getMedicineName(), saved.getDosage(), "Morning", "08:00 AM", foodTiming, doctor
                ));
            }
            if (saved.getAfternoon() != null && !saved.getAfternoon().isBlank()) {
                notificationRepository.save(new MedicationNotification(
                        patientUsername, patientName, saved.getId(),
                        saved.getMedicineName(), saved.getDosage(), "Afternoon", "01:00 PM", foodTiming, doctor
                ));
            }
            if (saved.getEvening() != null && !saved.getEvening().isBlank()) {
                notificationRepository.save(new MedicationNotification(
                        patientUsername, patientName, saved.getId(),
                        saved.getMedicineName(), saved.getDosage(), "Evening", "06:00 PM", foodTiming, doctor
                ));
            }
            if (saved.getNight() != null && !saved.getNight().isBlank()) {
                notificationRepository.save(new MedicationNotification(
                        patientUsername, patientName, saved.getId(),
                        saved.getMedicineName(), saved.getDosage(), "Night", "09:00 PM", foodTiming, doctor
                ));
            }
        }

        return "redirect:/prescriptions";
    }

    @GetMapping("/prescriptions/{id}/delete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String deletePrescription(@PathVariable("id") String id) {
        prescriptionRepository.deleteById(id);
        notificationRepository.deleteByPrescriptionId(id);
        return "redirect:/prescriptions";
    }

    @GetMapping("/prescriptions/{id}/print")
    public String printPrescription(
            @PathVariable("id") String id,
            @RequestParam(value = "download", required = false) String download,
            Model model,
            Authentication authentication) {

        if (id == null || id.isBlank()) {
            return "redirect:/dashboard";
        }

        Prescription prescription = prescriptionRepository.findById(id).orElse(null);
        if (prescription == null) {
            return "redirect:/dashboard";
        }

        if (authentication != null) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseGet(() -> userRepository.findByEmail(username).orElse(null));

            if (user != null && user.getRole() == Role.PATIENT) {
                if (prescription.getPatient() != null) {
                    String patientUser = prescription.getPatient().getPatientUsername();
                    String patientEmail = prescription.getPatient().getEmail();
                    String patientName = prescription.getPatient().getFullName();

                    boolean matchesUser = (patientUser != null && (patientUser.equalsIgnoreCase(user.getUsername()) || patientUser.equalsIgnoreCase(user.getEmail()) || patientUser.equalsIgnoreCase(username))) ||
                                          (patientEmail != null && (patientEmail.equalsIgnoreCase(user.getEmail()) || patientEmail.equalsIgnoreCase(username))) ||
                                          (patientName != null && patientName.equalsIgnoreCase(user.getFullName()));

                    if (!matchesUser) {
                        return "redirect:/my-prescriptions";
                    }
                }
            }
        }

        String doctorUsername = prescription.getPrescribedBy();
        User doctor = (doctorUsername != null) ? userRepository.findByUsername(doctorUsername)
                .orElseGet(() -> userRepository.findByEmail(doctorUsername).orElse(null)) : null;
        String doctorName = (doctor != null && doctor.getFullName() != null) ? doctor.getFullName() : (doctorUsername != null ? doctorUsername : "Attending Physician");

        model.addAttribute("prescription", prescription);
        model.addAttribute("patient", prescription.getPatient());
        model.addAttribute("doctorName", doctorName);

        return "prescription-print";
    }

    @GetMapping("/prescriptions/{id}/download")
    public String downloadPrescription(
            @PathVariable("id") String id,
            @RequestParam(value = "download", required = false) String download,
            Model model,
            Authentication authentication) {
        return printPrescription(id, download, model, authentication);
    }

    @GetMapping("/my-prescriptions")
    public String patientPrescriptions(Model model, Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR") || a.getAuthority().equals("ROLE_CHAIRMAN"))) {
            return "redirect:/prescriptions";
        }

        String authName = authentication != null ? authentication.getName() : null;
        User user = (authName != null) ? userRepository.findByUsername(authName)
                .orElseGet(() -> userRepository.findByEmail(authName).orElse(null)) : null;

        java.util.Set<String> searchKeys = new java.util.HashSet<>();
        if (authName != null && !authName.isBlank()) {
            searchKeys.add(authName);
        }
        if (user != null) {
            if (user.getUsername() != null && !user.getUsername().isBlank()) searchKeys.add(user.getUsername());
            if (user.getEmail() != null && !user.getEmail().isBlank()) searchKeys.add(user.getEmail());
            if (user.getFullName() != null && !user.getFullName().isBlank()) searchKeys.add(user.getFullName());
        }

        java.util.List<pep.com.pepclass.model.Patient> matchingPatients = new java.util.ArrayList<>();
        for (String key : searchKeys) {
            if (key == null || key.isBlank()) continue;
            patientRepository.findByPatientUsername(key).ifPresent(matchingPatients::add);
            matchingPatients.addAll(patientRepository.findByEmailIgnoreCase(key));
            matchingPatients.addAll(patientRepository.findByFullNameIgnoreCase(key));
            matchingPatients.addAll(patientRepository.findByContact(key));
        }

        java.util.List<Prescription> userPrescriptions = new java.util.ArrayList<>();
        java.util.List<pep.com.pepclass.model.Medicine> userMedicines = new java.util.ArrayList<>();
        java.util.List<MedicationNotification> userNotifications = new java.util.ArrayList<>();

        for (pep.com.pepclass.model.Patient p : matchingPatients) {
            if (p != null && p.getId() != null && !p.getId().isBlank()) {
                for (Prescription rx : prescriptionRepository.findByPatientId(p.getId())) {
                    if (rx != null && rx.getId() != null && userPrescriptions.stream().noneMatch(x -> rx.getId().equals(x.getId()))) {
                        userPrescriptions.add(rx);
                    }
                }
                for (pep.com.pepclass.model.Medicine m : medicineRepository.findByPatientId(p.getId())) {
                    if (m != null && m.getId() != null && userMedicines.stream().noneMatch(x -> m.getId().equals(x.getId()))) {
                        userMedicines.add(m);
                    }
                }
            }
        }

        for (String key : searchKeys) {
            if (key == null || key.isBlank()) continue;
            for (Prescription rx : prescriptionRepository.findByPatientPatientUsername(key)) {
                if (rx != null && rx.getId() != null && userPrescriptions.stream().noneMatch(x -> rx.getId().equals(x.getId()))) {
                    userPrescriptions.add(rx);
                }
            }
            for (Prescription rx : prescriptionRepository.findByPatientEmailIgnoreCase(key)) {
                if (rx != null && rx.getId() != null && userPrescriptions.stream().noneMatch(x -> rx.getId().equals(x.getId()))) {
                    userPrescriptions.add(rx);
                }
            }
            for (Prescription rx : prescriptionRepository.findByPatientFullNameIgnoreCase(key)) {
                if (rx != null && rx.getId() != null && userPrescriptions.stream().noneMatch(x -> rx.getId().equals(x.getId()))) {
                    userPrescriptions.add(rx);
                }
            }

            for (pep.com.pepclass.model.Medicine m : medicineRepository.findByPatientPatientUsername(key)) {
                if (m != null && m.getId() != null && userMedicines.stream().noneMatch(x -> m.getId().equals(x.getId()))) {
                    userMedicines.add(m);
                }
            }
            for (pep.com.pepclass.model.Medicine m : medicineRepository.findByPatientEmailIgnoreCase(key)) {
                if (m != null && m.getId() != null && userMedicines.stream().noneMatch(x -> m.getId().equals(x.getId()))) {
                    userMedicines.add(m);
                }
            }

            for (MedicationNotification n : notificationRepository.findByPatientUsernameAndStatus(key, "DUE")) {
                if (n != null && n.getId() != null && userNotifications.stream().noneMatch(x -> n.getId().equals(x.getId()))) {
                    userNotifications.add(n);
                }
            }
        }

        model.addAttribute("prescriptions", userPrescriptions);
        model.addAttribute("medicines", userMedicines);
        model.addAttribute("notifications", userNotifications);
        return "patient-prescriptions";
    }
}

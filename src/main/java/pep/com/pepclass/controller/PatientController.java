package pep.com.pepclass.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import pep.com.pepclass.model.Patient;
import pep.com.pepclass.repository.MedicineRepository;
import pep.com.pepclass.repository.PatientRepository;
import pep.com.pepclass.repository.UserRepository;

@Controller
public class PatientController {

    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;

    public PatientController(PatientRepository patientRepository, MedicineRepository medicineRepository, UserRepository userRepository) {
        this.patientRepository = patientRepository;
        this.medicineRepository = medicineRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String listPatients(Model model) {
        List<Patient> patients = patientRepository.findAll();
        model.addAttribute("patients", patients);
        model.addAttribute("medicines", medicineRepository.findAll());
        return "patients";
    }

    @GetMapping("/patients/new")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String newPatient(Model model) {
        model.addAttribute("patient", new Patient());
        return "patient-form";
    }

    @PostMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String savePatient(@Valid @ModelAttribute Patient patient, BindingResult result) {
        if (result.hasErrors()) {
            return "patient-form";
        }
        if (patient.getId() != null && patient.getId().isBlank()) {
            patient.setId(null);
        }
        if (patient.getPatientUsername() == null || patient.getPatientUsername().isBlank()) {
            if (patient.getEmail() != null && !patient.getEmail().isBlank()) {
                userRepository.findByEmail(patient.getEmail().trim().toLowerCase()).ifPresent(u -> {
                    patient.setPatientUsername(u.getUsername());
                });
            }
        }
        patientRepository.save(patient);
        return "redirect:/patients";
    }

    @GetMapping("/patients/{id}/edit")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String editPatient(@PathVariable("id") String id, Model model) {
        Patient patient = patientRepository.findById(id).orElseThrow();
        model.addAttribute("patient", patient);
        return "patient-form";
    }
}

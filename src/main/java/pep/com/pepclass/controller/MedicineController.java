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
import org.springframework.web.bind.annotation.RequestParam;

import pep.com.pepclass.model.Medicine;
import pep.com.pepclass.repository.MedicineRepository;
import pep.com.pepclass.repository.PatientRepository;

@Controller
public class MedicineController {

    private final MedicineRepository medicineRepository;
    private final PatientRepository patientRepository;

    public MedicineController(MedicineRepository medicineRepository, PatientRepository patientRepository) {
        this.medicineRepository = medicineRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/medicines")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String listMedicines(Model model) {
        List<Medicine> medicines = medicineRepository.findAll();
        model.addAttribute("medicines", medicines);
        return "medicines";
    }

    @GetMapping("/medicines/new")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String newMedicine(Model model, @RequestParam(value = "patientId", required = false) String patientId) {
        Medicine medicine = new Medicine();
        if (patientId != null) {
            patientRepository.findById(patientId).ifPresent(medicine::setPatient);
        }
        model.addAttribute("medicine", medicine);
        model.addAttribute("patients", patientRepository.findAll());
        return "medicine-form";
    }

    @PostMapping("/medicines")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String saveMedicine(@Valid @ModelAttribute("medicine") Medicine medicine, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientRepository.findAll());
            return "medicine-form";
        }
        if (medicine.getId() != null && medicine.getId().isBlank()) {
            medicine.setId(null);
        }
        medicineRepository.save(medicine);
        return "redirect:/medicines";
    }

    @GetMapping("/medicines/{id}/edit")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String editMedicine(@PathVariable("id") String id, Model model) {
        model.addAttribute("medicine", medicineRepository.findById(id).orElseThrow());
        model.addAttribute("patients", patientRepository.findAll());
        return "medicine-form";
    }

    @GetMapping("/medicines/{id}/delete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String deleteMedicine(@PathVariable("id") String id) {
        medicineRepository.deleteById(id);
        return "redirect:/medicines";
    }
}

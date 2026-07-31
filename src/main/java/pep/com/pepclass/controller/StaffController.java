package pep.com.pepclass.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import pep.com.pepclass.dto.StaffForm;
import pep.com.pepclass.model.Role;
import pep.com.pepclass.model.User;
import pep.com.pepclass.repository.UserRepository;

@Controller
@PreAuthorize("hasRole('CHAIRMAN')")
public class StaffController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/doctors")
    public String doctors(Model model) {
        model.addAttribute("doctors", userRepository.findByRole(Role.DOCTOR));
        return "doctors";
    }

    @GetMapping("/doctors/new")
    public String newDoctor(Model model) {
        model.addAttribute("doctor", new StaffForm());
        return "doctor-form";
    }

    @PostMapping("/doctors")
    public String saveDoctor(@Valid @ModelAttribute("doctor") StaffForm form, BindingResult result) {
        if (userRepository.findByUsername(form.getUsername()).isPresent()) {
            result.rejectValue("username", "duplicate", "Username already exists");
        }
        if (result.hasErrors()) {
            return "doctor-form";
        }
        User doctor = new User();
        doctor.setUsername(form.getUsername());
        doctor.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        doctor.setFullName(form.getFullName());
        doctor.setEmail(form.getEmail());
        doctor.setPhone(form.getPhone());
        doctor.setRole(Role.DOCTOR);
        doctor.setEnabled(true);
        userRepository.save(doctor);
        return "redirect:/doctors";
    }

    @GetMapping("/doctors/{id}/delete")
    public String deleteDoctor(@PathVariable("id") String id) {
        userRepository.deleteById(id);
        return "redirect:/doctors";
    }
}

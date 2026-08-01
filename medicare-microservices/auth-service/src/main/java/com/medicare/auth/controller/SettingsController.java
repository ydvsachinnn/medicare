package com.medicare.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.medicare.auth.model.User;
import com.medicare.auth.repository.PatientRepository;
import com.medicare.auth.repository.UserRepository;
import com.medicare.auth.service.ConversationService;

@Controller
public class SettingsController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConversationService conversationService;

    public SettingsController(
            UserRepository userRepository,
            PatientRepository patientRepository,
            PasswordEncoder passwordEncoder,
            ConversationService conversationService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.conversationService = conversationService;
    }

    /**
     * REST endpoint to clear the patient's chatbot conversation history.
     * Called by the chatbot JavaScript when the user clicks the "Clear Chat" button.
     */
    @PostMapping("/api/settings/clear-chat")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseBody
    public ResponseEntity<Void> clearChatHistory(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "";
        conversationService.clearHistory(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public String getSettings(Model model, Authentication authentication) {
        String identifier = authentication.getName();
        User user = userRepository.findByUsername(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier).orElse(null));

        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("fullName", user.getFullName());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("phone", user.getPhone());
            model.addAttribute("role", user.getRole().name());
        }

        return "settings";
    }

    @PostMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public String updateSettings(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            Authentication authentication) {

        String identifier = authentication.getName();
        User user = userRepository.findByUsername(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier).orElse(null));

        if (user != null) {
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);

            if (newPassword != null && !newPassword.isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(newPassword));
            }

            userRepository.save(user);

            // If user is a patient, sync patient details too
            patientRepository.findAll().stream()
                    .filter(p -> p.getPatientUsername() != null && 
                            (p.getPatientUsername().equalsIgnoreCase(user.getUsername()) || 
                             p.getPatientUsername().equalsIgnoreCase(user.getEmail())))
                    .findFirst()
                    .ifPresent(patient -> {
                        patient.setFullName(fullName);
                        patient.setEmail(email);
                        patient.setContact(phone);
                        patientRepository.save(patient);
                    });
        }

        return "redirect:/settings?success";
    }
}

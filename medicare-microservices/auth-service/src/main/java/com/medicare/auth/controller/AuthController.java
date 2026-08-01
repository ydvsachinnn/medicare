package com.medicare.auth.controller;

import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.medicare.auth.dto.ForgotPasswordForm;
import com.medicare.auth.dto.RegistrationForm;
import com.medicare.auth.dto.ResetPasswordForm;
import com.medicare.auth.model.PasswordResetOtp;
import com.medicare.auth.model.Patient;
import com.medicare.auth.model.Role;
import com.medicare.auth.model.User;
import com.medicare.auth.repository.PasswordResetOtpRepository;
import com.medicare.auth.repository.PatientRepository;
import com.medicare.auth.repository.UserRepository;
import com.medicare.auth.service.EmailService;
import com.medicare.auth.client.ClinicalServiceClient;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ClinicalServiceClient clinicalServiceClient;

    public AuthController(
            UserRepository userRepository,
            PatientRepository patientRepository,
            PasswordResetOtpRepository otpRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            ClinicalServiceClient clinicalServiceClient) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.clinicalServiceClient = clinicalServiceClient;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") RegistrationForm form, BindingResult result, Model model) {
        if (form.getUsername() == null || form.getUsername().isBlank()) {
            form.setUsername(form.getEmail());
        }
        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            result.rejectValue("email", "duplicate", "A user account with this email address already exists.");
        }
        if (userRepository.findByUsername(form.getUsername()).isPresent()) {
            result.rejectValue("username", "duplicate", "Username or email already in use.");
        }
        if (result.hasErrors()) {
            return "register";
        }
        User user = new User();
        user.setUsername(form.getUsername());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setPhone(form.getPhone());
        user.setRole(Role.PATIENT);
        user.setEnabled(true);
        userRepository.save(user);

        Patient patient = new Patient();
        patient.setFullName(form.getFullName());
        patient.setContact(form.getPhone());
        patient.setEmail(form.getEmail());
        patient.setPatientUsername(form.getUsername());
        patientRepository.save(patient);

        return "redirect:/login?registered";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("form", new ForgotPasswordForm());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute("form") ForgotPasswordForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "forgot-password";
        }
        String inputEmail = form.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(inputEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(inputEmail);
        }
        if (userOpt.isEmpty()) {
            result.rejectValue("email", "notFound", "No account registered with email address: " + inputEmail);
            return "forgot-password";
        }

        User user = userOpt.get();
        String targetEmail = user.getEmail() != null ? user.getEmail() : inputEmail;

        List<PasswordResetOtp> previousOtps = otpRepository.findByEmail(targetEmail.toLowerCase());
        for (PasswordResetOtp oldOtp : previousOtps) {
            oldOtp.setUsed(true);
            otpRepository.save(oldOtp);
        }

        String otpCode = String.format("%06d", new Random().nextInt(900000) + 100000);
        PasswordResetOtp otpRecord = new PasswordResetOtp();
        otpRecord.setEmail(targetEmail.toLowerCase());
        otpRecord.setOtpCode(otpCode);
        otpRecord.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        otpRecord.setUsed(false);
        otpRepository.save(otpRecord);

        emailService.sendOtpEmail(targetEmail, otpCode, 5);

        return "redirect:/verify-otp?email=" + targetEmail + "&sent=true";
    }

    @GetMapping("/resend-otp")
    public String resendOtp(@RequestParam("email") String email) {
        if (email == null || email.isBlank()) {
            return "redirect:/forgot-password";
        }
        ForgotPasswordForm form = new ForgotPasswordForm();
        form.setEmail(email);
        BindingResult dummyResult = new org.springframework.validation.BeanPropertyBindingResult(form, "form");
        return processForgotPassword(form, dummyResult, null);
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "sent", required = false) String sent,
            Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("email", email);
        model.addAttribute("sent", sent != null);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processVerifyOtp(
            @RequestParam("email") String email,
            @RequestParam("otpCode") String otpCode,
            Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/forgot-password";
        }
        String cleanEmail = email.trim().toLowerCase();
        String cleanCode = otpCode == null ? "" : otpCode.trim();

        Optional<PasswordResetOtp> otpOpt = otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(cleanEmail);
        if (otpOpt.isEmpty()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "No active OTP request found. Please enter your email again.");
            return "verify-otp";
        }

        PasswordResetOtp otpRecord = otpOpt.get();

        if (otpRecord.isExpired()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "The OTP code has expired (valid for 5 minutes). Please request a new OTP code.");
            return "verify-otp";
        }

        if (!otpRecord.getOtpCode().equals(cleanCode)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Incorrect 6-digit verification code. Please check your email and try again.");
            return "verify-otp";
        }

        otpRecord.setUsed(true);
        String resetToken = UUID.randomUUID().toString();
        otpRecord.setResetToken(resetToken);
        otpRepository.save(otpRecord);

        return "redirect:/reset-password?email=" + cleanEmail + "&token=" + resetToken;
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "token", required = false) String token,
            Model model) {
        if (email == null || token == null || email.isBlank() || token.isBlank()) {
            return "redirect:/forgot-password";
        }
        Optional<PasswordResetOtp> tokenOpt = otpRepository.findByEmailAndResetTokenAndUsedFalse(email.toLowerCase(), token);
        if (tokenOpt.isEmpty()) {
            return "redirect:/forgot-password";
        }

        ResetPasswordForm form = new ResetPasswordForm();
        form.setEmail(email);
        form.setResetCode(token);
        model.addAttribute("form", form);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@Valid @ModelAttribute("form") ResetPasswordForm form, BindingResult result, Model model) {
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        String cleanEmail = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(cleanEmail);
        }
        if (userOpt.isEmpty()) {
            result.rejectValue("email", "notFound", "Account not found for email: " + cleanEmail);
        }
        if (result.hasErrors()) {
            return "reset-password";
        }

        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);

        List<PasswordResetOtp> otps = otpRepository.findByEmail(cleanEmail);
        for (PasswordResetOtp otp : otps) {
            otp.setUsed(true);
            otpRepository.save(otp);
        }

        return "redirect:/login?resetSuccess";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username).orElse(null));

        if (user != null) {
            String displayName = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName() : user.getUsername();
            displayName = capitalizeWords(displayName);

            model.addAttribute("fullName", displayName);
            model.addAttribute("role", user.getRole().name());
            model.addAttribute("roleLabel", formatRole(user.getRole()));

            // Cross-service calls via OpenFeign for dashboard stats
            if (user.getRole() == Role.PATIENT) {
                try {
                    var notifications = clinicalServiceClient.getPatientNotifications(user.getUsername());
                    model.addAttribute("notifications", notifications);
                } catch (Exception e) {
                    model.addAttribute("notifications", List.of());
                }
                try {
                    var appts = clinicalServiceClient.getPatientAppointments(user.getUsername());
                    model.addAttribute("patientAppts", appts);
                } catch (Exception e) {
                    model.addAttribute("patientAppts", List.of());
                }
            }

            if (user.getRole() == Role.DOCTOR || user.getRole() == Role.CHAIRMAN) {
                try {
                    var emergencies = clinicalServiceClient.getEmergencyAppointments();
                    model.addAttribute("emergencyAlerts", emergencies);
                } catch (Exception e) {
                    model.addAttribute("emergencyAlerts", List.of());
                }
            }
        } else {
            model.addAttribute("fullName", capitalizeWords(username));
            model.addAttribute("role", "PATIENT");
            model.addAttribute("roleLabel", "Patient");
        }

        model.addAttribute("patientCount", patientRepository.count());

        // Cross-service stats via OpenFeign
        try {
            var stats = clinicalServiceClient.getStats();
            model.addAttribute("medicineCount", stats.getOrDefault("medicineCount", 0L));
            model.addAttribute("reportCount", stats.getOrDefault("reportCount", 0L));
            model.addAttribute("prescriptionCount", stats.getOrDefault("prescriptionCount", 0L));
        } catch (Exception e) {
            model.addAttribute("medicineCount", 0L);
            model.addAttribute("reportCount", 0L);
            model.addAttribute("prescriptionCount", 0L);
        }

        return "dashboard";
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return "";
        String[] words = str.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                result.append(Character.toUpperCase(w.charAt(0)))
                      .append(w.length() > 1 ? w.substring(1) : "")
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    private String formatRole(Role role) {
        String lower = role.name().toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }
}

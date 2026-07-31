package pep.com.pepclass.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pep.com.pepclass.model.Role;
import pep.com.pepclass.model.User;
import pep.com.pepclass.repository.UserRepository;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedDemoUser("chairmen", "chairmen123", "Hospital Chairman", Role.CHAIRMAN);
        seedDemoUser("doctor", "doctor123", "Dr. Alice Morgan", Role.DOCTOR);
        seedDemoUser("patient", "patient123", "Patient Bob Carter", Role.PATIENT);
    }

    private void seedDemoUser(String username, String rawPassword, String fullName, Role role) {
        userRepository.findByUsername(username).ifPresentOrElse(user -> {
            boolean changed = false;
            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                changed = true;
            }
            if (user.getRole() != role) {
                user.setRole(role);
                changed = true;
            }
            if (!user.isEnabled()) {
                user.setEnabled(true);
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
        }, () -> {
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            user.setEmail(username + "@hospital.local");
            user.setPhone("0000000000");
            user.setRole(role);
            user.setEnabled(true);
            userRepository.save(user);
        });
    }
}

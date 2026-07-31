package pep.com.pepclass;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import pep.com.pepclass.model.Medicine;
import pep.com.pepclass.model.Patient;
import pep.com.pepclass.model.Role;
import pep.com.pepclass.model.User;
import pep.com.pepclass.repository.MedicineRepository;
import pep.com.pepclass.repository.PatientRepository;
import pep.com.pepclass.repository.UserRepository;

@SpringBootTest
class HospitalManagementSystemTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void registerPatientShouldPersistAccount() {
        User user = new User();
        user.setUsername("patient1");
        user.setPasswordHash(passwordEncoder.encode("Str0ngPass!"));
        user.setFullName("Alice Smith");
        user.setEmail("alice@example.com");
        user.setPhone("+919876543210");
        user.setRole(Role.PATIENT);
        user.setEnabled(true);

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findByUsername("patient1")).isPresent();
    }

    @Test
    void demoAccountsShouldBeSeededForAllRoles() {
        assertThat(userRepository.findByUsername("chairman")).isPresent();
        assertThat(userRepository.findByUsername("doctor")).isPresent();
        assertThat(userRepository.findByUsername("patient")).isPresent();
    }

    @Test
    void medicineScheduleFieldsShouldPersist() {
        Patient patient = new Patient();
        patient.setFullName("Alice Smith");
        patient.setContact("+919876543210");
        patient.setEmail("alice@example.com");
        patient.setAge(30);
        patient.setPatientUsername("alice");
        patient.setMedicalHistory("No known allergies");
        Patient savedPatient = patientRepository.save(patient);

        Medicine medicine = new Medicine();
        medicine.setPatient(savedPatient);
        medicine.setName("Paracetamol");
        medicine.setDosage("1 tablet");
        medicine.setMorning("1");
        medicine.setAfternoon("0");
        medicine.setEvening("1");
        medicine.setNight("0");
        medicine.setFoodTiming("After Food");

        Medicine saved = medicineRepository.save(medicine);

        assertThat(saved.getId()).isNotNull();
        assertThat(medicineRepository.findById(saved.getId())).isPresent();
    }

}

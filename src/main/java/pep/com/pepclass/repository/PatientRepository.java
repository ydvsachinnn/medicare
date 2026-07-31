package pep.com.pepclass.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pep.com.pepclass.model.Patient;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {
    Optional<Patient> findByPatientUsername(String patientUsername);
    List<Patient> findByEmailIgnoreCase(String email);
    List<Patient> findByFullNameIgnoreCase(String fullName);
    List<Patient> findByContact(String contact);
}

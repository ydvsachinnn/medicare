package com.medicare.clinical.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.medicare.clinical.model.Medicine;

@Repository
public interface MedicineRepository extends MongoRepository<Medicine, String> {
    List<Medicine> findByPatientPatientUsername(String patientUsername);
    List<Medicine> findByPatientId(String patientId);
    List<Medicine> findByPatientEmailIgnoreCase(String email);
    List<Medicine> findByPatientFullNameIgnoreCase(String fullName);
}

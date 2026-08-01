package com.medicare.clinical.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.medicare.clinical.model.Prescription;

@Repository
public interface PrescriptionRepository extends MongoRepository<Prescription, String> {
    List<Prescription> findByPatientPatientUsername(String patientUsername);
    List<Prescription> findByPatientId(String patientId);
    List<Prescription> findByPatientEmailIgnoreCase(String email);
    List<Prescription> findByPatientFullNameIgnoreCase(String fullName);
}

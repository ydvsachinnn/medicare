package com.medicare.clinical.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.medicare.clinical.model.MedicalReport;

@Repository
public interface MedicalReportRepository extends MongoRepository<MedicalReport, String> {
    List<MedicalReport> findByPatientPatientUsername(String patientUsername);
    List<MedicalReport> findByPatientId(String patientId);
    List<MedicalReport> findByPatientEmailIgnoreCase(String email);
    List<MedicalReport> findByPatientFullNameIgnoreCase(String fullName);
}

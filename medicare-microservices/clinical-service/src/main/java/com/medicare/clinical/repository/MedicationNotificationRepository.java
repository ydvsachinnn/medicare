package com.medicare.clinical.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.medicare.clinical.model.MedicationNotification;

@Repository
public interface MedicationNotificationRepository extends MongoRepository<MedicationNotification, String> {

    List<MedicationNotification> findByPatientUsername(String patientUsername);

    List<MedicationNotification> findByPatientUsernameAndStatus(String patientUsername, String status);

    List<MedicationNotification> findByPrescriptionId(String prescriptionId);

    void deleteByPrescriptionId(String prescriptionId);
}

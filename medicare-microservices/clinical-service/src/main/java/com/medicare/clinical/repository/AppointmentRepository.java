package com.medicare.clinical.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.medicare.clinical.model.Appointment;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    List<Appointment> findByPatientUsername(String patientUsername);
    List<Appointment> findByDoctorId(String doctorId);
    List<Appointment> findByDoctorName(String doctorName);
    List<Appointment> findAllByOrderByCreatedAtDesc();
}

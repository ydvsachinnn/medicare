package pep.com.pepclass.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pep.com.pepclass.model.Appointment;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    List<Appointment> findByPatientUsername(String patientUsername);
    List<Appointment> findByDoctorId(String doctorId);
    List<Appointment> findByDoctorName(String doctorName);
    List<Appointment> findAllByOrderByCreatedAtDesc();
}

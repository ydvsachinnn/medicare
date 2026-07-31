package pep.com.pepclass.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "medication_notifications")
public class MedicationNotification {

    @Id
    private String id;

    private String patientUsername;

    private String patientName;

    private String prescriptionId;

    private String medicineName;

    private String dosage;

    private String timeSlot; // Morning, Afternoon, Evening, Night

    private String scheduledTime; // e.g. 08:00 AM, 01:00 PM, 06:00 PM, 09:00 PM

    private String foodTiming; // After Food, Before Food

    private String status = "DUE"; // DUE, TAKEN

    private String prescribedBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime takenAt;

    public MedicationNotification() {
    }

    public MedicationNotification(String patientUsername, String patientName, String prescriptionId,
                                  String medicineName, String dosage, String timeSlot,
                                  String scheduledTime, String foodTiming, String prescribedBy) {
        this.patientUsername = patientUsername;
        this.patientName = patientName;
        this.prescriptionId = prescriptionId;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.timeSlot = timeSlot;
        this.scheduledTime = scheduledTime;
        this.foodTiming = foodTiming;
        this.prescribedBy = prescribedBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientUsername() {
        return patientUsername;
    }

    public void setPatientUsername(String patientUsername) {
        this.patientUsername = patientUsername;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getFoodTiming() {
        return foodTiming;
    }

    public void setFoodTiming(String foodTiming) {
        this.foodTiming = foodTiming;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrescribedBy() {
        return prescribedBy;
    }

    public void setPrescribedBy(String prescribedBy) {
        this.prescribedBy = prescribedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }
}

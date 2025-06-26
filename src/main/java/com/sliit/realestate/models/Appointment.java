package com.sliit.realestate.models;
import java.time.LocalDateTime;
import java.util.UUID;

public class Appointment {

    private String appointmentId;
    private String agentId;
    private String clientId;
    private String timeSlotId;   //new

    private LocalDateTime dateTime;
    private AppointmentStatus status;
    private String notes;

    public enum AppointmentStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }

    // --- CONSTRUCTORS ---

    /**
     * Constructor for CREATING a NEW Appointment.
     * Generates a new UUID for the appointmentId.
     * Takes only the necessary IDs and details.
     */
    public Appointment(String agentId, String clientId,String timeSlotId, LocalDateTime dateTime, String notes) {
        this.appointmentId = UUID.randomUUID().toString(); // Generate unique ID automatically
        this.agentId = agentId;
        this.clientId = clientId;// Assign the clientId
        this.timeSlotId = timeSlotId;
        this.dateTime = dateTime;
        this.status = AppointmentStatus.PENDING; // Default status for new appointments
        this.notes = (notes != null) ? notes : ""; // Handle null notes
    }

    /**
     * Constructor for LOADING an EXISTING Appointment (e.g., from a file).
     * Takes all fields, including the existing appointmentId and status.
     */
    public Appointment(String appointmentId, String agentId, String clientId,String timeSlotId, LocalDateTime dateTime, AppointmentStatus status, String notes) {
        this.appointmentId = appointmentId;
        this.agentId = agentId;
        this.clientId = clientId; // Assign the clientId
        this.timeSlotId = timeSlotId;
        this.dateTime = dateTime;
        this.status = status;
        this.notes = (notes != null) ? notes : ""; // Handle null notes
    }

    // --- Getters ---
    public String getAppointmentId() { return appointmentId; }
    public String getAgentId() { return agentId; }
    public String getClientId() { return clientId; } // Now correctly assigned
    public String getTimeSlotId() { return timeSlotId; }
    public LocalDateTime getDateTime() { return dateTime; }
    public AppointmentStatus getStatus() { return status; }
    public String getNotes() { return notes; }

    // --- Setters ---


    // Only allow changing status and notes after creation

    public void setStatus(AppointmentStatus status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setTimeSlotId(String timeSlotId) { this.timeSlotId = timeSlotId; }



    @Override
    public String toString() {
        return "Appointment{" +
                "appointmentId='" + appointmentId + '\'' +
                ", agentId='" + agentId + '\'' +
                ", clientId='" + clientId + '\'' + // Use clientId in toString
                ", dateTime=" + dateTime +
                ", status=" + status +
                ", notes='" + notes + '\'' +
                '}';
    }
}
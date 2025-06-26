package com.sliit.realestate.repositories;

import com.sliit.realestate.models.Appointment;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AppointmentRepository {

    private static final String FILE_PATH_STR = "src/main/resources/appointments.txt";

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AppointmentRepository() {
        System.out.println(">>> AppointmentRepository: Initializing bean and ensuring file exists...");
        ensureFileExists();
        System.out.println(">>> AppointmentRepository: Initialization complete.");
    }

    /**
     * Ensures the directory structure and the appointment data file exist.
     */
    private void ensureFileExists() {
        File file = new File(FILE_PATH_STR);
        File directory = file.getParentFile();


        if (directory != null && !directory.exists()) {
            System.out.println(">>> AppointmentRepository.ensureFileExists: Creating directory: " + directory.getAbsolutePath());
            boolean created = directory.mkdirs();
            System.out.println(">>> AppointmentRepository.ensureFileExists: Directory created: " + created);
            if (!created) {
                System.err.println("!!! AppointmentRepository.ensureFileExists: FAILED to create directory: " + directory.getAbsolutePath());
            }
        } else if (directory == null) {
            System.err.println("!!! AppointmentRepository.ensureFileExists: Cannot determine parent directory for: " + FILE_PATH_STR);
        }

        if (!file.exists()) {
            try {
                System.out.println(">>> AppointmentRepository.ensureFileExists: File does not exist, attempting to create: " + file.getAbsolutePath());
                boolean created = file.createNewFile();
                System.out.println(">>> AppointmentRepository.ensureFileExists: Created new file: " + created);
                if (!created) {
                    System.err.println("!!! AppointmentRepository.ensureFileExists: FAILED to create new file (unknown reason): " + file.getAbsolutePath());
                }
            } catch (IOException e) {
                System.err.println("!!! AppointmentRepository.ensureFileExists: FAILED to create file due to IOException: " + e.getMessage());
                e.printStackTrace();
                // throw new RuntimeException("Failed to create appointment data file at " + file.getAbsolutePath(), e);
            }
        } else {
            System.out.println(">>> AppointmentRepository.ensureFileExists: File already exists: " + file.getAbsolutePath());
            System.out.println(">>> AppointmentRepository.ensureFileExists: File is readable: " + file.canRead());
            System.out.println(">>> AppointmentRepository.ensureFileExists: File is writable: " + file.canWrite());
        }
    }

    /**
     * Saves an appointment (creates if new, updates if exists) to the file.
     *
     * @param appointment The appointment object to save.
     * @return The saved appointment object.
     * @throws RuntimeException if the save operation fails.
     */
    public Appointment save(Appointment appointment) {
        File file = new File(FILE_PATH_STR);
        System.out.println("===== APPOINTMENT REPOSITORY: SAVE =====");
        if (appointment == null || appointment.getAppointmentId() == null) {
            System.err.println("!!! AppointmentRepository.save: Attempted to save null appointment or appointment with null ID.");
            throw new IllegalArgumentException("Cannot save null appointment or appointment with null ID");
        }
        System.out.println("Saving appointment ID: " + appointment.getAppointmentId() + " for Agent ID: " + appointment.getAgentId());

        try {
            List<Appointment> appointments = findAll(); // Read current state
            final String appointmentIdToSave = appointment.getAppointmentId();

            Optional<Appointment> existingApptOpt = appointments.stream()
                    .filter(a -> a != null && a.getAppointmentId() != null && a.getAppointmentId().equals(appointmentIdToSave))
                    .findFirst();

            if (existingApptOpt.isPresent()) {
                System.out.println(">>> AppointmentRepository.save: Updating existing appointment ID: " + appointmentIdToSave);
                // Update by replacing the old entry with the new one
                appointments = appointments.stream()
                        .map(a -> (a != null && a.getAppointmentId() != null && a.getAppointmentId().equals(appointmentIdToSave)) ? appointment : a)
                        .collect(Collectors.toList());
            } else {
                System.out.println(">>> AppointmentRepository.save: Adding new appointment ID: " + appointmentIdToSave);
                appointments.add(appointment);
            }

            writeToFile(appointments); // Write the modified list back
            System.out.println(">>> AppointmentRepository.save: Appointment save operation completed for ID: " + appointmentIdToSave);
            return appointment;

        } catch (Exception e) {
            System.err.println("!!! Error during save operation for appointment ID " + (appointment != null ? appointment.getAppointmentId() : "null") + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save appointment data for ID: " + (appointment != null ? appointment.getAppointmentId() : "null"), e);
        }
    }

    /**
     * Finds an appointment by its unique ID.
     *
     * @param appointmentId The ID of the appointment to find.
     * @return An Optional containing the Appointment if found, otherwise Optional.empty().
     */
    public Optional<Appointment> findById(String appointmentId) {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> AppointmentRepository - findById - Accessing path: " + file.getAbsolutePath());

        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            System.err.println("!!! AppointmentRepository.findById: Called with null or empty appointmentId.");
            return Optional.empty();
        }
        if (!file.exists() || !file.canRead()) {
            System.err.println("!!! AppointmentRepository - findById - File does not exist or cannot be read: " + file.getAbsolutePath());
            return Optional.empty();
        }

        System.out.println(">>> AppointmentRepository.findById: Attempting to find appointment by ID: " + appointmentId);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;

                // Use split with limit -1
                String[] parts = line.split("\\|", -1);

                if (parts.length > 0 && appointmentId.equals(parts[0])) {
                    System.out.println(">>> AppointmentRepository.findById: Found potential match for ID " + appointmentId + " on line " + lineNum);
                    Appointment appointment = parseAppointment(parts);
                    return Optional.ofNullable(appointment); // Use ofNullable
                }
            }
            System.out.println(">>> AppointmentRepository.findById: Appointment ID " + appointmentId + " not found after checking " + lineNum + " lines.");
        } catch (IOException e) {
            System.err.println("!!! IOException during findById for appointment ID " + appointmentId + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("!!! Unexpected error during findById for appointment ID " + appointmentId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Finds all appointments for a specific agent ID.
     *
     * @param agentId The ID of the agent whose appointments are needed.
     * @return A List of Appointments for that agent (may be empty).
     */
    public List<Appointment> findByAgentId(String agentId) {
        System.out.println(">>> AppointmentRepository - findByAgentId: " + agentId);
        if (agentId == null || agentId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Less efficient: reads all appointments then filters.
        // For larger scale, you might index or structure data differently.
        return findAll().stream()
                .filter(appt -> appt != null && agentId.equals(appt.getAgentId()))
                .collect(Collectors.toList());
    }

    /**
     * Finds all appointments for a specific client ID.
     *
     * @param clientId The ID of the client whose appointments are needed.
     * @return A List of Appointments for that client (may be empty).
     */
    public List<Appointment> findByClientId(String clientId) {
        System.out.println(">>> AppointmentRepository - findByClientId: " + clientId);
        if (clientId == null || clientId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return findAll().stream()
                .filter(appt -> appt != null && clientId.equals(appt.getClientId())) // Assumes getClientId() exists
                .collect(Collectors.toList());
    }


    /**
     * Gets all appointments by reading and parsing the entire data file.
     *
     * @return A list of all successfully parsed Appointment objects.
     */
    public List<Appointment> findAll() {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> AppointmentRepository - findAll - Accessing path: " + file.getAbsolutePath());
        List<Appointment> appointments = new ArrayList<>();

        if (!file.exists() || !file.canRead()) {
            System.err.println("!!! AppointmentRepository - findAll - File does not exist or cannot be read: " + file.getAbsolutePath());
            return appointments;
        }

        System.out.println(">>> AppointmentRepository.findAll: Attempting to find all appointments...");
        int lineNum = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;

                // Use split with limit -1
                String[] parts = line.split("\\|", -1);

                Appointment appointment = parseAppointment(parts);
                if (appointment != null) {
                    appointments.add(appointment);
                } else {
                    System.err.println("!!! AppointmentRepository.findAll: Skipping line " + lineNum + " due to parseAppointment returning null. Line: ["+line+"]");
                }
            }
            System.out.println(">>> AppointmentRepository.findAll: completed. Found " + appointments.size() + " valid appointment records in " + lineNum + " lines processed.");
        } catch (IOException e) {
            System.err.println("!!! IOException during findAll appointments: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("!!! Unexpected error during findAll appointments: " + e.getMessage());
            e.printStackTrace();
        }
        return appointments;
    }

    /**
     * Deletes an appointment by ID.
     *
     * @param appointmentId The ID of the appointment to delete.
     */
    public void deleteById(String appointmentId) {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> AppointmentRepository - deleteById - Accessing path: " + file.getAbsolutePath());
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            System.err.println("!!! AppointmentRepository.deleteById: Called with null or empty appointmentId.");
            return;
        }
        System.out.println(">>> AppointmentRepository.deleteById: Attempting to delete appointment ID: " + appointmentId);
        List<Appointment> appointments = findAll();
        int initialSize = appointments.size();

        List<Appointment> appointmentsAfterDeletion = appointments.stream()
                .filter(appt -> appt != null && appt.getAppointmentId() != null && !appt.getAppointmentId().equals(appointmentId))
                .collect(Collectors.toList());

        int finalSize = appointmentsAfterDeletion.size();

        if (initialSize > finalSize) {
            System.out.println(">>> AppointmentRepository.deleteById: Appointment ID " + appointmentId + " found and will be removed.");
            writeToFile(appointmentsAfterDeletion);
            System.out.println(">>> AppointmentRepository.deleteById: Wrote " + finalSize + " appointments back to file after deletion.");
        } else {
            System.out.println(">>> AppointmentRepository.deleteById: Appointment ID " + appointmentId + " not found for deletion.");
        }
    }


    /**
     * Parses a String array (from a split line) into an Appointment object.
     * Assumes 6 fields: appointmentId|agentId|clientId|dateTimeISOString|status|notes
     * Returns null if parsing fails or the input is invalid.
     *
     * @param parts The String array representing appointment fields.
     * @return An Appointment object if parsing succeeds, otherwise null.
     */
    private Appointment parseAppointment(String[] parts) {
        // CORRECTED: Expect 7 fields based on writeToFile format and constructor used
        int EXPECTED_FIELDS = 7;
        if (parts == null || parts.length != EXPECTED_FIELDS) {
            // Keep the check, but use the correct expected number
            System.err.println(">>> parseAppointment RETURNING NULL. Invalid appointment data: Expected " + EXPECTED_FIELDS + " fields, got " + (parts == null ? "null" : parts.length) + ". Parts: " + Arrays.toString(parts));
            return null;
        }

        try {
            String appointmentId = parts[0];
            String agentId = parts[1];
            String clientId = parts[2];
            String timeSlotId = parts[3];
            // Make sure 'formatter' is defined and accessible in this class/method
            LocalDateTime dateTime = LocalDateTime.parse(parts[4], formatter);
            Appointment.AppointmentStatus status = Appointment.AppointmentStatus.valueOf(parts[5].toUpperCase());
            String notes = parts[6]; // Now this access is safe after checking for 7 parts

            // Assuming your Appointment constructor matches this order and types:
            // public Appointment(String appointmentId, String agentId, String clientId, String timeSlotId, LocalDateTime dateTime, Appointment.AppointmentStatus status, String notes)
            return new Appointment(appointmentId, agentId, clientId, timeSlotId, dateTime, status, notes);
            // ** Double-check your actual Appointment constructor signature! **

        } catch (DateTimeParseException e) {
            // Corrected index for logging
            System.err.println(">>> parseAppointment RETURNING NULL due to DateTimeParseException: " + e.getMessage() + ". Input: " + (parts.length > 4 ? parts[4] : "N/A") + ". Parts: " + Arrays.toString(parts));
            return null;
        } catch (IllegalArgumentException e) {
            // Corrected index for logging
            System.err.println(">>> parseAppointment RETURNING NULL due to IllegalArgumentException (likely invalid Status Enum): " + e.getMessage() + ". Input: " + (parts.length > 5 ? parts[5] : "N/A") + ". Parts: " + Arrays.toString(parts));
            return null;
        } catch (Exception e) {
            System.err.println(">>> parseAppointment RETURNING NULL due to unexpected Exception: " + e.getMessage() + ". Parts: " + Arrays.toString(parts));
            e.printStackTrace(); // Good to keep for unexpected errors
            return null;
        }
    }
    /**
     * Writes the entire list of appointments to the file, overwriting existing content.
     *
     * @param appointments The list of appointments to write.
     */
    private void writeToFile(List<Appointment> appointments) {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> AppointmentRepository - writeToFile - Accessing path: " + file.getAbsolutePath());
        if (appointments == null) {
            System.err.println("!!! AppointmentRepository.writeToFile: Received null list of appointments to write.");
            appointments = new ArrayList<>();
        }
        System.out.println(">>> AppointmentRepository.writeToFile: Attempting to write " + appointments.size() + " appointments to file...");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) { // false = Overwrite
            int lineCount = 0;
            for (Appointment appt : appointments) {
                if (appt == null) {
                    System.err.println("!!! Skipping null appointment object during writeToFile.");
                    continue;
                }

                // Format: appointmentId|agentId|clientId|dateTimeISOString|status|notes
                String lineToWrite = String.format("%s|%s|%s|%s|%s|%s|%s",
                        appt.getAppointmentId(),
                        appt.getAgentId(),
                        appt.getClientId(),
                        (appt.getTimeSlotId() != null ? appt.getTimeSlotId() : ""),// Assuming getClientId() exists
                        appt.getDateTime().format(formatter), // Format date using formatter
                        appt.getStatus().name(), // Get enum constant name (e.g., "PENDING")
                        (appt.getNotes() != null ? appt.getNotes() : "") // Handle null notes
                );

                writer.write(lineToWrite);
                writer.newLine();
                lineCount++;
            }
            System.out.println(">>> AppointmentRepository.writeToFile: Finished writing " + lineCount + " appointment lines.");
        } catch (IOException e) {
            System.err.println("!!! IOException during writeToFile appointments: " + e.getMessage());
            e.printStackTrace();
            // throw new RuntimeException("Failed to write appointment data to file: " + file.getAbsolutePath(), e);
        } catch (Exception e) {
            System.err.println("!!! Unexpected error during writeToFile appointments: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
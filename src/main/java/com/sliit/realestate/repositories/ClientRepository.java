package com.sliit.realestate.repositories;


import com.sliit.realestate.models.Client; // Adjust import if needed
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ClientRepository {

    private static final String FILE_PATH_STR = "src/main/resources/clients.txt";
    // *** NEW: Define expected fields for the clients.txt format ***
    private static final int EXPECTED_CLIENT_FILE_PARTS = 4; // userId|favProps|searchPrefs|appts

    // Constructor and ensureFileExists remain the same as the corrected version previously provided...
    public ClientRepository() { /* ... include ensureFileExists call ... */ }
    private void ensureFileExists() { /* ... include the full ensureFileExists method ... */ }

    /**
     * Saves client-specific data (ID and lists). Assumes base User data is saved separately.
     */
    public Client save(Client client) {
        File file = new File(FILE_PATH_STR);
        System.out.println("===== CLIENT REPOSITORY (Specific Data): SAVE =====");
        System.out.println(">>> ClientRepository - save - Accessing path: " + file.getAbsolutePath());
        if (client == null || client.getUserId() == null) {
            System.err.println("!!! ClientRepository.save: Attempted to save null client or client with null ID.");
            throw new IllegalArgumentException("Cannot save null client or client with null ID");
        }
        // Log client-specific details being targeted for save if needed
        System.out.println("Saving/Updating client specific data for ID: " + client.getUserId());

        try {
            // Read only client-specific data currently in the file
            List<Client> clientSpecificDataList = findAllInternal(); // Use internal findAll to avoid infinite loop
            final String clientIdToSave = client.getUserId();

            Optional<Client> existingClientDataOpt = clientSpecificDataList.stream()
                    .filter(c -> c != null && c.getUserId() != null && c.getUserId().equals(clientIdToSave))
                    .findFirst();

            List<Client> listToWrite;
            if (existingClientDataOpt.isPresent()) {
                System.out.println(">>> ClientRepository.save: Updating existing client-specific data for ID: " + clientIdToSave);
                // Replace existing entry with the new client data (which only needs ID and lists for writing)
                listToWrite = clientSpecificDataList.stream()
                        .map(c -> (c != null && c.getUserId() != null && c.getUserId().equals(clientIdToSave)) ? client : c)
                        .collect(Collectors.toList());
            } else {
                System.out.println(">>> ClientRepository.save: Adding new client-specific data for ID: " + clientIdToSave);
                // Add the new client data to the existing list
                clientSpecificDataList.add(client);
                listToWrite = clientSpecificDataList;
            }

            writeToFile(listToWrite); // Write the modified list back
            System.out.println(">>> ClientRepository.save: Client-specific save operation completed for ID: " + clientIdToSave);
            // Return the input client object (which might be fully populated from service layer)
            return client;

        } catch (Exception e) {
            System.err.println("!!! Error during save operation for client-specific data ID " + (client != null ? client.getUserId() : "null") + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save client-specific data for ID: " + (client != null ? client.getUserId() : "null"), e);
        }
    }

    /**
     * Finds client-specific data by ID. Returns a Client object populated ONLY
     * with userId and the lists read from clients.txt.
     */
    public Optional<Client> findById(String clientId) {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> ClientRepository - findById (Specific Data) - Accessing path: " + file.getAbsolutePath());
        if (clientId == null || clientId.trim().isEmpty()) return Optional.empty();

        if (!file.exists() || !file.canRead()) {
            System.err.println("!!! ClientRepository - findById - File does not exist or cannot be read: " + file.getAbsolutePath());
            return Optional.empty();
        }

        System.out.println(">>> ClientRepository.findById: Attempting to find client-specific data by ID: " + clientId);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|", -1); // Keep trailing empty fields

                if (parts.length > 0 && clientId.equals(parts[0])) {
                    System.out.println(">>> ClientRepository.findById: Found potential match for ID " + clientId + " on line " + lineNum);
                    Client clientData = parseClient(parts); // Parse the specific data
                    return Optional.ofNullable(clientData);
                }
            }
            System.out.println(">>> ClientRepository.findById: Client ID " + clientId + " not found in specific data file.");
        } catch (IOException e) {
            System.err.println("!!! IOException during findById for client ID " + clientId + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("!!! Unexpected error during findById for client ID " + clientId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // findByEmail is no longer possible directly in this repository as email isn't stored here.
    // It must be done via UserService/UserRepository.

    /**
     * Gets all client-specific data records. Returns Client objects populated ONLY
     * with userId and the lists read from clients.txt.
     */
    public List<Client> findAll() {
        return findAllInternal(); // Use the internal helper
    }

    /** Helper for internal use to avoid loops within save */
    private List<Client> findAllInternal() {
        File file = new File(FILE_PATH_STR);
        // Log path access only if needed for debugging findAll specifically
        // System.out.println(">>> ClientRepository - findAllInternal - Accessing path: " + file.getAbsolutePath());
        List<Client> clients = new ArrayList<>();

        if (!file.exists() || !file.canRead()) {
            // Don't log error every time if file just doesn't exist yet
            // System.err.println("!!! ClientRepository - findAllInternal - File does not exist or cannot be read: " + file.getAbsolutePath());
            return clients;
        }

        int lineNum = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|", -1); // Keep trailing empty fields
                Client clientData = parseClient(parts); // Parse specific data

                if (clientData != null) {
                    clients.add(clientData);
                } else {
                    System.err.println("!!! ClientRepository.findAllInternal: Skipping line " + lineNum + " due to parseClient returning null. Line: ["+line+"]");
                }
            }
        } catch (IOException e) {
            System.err.println("!!! IOException during findAllInternal clients: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("!!! Unexpected error during findAllInternal clients: " + e.getMessage());
            e.printStackTrace();
        }
        // System.out.println(">>> ClientRepository.findAllInternal: completed. Found " + clients.size() + " valid client-specific records.");
        return clients;
    }


    /**
     * Parses a String array (from clients.txt) into a Client object containing
     * ONLY the userId and client-specific lists. Other user fields are null/default.
     * Assumes 4 fields: userId|favProps|searchPrefs|appts
     */
    private Client parseClient(String[] parts) {
        // *** MODIFIED: Check for exactly 4 pipe-separated parts ***
        if (parts == null || parts.length != EXPECTED_CLIENT_FILE_PARTS) {
            System.err.println(">>> parseClient RETURNING NULL. Invalid client-specific data: Expected " + EXPECTED_CLIENT_FILE_PARTS + " fields, got " + (parts == null ? "null" : parts.length) + ". Parts: " + Arrays.toString(parts));
            return null;
        }

        try {
            // *** MODIFIED: Only parse fields present in clients.txt ***
            String userId = parts[0]; // Required key

            // Use new ArrayList<>(...) for mutability
            List<String> favoriteProperties = parts[1].isEmpty() ? new ArrayList<>() :
                    new ArrayList<>(Arrays.asList(parts[1].split(";")));
            List<String> searchPreferences = parts[2].isEmpty() ? new ArrayList<>() :
                    new ArrayList<>(Arrays.asList(parts[2].split(";")));
            List<String> appointmentHistory = parts[3].isEmpty() ? new ArrayList<>() :
                    new ArrayList<>(Arrays.asList(parts[3].split(";")));

            // Create a Client object using a constructor that takes these specific fields.
            // We'll use the comprehensive one, passing nulls for base user fields (except ID).
            // The service layer will provide the full user details later.
            Client clientData = new Client(
                    userId, null, null, null, null, null, // Base user fields set to null (except ID)
                    favoriteProperties, searchPreferences, appointmentHistory
            );
            // Note: 'active' status isn't stored here, comes from User record.

            // System.out.println(">>> parseClient RETURNING Client Specific Data object for ID: " + userId);
            return clientData;

        } catch (Exception e) { // Catch any parsing exceptions
            System.err.println(">>> parseClient RETURNING NULL due to unexpected Exception: " + e.getMessage() + ". Parts: " + Arrays.toString(parts));
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Deletes a client's specific data record by ID.
     */
    public void deleteById(String clientId) {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> ClientRepository - deleteById (Specific Data) - Accessing path: " + file.getAbsolutePath());
        if (clientId == null || clientId.trim().isEmpty()) {
            System.err.println("!!! ClientRepository.deleteById: Called with null or empty clientId.");
            return;
        }
        System.out.println(">>> ClientRepository.deleteById: Attempting to delete client-specific data for ID: " + clientId);
        List<Client> clients = findAllInternal(); // Use internal to avoid loop
        int initialSize = clients.size();

        List<Client> clientsAfterDeletion = clients.stream()
                .filter(client -> client != null && client.getUserId() != null && !client.getUserId().equals(clientId))
                .collect(Collectors.toList());

        int finalSize = clientsAfterDeletion.size();

        if (initialSize > finalSize) {
            System.out.println(">>> ClientRepository.deleteById: Client-specific data for ID " + clientId + " found and will be removed.");
            writeToFile(clientsAfterDeletion);
            System.out.println(">>> ClientRepository.deleteById: Wrote " + finalSize + " client-specific records back to file after deletion.");
        } else {
            System.out.println(">>> ClientRepository.deleteById: Client-specific data for ID " + clientId + " not found for deletion.");
        }
    }

    /**
     * Writes the list of client-specific data records to the file, overwriting.
     */
    private void writeToFile(List<Client> clients) {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> ClientRepository - writeToFile (Specific Data) - Accessing path: " + file.getAbsolutePath());
        if (clients == null) {
            clients = new ArrayList<>();
        }
        System.out.println(">>> ClientRepository.writeToFile: Attempting to write " + clients.size() + " client-specific records to file...");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) { // false=Overwrite
            int lineCount = 0;
            for (Client client : clients) {
                if (client == null || client.getUserId() == null) { // Need at least userId
                    System.err.println("!!! Skipping null client or client with null ID during writeToFile.");
                    continue;
                }

                // Format lists using semicolon separator, handle null lists
                String favoriteProps = (client.getFavoriteProperties() != null) ? String.join(";", client.getFavoriteProperties()) : "";
                String searchPrefs = (client.getSearchPreferences() != null) ? String.join(";", client.getSearchPreferences()) : "";
                String appointments = (client.getAppointmentHistory() != null) ? String.join(";", client.getAppointmentHistory()) : "";

                // *** MODIFIED: Write only userId and the specific lists ***
                // Format: userId|favProps|searchPrefs|appts
                String lineToWrite = String.format("%s|%s|%s|%s",
                        client.getUserId(), // Key
                        favoriteProps,
                        searchPrefs,
                        appointments
                );

                // System.out.println(">>> WRITE Client Specific Line: [" + lineToWrite + "]");
                writer.write(lineToWrite);
                writer.newLine();
                lineCount++;
            }
            System.out.println(">>> ClientRepository.writeToFile: Finished writing " + lineCount + " client-specific lines.");
        } catch (IOException e) {
            System.err.println("!!! IOException during writeToFile client-specific data: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("!!! Unexpected error during writeToFile client-specific data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
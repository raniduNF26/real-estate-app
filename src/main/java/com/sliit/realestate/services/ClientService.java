package com.sliit.realestate.services;



import com.sliit.realestate.models.Client; // Adjust imports
import com.sliit.realestate.models.User;
import com.sliit.realestate.repositories.ClientRepository;
import com.sliit.realestate.repositories.UserRepository; // May need this directly or via UserService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // If needed here
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserService userService;



    /**
     * Creates a new client, saving base data via UserService and specific data via ClientRepository.
     */
    public Client createClient(Client client) {
        System.out.println("ClientService: Creating client with username: " + client.getUsername());
        if (client == null) {
            throw new IllegalArgumentException("Cannot create null client");
        }
        try {
            // 1. Save base user data (gets ID assigned, password hashed)
            // Pass the Client object (as it extends User) to userService.createUser
            User savedUserBase = userService.createUser(client);
            // Ensure the input client object has the generated ID and potentially hashed password updated
            client.setUserId(savedUserBase.getUserId());
            client.setPassword(savedUserBase.getPassword()); // Get hashed password back

            System.out.println("User base saved via UserService with userId: " + client.getUserId());

            // 2. Save client-specific data (ID + lists) using ClientRepository
            // The client object passed now has the correct ID.
            Client savedClientSpecific = clientRepository.save(client);
            System.out.println("Client-specific data saved via ClientRepository successfully");

            // Return the client object which now contains both merged base data (from input)
            // and potentially updated lists (though save doesn't modify lists here)
            return client;
        } catch (Exception e) {
            System.err.println("!!! Error in createClient: " + e.getMessage());
            e.printStackTrace();
            // Consider if cleanup is needed (e.g., delete user if client save fails)
            throw e; // Re-throw
        }
    }


    /**
     * Gets a COMPLETE Client object by merging User data and Client-specific data.
     *
     * @param username The username of the client.
     * @return Optional containing the complete Client object if found, otherwise empty.
     */
    public Optional<Client> getCompleteClientByUsername(String username) {
        System.out.println(">>> ClientService: Getting complete client for username: " + username);
        // 1. Get base user data
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty() || !"CLIENT".equals(userOpt.get().getRole())) {
            System.out.println(">>> ClientService: Base user not found or not a CLIENT for username: " + username);
            return Optional.empty();
        }
        User baseUser = userOpt.get();
        System.out.println(">>> ClientService: Found base user ID: " + baseUser.getUserId());

        // 2. Get client-specific data (lists)
        Optional<Client> clientSpecificDataOpt = clientRepository.findById(baseUser.getUserId());
        if (clientSpecificDataOpt.isEmpty()) {
            System.err.println("!!! ClientService: Found base user but NO specific client data for ID: " + baseUser.getUserId() + ". Data might be inconsistent.");
            // Decide how to handle - return Optional.empty()? Or create Client with empty lists?
            // Let's create with empty lists for now, assuming user exists but client file entry is missing/corrupt
            clientSpecificDataOpt = Optional.of(new Client(baseUser.getUserId(), null, null, null, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        }
        Client clientSpecificData = clientSpecificDataOpt.get(); // Contains ID and lists

        // 3. Merge into a complete Client object
        // Use the comprehensive Client constructor
        Client completeClient = new Client(
                baseUser.getUserId(),
                baseUser.getUsername(),
                baseUser.getPassword(), // Hash
                baseUser.getFullName(),
                baseUser.getEmail(),
                baseUser.getPhone(),
                clientSpecificData.getFavoriteProperties(), // List from ClientRepository result
                clientSpecificData.getSearchPreferences(), // List from ClientRepository result
                clientSpecificData.getAppointmentHistory() // List from ClientRepository result
        );
        completeClient.setActive(baseUser.isActive()); // Set active status from User record

        System.out.println(">>> ClientService: Successfully merged User and Client data for: " + username);
        return Optional.of(completeClient);
    }


    /**
     * Gets a COMPLETE Client object by merging User data and Client-specific data.
     *
     * @param userId The ID of the client.
     * @return Optional containing the complete Client object if found, otherwise empty.
     */
    public Optional<Client> getCompleteClientById(String userId) {
        System.out.println(">>> ClientService: Getting complete client for ID: " + userId);
        // 1. Get base user data
        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty() || !"CLIENT".equals(userOpt.get().getRole())) {
            System.out.println(">>> ClientService: Base user not found or not a CLIENT for ID: " + userId);
            return Optional.empty();
        }
        User baseUser = userOpt.get();

        // 2. Get client-specific data (lists)
        Optional<Client> clientSpecificDataOpt = clientRepository.findById(baseUser.getUserId());
        if (clientSpecificDataOpt.isEmpty()) {
            System.err.println("!!! ClientService: Found base user but NO specific client data for ID: " + baseUser.getUserId() + ". Data might be inconsistent.");
            clientSpecificDataOpt = Optional.of(new Client(baseUser.getUserId(), null, null, null, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        }
        Client clientSpecificData = clientSpecificDataOpt.get(); // Contains ID and lists

        // 3. Merge into a complete Client object
        Client completeClient = new Client(
                baseUser.getUserId(), baseUser.getUsername(), baseUser.getPassword(),
                baseUser.getFullName(), baseUser.getEmail(), baseUser.getPhone(),
                clientSpecificData.getFavoriteProperties(),
                clientSpecificData.getSearchPreferences(),
                clientSpecificData.getAppointmentHistory()
        );
        completeClient.setActive(baseUser.isActive());

        System.out.println(">>> ClientService: Successfully merged User and Client data for ID: " + userId);
        return Optional.of(completeClient);
    }


    /**
     * Gets all *complete* Client objects. Reads both files. Potentially inefficient.
     * @return List of complete Client objects.
     */
    public List<Client> getAllCompleteClients() {
        System.out.println(">>> ClientService: Getting all complete clients...");
        List<User> baseUsers = userService.getAllUsers().stream()
                .filter(u -> "CLIENT".equals(u.getRole()))
                .toList(); // Get only CLIENT base users

        List<Client> completeClients = new ArrayList<>();
        for (User baseUser : baseUsers) {
            // For each client user, try to find specific data and merge
            Optional<Client> clientSpecificDataOpt = clientRepository.findById(baseUser.getUserId());
            Client clientSpecificData = clientSpecificDataOpt.orElseGet(() -> {
                // Create default specific data if missing in clients.txt
                System.err.println("!!! ClientService: NO specific client data found for ID: " + baseUser.getUserId() + " during getAll. Creating default.");
                return new Client(baseUser.getUserId(), null, null, null, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            });

            // Merge
            Client completeClient = new Client(
                    baseUser.getUserId(), baseUser.getUsername(), baseUser.getPassword(),
                    baseUser.getFullName(), baseUser.getEmail(), baseUser.getPhone(),
                    clientSpecificData.getFavoriteProperties(),
                    clientSpecificData.getSearchPreferences(),
                    clientSpecificData.getAppointmentHistory()
            );
            completeClient.setActive(baseUser.isActive());
            completeClients.add(completeClient);
        }
        System.out.println(">>> ClientService: Returning " + completeClients.size() + " complete clients.");
        return completeClients;
    }


    /**
     * Authenticates a client by checking base user credentials AND role.
     */
    public boolean authenticate(String username, String password) {
        System.out.println("ClientService authenticate method called for: " + username);
        // Use UserService to check credentials and active status
        if (userService.authenticate(username, password)) {
            Optional<User> userOpt = userService.getUserByUsername(username);
            // *** CORRECTED CHECK: Check role string ***
            return userOpt.isPresent()
                    && userOpt.get().isActive() // Redundant if userService.authenticate checks it, but safe
                    && "CLIENT".equals(userOpt.get().getRole());
        }
        System.out.println("ClientService authenticate returning false for: " + username);
        return false;
    }


    /**
     * Updates client data. Updates base user data via UserService
     * and client-specific data via ClientRepository.
     * Assumes the input client object is fully populated with desired changes.
     */
    public Client updateClient(Client client) {
        System.out.println(">>> ClientService: Updating client ID: " + client.getUserId());
        // 1. Update base user details (handles password hashing if needed)
        User updatedUserBase = userService.updateUser(client);
        // Ensure the client object reflects any changes from userService (like maybe new password hash)
        client.setPassword(updatedUserBase.getPassword());

        // 2. Save the updated client-specific details
        // The input 'client' object now has the correct ID and updated lists/etc.
        Client savedClientSpecific = clientRepository.save(client);
        System.out.println(">>> ClientService: Client update completed for ID: " + client.getUserId());

        // Return the fully updated client object passed in (now reflecting saved state)
        return client;
    }

    /**
     * Deletes a client from both user and client repositories.
     */
    public void deleteClient(String clientId) {
        System.out.println(">>> ClientService: Deleting client ID: " + clientId);
        userService.deleteUser(clientId); // Delete from users.txt
        clientRepository.deleteById(clientId); // Delete from clients.txt
        System.out.println(">>> ClientService: Client deletion completed for ID: " + clientId);
    }

    // --- Methods to modify client-specific lists ---
    // These methods now need to fetch the complete client, modify, then save.

    public void addFavoriteProperty(String clientId, String propertyId) {
        System.out.println(">>> ClientService: Adding fav prop " + propertyId + " for client " + clientId);
        Optional<Client> clientOpt = getCompleteClientById(clientId); // Get complete client
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            // Modify the list on the retrieved object
            // Need Client model to have a *real* adder method, not one that operates on a copy
            // Let's assume Client has: internalAddFavoriteProperty(String id)
            // client.internalAddFavoriteProperty(propertyId); // Modify the actual list
            // Or, get the list, modify, and set it back (less ideal)
            List<String> favs = new ArrayList<>(client.getFavoriteProperties()); // Get copy
            if (!favs.contains(propertyId)) { // Avoid duplicates
                favs.add(propertyId);
                client.setFavoriteProperties(favs); // Assumes Client has setFavoriteProperties(List<String> list)
            }

            clientRepository.save(client); // Save the updated client-specific data
        } else {
            System.err.println("!!! ClientService: Client not found for adding favorite property: " + clientId);
            throw new RuntimeException("Client not found with ID: " + clientId);
        }
    }

    public void removeFavoriteProperty(String clientId, String propertyId) {
        System.out.println(">>> ClientService: Removing fav prop " + propertyId + " for client " + clientId);
        Optional<Client> clientOpt = getCompleteClientById(clientId);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            List<String> favs = new ArrayList<>(client.getFavoriteProperties());
            boolean removed = favs.remove(propertyId);
            if (removed) {
                client.setFavoriteProperties(favs); // Assumes setter exists
                clientRepository.save(client);
            }
        } else {
            System.err.println("!!! ClientService: Client not found for removing favorite property: " + clientId);
            throw new RuntimeException("Client not found with ID: " + clientId);
        }
    }



    public void addAppointmentToHistory(String clientId, String appointmentId) { // Renamed for clarity
        System.out.println(">>> ClientService: Adding appt " + appointmentId + " to history for client " + clientId);
        Optional<Client> clientOpt = getCompleteClientById(clientId);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            List<String> history = new ArrayList<>(client.getAppointmentHistory());
            if (!history.contains(appointmentId)) { // Avoid duplicates
                history.add(appointmentId);
                client.setAppointmentHistory(history); // Assumes setter exists
            }
            clientRepository.save(client); // Save updated lists
        } else {
            System.err.println("!!! ClientService: Client not found for adding appointment history: " + clientId);
            throw new RuntimeException("Client not found with ID: " + clientId);
        }
    }

    public void setSearchPreferences(String clientId, List<String> preferences) {
        System.out.println(">>> ClientService: Setting search prefs for client " + clientId);
        Optional<Client> clientOpt = getCompleteClientById(clientId);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            client.setSearchPreferences(preferences); // Assumes setter exists
            clientRepository.save(client); // Save updated lists
        } else {
            System.err.println("!!! ClientService: Client not found for setting search prefs: " + clientId);
            throw new RuntimeException("Client not found with ID: " + clientId);
        }
    }


}
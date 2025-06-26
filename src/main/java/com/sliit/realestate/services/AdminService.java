package com.sliit.realestate.services; // Adjust package if needed

// Import necessary models and services/repositories
import com.sliit.realestate.models.Appointment;
import com.sliit.realestate.models.Client;
import com.sliit.realestate.models.Agent;
import com.sliit.realestate.models.Property;
import com.sliit.realestate.models.User;
import com.sliit.realestate.repositories.AppointmentRepository;
import com.sliit.realestate.repositories.PropertyRepository;
// No AdminRepository needed
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminService {

    // Inject all services/repositories needed to fetch data for admin views
    @Autowired
    private UserService userService; // Handles User operations

    @Autowired
    private AgentService agentService; // To get complete Agent details

    @Autowired
    private ClientService clientService; // To get complete Client details

    @Autowired
    private PropertyService propertyService; // To get Property details

    @Autowired
    private AppointmentRepository appointmentRepository; // To get Appointment details (or use AgentService)


    /**
     * Authenticates a user based on username/password and verifies they have ADMIN role.
     */
    public boolean authenticate(String username, String password) {
        System.out.println("AdminService authenticate method called for: " + username);
        // Use UserService to check credentials and active status
        if (userService.authenticate(username, password)) {
            Optional<User> userOpt = userService.getUserByUsername(username);
            // Check if user exists, is active, AND has the ADMIN role string
            return userOpt.isPresent()
                    && userOpt.get().isActive()
                    && "ADMIN".equals(userOpt.get().getRole()); // Check role string
        }
        System.out.println("AdminService authenticate returning false for: " + username);
        return false;
    }

    // --- Methods for Admin User Management ---

    public List<User> getAllUsers() {
        System.out.println(">>> AdminService: Getting all users.");
        return userService.getAllUsers(); // Get all users (includes Admins, Agents, Clients)
    }

    public Optional<User> getUserById(String userId) {
        System.out.println(">>> AdminService: Getting user by ID: " + userId);
        return userService.getUserById(userId);
    }

    // Example: Activate User
    public boolean activateUser(String userId) {
        System.out.println(">>> AdminService: Activating user ID: " + userId);
        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isActive()) {
                user.setActive(true);
                try {
                    userService.updateUser(user); // Save updated user
                    return true;
                } catch (Exception e) {
                    System.err.println("!!! AdminService: Error updating user during activation for ID " + userId + ": " + e.getMessage());
                    return false;
                }
            }
            return true; // Already active
        }
        return false; // User not found
    }

    // Example: Deactivate User
    public boolean deactivateUser(String userId) {
        System.out.println(">>> AdminService: Deactivating user ID: " + userId);
        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Optional: Add check to prevent deactivating the last admin?
            if (user.isActive()) {
                user.setActive(false);
                try {
                    userService.updateUser(user); // Save updated user
                    return true;
                } catch (Exception e) {
                    System.err.println("!!! AdminService: Error updating user during deactivation for ID " + userId + ": " + e.getMessage());
                    return false;
                }
            }
            return true; // Already inactive
        }
        return false; // User not found
    }

    // Example: Delete User (Use with caution!)
    public void deleteUser(String userId) {
        System.out.println(">>> AdminService: Deleting user ID: " + userId);
        // Optional: Add checks (don't delete self, don't delete last admin)
        userService.deleteUser(userId); // This should handle deleting from users.txt AND related agent/client data if implemented correctly in UserService/dependent services
    }


    // --- Methods to Fetch Data for Admin Views ---

    public List<Agent> getAllAgents() {
        System.out.println(">>> AdminService: Getting all agents.");
        // Use the service method that returns complete agents
        return agentService.getAllAgentsSortedByRating(); // Or a new unsorted method if preferred
    }

    public List<Client> getAllClients() {
        System.out.println(">>> AdminService: Getting all clients.");
        // Use the service method that returns complete clients
        return clientService.getAllCompleteClients(); // Requires this method in ClientService
    }

    public List<Property> getAllProperties() {
        System.out.println(">>> AdminService: Getting all properties.");
        return propertyService.getAllProperties();
    }

    public List<Appointment> getAllAppointments() {
        System.out.println(">>> AdminService: Getting all appointments.");
        return appointmentRepository.findAll(); // Fetch directly from repo
    }

    // Methods to delete Properties or Appointments would call the respective service/repo delete methods
    public void deleteProperty(String propertyId) {
        System.out.println(">>> AdminService: Deleting property ID: " + propertyId);
        // Need to also potentially remove from agent managed list and client favorites! More complex.
        // Start by just deleting the property record:
        propertyService.deleteProperty(propertyId);
    }

    public void deleteAppointment(String appointmentId) {
        System.out.println(">>> AdminService: Deleting appointment ID: " + appointmentId);
        // Need to potentially remove from client history?
        appointmentRepository.deleteById(appointmentId);
    }

}
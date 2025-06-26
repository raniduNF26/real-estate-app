package com.sliit.realestate.services;

import com.sliit.realestate.models.Agent;
import com.sliit.realestate.models.Appointment;
import com.sliit.realestate.models.User;
import com.sliit.realestate.repositories.AgentRepository;
import com.sliit.realestate.repositories.AppointmentRepository;
import com.sliit.realestate.util.AgentBST;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgentService {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // AgentBST is initialized once when the service is created
    private AgentBST agentBST = new AgentBST();

    // Constructor to inject dependencies and initialize the BST
    // Spring will automatically call this constructor due to @Autowired
    public AgentService(AgentRepository agentRepository, ClientService clientService, UserService userService, AppointmentRepository appointmentRepository, PasswordEncoder passwordEncoder) {
        this.agentRepository = agentRepository;
        this.clientService = clientService;
        this.userService = userService;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
        // Initialize BST immediately upon service creation
        initializeBST();
        System.out.println("AgentService: BST initialized with agents from repository.");
    }

    // Initialize BST with all agents from repository
    // This method now clears the BST first to ensure fresh data
    private void initializeBST() {
        agentBST.clear(); // Clear existing BST content to ensure fresh load
        List<Agent> agents = getAllCompleteAgents(); // Fetch complete agents to populate BST
        for (Agent agent : agents) {
            agentBST.insert(agent);
        }
    }

    // Create a new agent
    public Agent createAgent(Agent agent) {
        System.out.println("AgentService: Creating agent with username: " + agent.getUsername());

        try {
            // Use UserService to handle common user creation tasks (ID generation, password hashing)
            User savedUser = userService.createUser(agent);
            agent.setUserId(savedUser.getUserId());
            agent.setPassword(savedUser.getPassword()); // Already hashed by UserService

            System.out.println("User saved via UserService with userId: " + agent.getUserId());

            // Now save the agent-specific details
            System.out.println("Saving agent to repository");
            Agent savedAgent = agentRepository.save(agent);
            System.out.println("Agent saved to repository successfully");

            // Update BST: The insert method in AgentBST handles both new insertions and updates
            System.out.println("Updating BST");
            agentBST.insert(savedAgent);
            System.out.println("BST updated successfully");

            return savedAgent;
        } catch (Exception e) {
            System.err.println("Error in createAgent: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to propagate upward
        }
    }

    public Optional<Agent> getCompleteAgentById(String agentId) {
        System.out.println(">>> AgentService: Getting complete agent for ID: " + agentId);
        if (agentId == null || agentId.trim().isEmpty()) { return Optional.empty(); }

        // 1. Get base user data
        Optional<User> userOpt = userService.getUserById(agentId); // Use getUserById
        if (userOpt.isEmpty() || !"AGENT".equals(userOpt.get().getRole())) {
            System.out.println(">>> AgentService: Base user not found or not an AGENT for ID: " + agentId);
            return Optional.empty();
        }
        User baseUser = userOpt.get();
        System.out.println(">>> AgentService: Found base user: " + baseUser.getUsername());

        // 2. Get agent-specific data
        Optional<Agent> agentSpecificDataOpt = agentRepository.findById(baseUser.getUserId());
        if (agentSpecificDataOpt.isEmpty()) {
            System.err.println("!!! AgentService: Found base user but NO specific agent data for ID: " + baseUser.getUserId());
            // Return empty if agent-specific data is missing, indicating inconsistency
            return Optional.empty();
        }
        Agent agentSpecificData = agentSpecificDataOpt.get(); // Contains ID + agent lists/details

        // 3. Merge into a complete Agent object
        Agent completeAgent = new Agent(
                baseUser.getUserId(), baseUser.getUsername(), baseUser.getPassword(),
                baseUser.getFullName(), baseUser.getEmail(), baseUser.getPhone(),
                agentSpecificData.getLicenseNumber(), agentSpecificData.getSpecialization(),
                agentSpecificData.getLocation(), agentSpecificData.getAverageRating(),
                agentSpecificData.getTotalRatings(), agentSpecificData.getManagedProperties(),
                agentSpecificData.getAvailability()
        );
        completeAgent.setActive(baseUser.isActive());
        // Set additional fields that might not be in the constructor
        completeAgent.setProfileImageUrl(agentSpecificData.getProfileImageUrl());
        completeAgent.setExperience(agentSpecificData.getExperience());
        completeAgent.setBio(agentSpecificData.getBio());


        // 4. Fetch and set Appointments
        try {
            List<Appointment> appointments = appointmentRepository.findByAgentId(completeAgent.getUserId());
            completeAgent.setAppointments(appointments); // Assumes Agent class has setAppointments(List<Appointment>)
            System.out.println(">>> AgentService: Fetched " + appointments.size() + " appointments for agent ID: " + agentId);
        } catch (Exception e) {
            System.err.println("!!! AgentService: Failed to fetch appointments for agent " + completeAgent.getUserId() + ": " + e.getMessage());
            completeAgent.setAppointments(new ArrayList<>()); // Set empty list on error
            e.printStackTrace();
        }

        System.out.println(">>> AgentService: Successfully created complete Agent object for ID: " + agentId);
        return Optional.of(completeAgent);
    }

    public Agent getAgentByUsername(String username) {
        System.out.println("Looking for agent with username: " + username);

        // 1. Get the base User details
        Optional<User> userOpt = userService.getUserByUsername(username);
        System.out.println("User found: " + userOpt.isPresent());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("User class: " + user.getClass().getName());

            // 2. Check if the role is AGENT
            if ("AGENT".equals(user.getRole())) {
                // 3. Fetch agent-specific data using the User's ID
                Optional<Agent> agentSpecificDataOpt = agentRepository.findById(user.getUserId());

                if (agentSpecificDataOpt.isPresent()) {
                    // 4. **Combine the data**
                    Agent agentSpecificData = agentSpecificDataOpt.get();

                    // Create a new, complete Agent object using the comprehensive constructor
                    Agent completeAgent = new Agent(
                            user.getUserId(),            // From User object
                            user.getUsername(),          // From User object
                            user.getPassword(),          // From User object (already hashed)
                            user.getFullName(),          // From User object
                            user.getEmail(),             // From User object
                            user.getPhone(),             // From User object
                            agentSpecificData.getLicenseNumber(), // From Agent-specific data
                            agentSpecificData.getSpecialization(),// From Agent-specific data
                            agentSpecificData.getLocation(),      // From Agent-specific data
                            agentSpecificData.getAverageRating(), // From Agent-specific data
                            agentSpecificData.getTotalRatings(),  // From Agent-specific data
                            agentSpecificData.getManagedProperties(), // From Agent-specific data
                            agentSpecificData.getAvailability()     // From Agent-specific data
                    );
                    // Set fields not handled by constructor (like active status)
                    completeAgent.setActive(user.isActive()); // From User object
                    completeAgent.setProfileImageUrl(agentSpecificData.getProfileImageUrl());
                    completeAgent.setExperience(agentSpecificData.getExperience());
                    completeAgent.setBio(agentSpecificData.getBio());

                    try {
                        List<Appointment> appointments = appointmentRepository.findByAgentId(completeAgent.getUserId());
                        completeAgent.setAppointments(appointments); // Use the setter in Agent class
                        System.out.println(">>> AgentService: Fetched " + (appointments == null ? 0 : appointments.size()) + " appointments for agent: " + username);
                    } catch (Exception e) {
                        System.err.println("!!! AgentService: Failed to fetch appointments for agent " + completeAgent.getUserId() + ": " + e.getMessage());
                        completeAgent.setAppointments(new ArrayList<>()); // Set empty list on error
                        e.printStackTrace();
                    }

                    // Return the fully populated agent object
                    System.out.println("Successfully created complete Agent object for: " + username);
                    return completeAgent;
                } else {
                    System.err.println("Agent-specific data not found in agents.txt for userId: " + user.getUserId());
                }
            } else {
                System.out.println("User role is not AGENT: " + user.getRole());
            }
        }

        System.out.println("No agent found or user is not an agent for username: " + username);
        return null; // Return null if user not found, not an agent, or agent data missing
    }

    // Get all agents sorted by rating
    public List<Agent> getAllAgentsSortedByRating() {
        System.out.println("Fetching all agents for sorting...");
        // Get all COMPLETE agents
        List<Agent> completeAgents = getAllCompleteAgents();
        System.out.println("Found " + completeAgents.size() + " complete agent objects.");

        // 3. Sort the complete list using the AgentBST's sortByRating (which uses selection sort)
        List<Agent> sortedAgents = AgentBST.sortByRating(completeAgents);
        System.out.println("Returning " + sortedAgents.size() + " sorted complete agents.");
        return sortedAgents;
    }

    public List<Agent> getAgentsFilteredAndSorted(Double minRating, String searchTerm) {
        System.out.println(">>> AgentService: Fetching agents. minRating=" + minRating + ", searchTerm=" + searchTerm);

        List<Agent> baseAgentsForFilter;
        // Optimization: If a search term is provided, try efficient location search first
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            List<Agent> agentsByLocation = agentBST.findByLocation(searchTerm);
            if (!agentsByLocation.isEmpty()) {
                baseAgentsForFilter = agentsByLocation;
                System.out.println(">>> AgentService: Used efficient location search for '" + searchTerm + "'. Found " + agentsByLocation.size() + " agents.");
            } else {
                // Fallback to all agents if no direct location match or search term isn't a location
                baseAgentsForFilter = getAllCompleteAgents();
                System.out.println(">>> AgentService: Falling back to full scan as '" + searchTerm + "' not a direct location match or no agents found for it efficiently.");
            }
        } else {
            baseAgentsForFilter = getAllCompleteAgents();
        }

        // Prepare search term for case-insensitive check
        final String lowerSearchTerm = (searchTerm != null && !searchTerm.trim().isEmpty()) ? searchTerm.trim().toLowerCase() : null;

        // 2. Apply Filters using Streams
        List<Agent> filteredAgents = baseAgentsForFilter.stream()
                .filter(agent -> agent != null) // Ensure agent object is not null
                // Apply rating filter
                .filter(agent -> minRating == null || agent.getAverageRating() >= minRating)
                // Apply search term filter (if searchTerm exists) across multiple fields
                .filter(agent -> lowerSearchTerm == null ||
                        (agent.getFullName() != null && agent.getFullName().toLowerCase().contains(lowerSearchTerm)) ||
                        (agent.getLocation() != null && agent.getLocation().toLowerCase().contains(lowerSearchTerm)) ||
                        (agent.getSpecialization() != null && agent.getSpecialization().toLowerCase().contains(lowerSearchTerm)) ||
                        (agent.getBio() != null && agent.getBio().toLowerCase().contains(lowerSearchTerm))
                )
                .collect(Collectors.toList()); // Collect intermediate filtered list

        System.out.println(">>> AgentService: Filtered down to " + filteredAgents.size() + " agents.");

        // 3. Sort the FINAL filtered list using the AgentBST's sortByRating (selection sort)
        List<Agent> sortedAndFilteredAgents = AgentBST.sortByRating(filteredAgents);

        System.out.println(">>> AgentService: Returning " + sortedAndFilteredAgents.size() + " filtered and sorted agents.");
        return sortedAndFilteredAgents;
    }


    // Helper method to fetch and merge all complete agent data
    private List<Agent> getAllCompleteAgents() {
        List<Agent> incompleteAgents = agentRepository.findAll();
        List<Agent> completeAgents = new ArrayList<>();
        for (Agent incompleteAgent : incompleteAgents) {
            Optional<User> userOpt = userService.getUserById(incompleteAgent.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Agent completeAgent = new Agent(user.getUserId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhone(),
                        incompleteAgent.getLicenseNumber(),
                        incompleteAgent.getSpecialization(),
                        incompleteAgent.getLocation(),
                        incompleteAgent.getAverageRating(),
                        incompleteAgent.getTotalRatings(),
                        incompleteAgent.getManagedProperties(),
                        incompleteAgent.getAvailability());
                completeAgent.setActive(user.isActive());
                // Set additional fields
                completeAgent.setProfileImageUrl(incompleteAgent.getProfileImageUrl());
                completeAgent.setExperience(incompleteAgent.getExperience());
                completeAgent.setBio(incompleteAgent.getBio());

                try {
                    List<Appointment> appointments = appointmentRepository.findByAgentId(completeAgent.getUserId());
                    completeAgent.setAppointments(appointments);
                } catch (Exception e) {
                    System.err.println("!!! AgentService: Failed to fetch appointments for agent " + completeAgent.getUserId() + " in getAllCompleteAgents: " + e.getMessage());
                    completeAgent.setAppointments(new ArrayList<>());
                }
                completeAgents.add(completeAgent);
            } else {
                System.err.println("Could not find User data for agentId: " + incompleteAgent.getUserId() + " while building full list in getAllCompleteAgents.");
            }
        }
        return completeAgents;
    }

    public boolean cancelClientAppointment(String appointmentId, String clientId) {
        System.out.println(">>> AgentService: Attempting cancellation for Appt ID: " + appointmentId + " by Client ID: " + clientId);
        Optional<Appointment> apptOpt = appointmentRepository.findById(appointmentId);

        if (apptOpt.isPresent()) {
            Appointment appointment = apptOpt.get();

            // **Security/Logic Check:** Ensure the client requesting cancel owns this appointment
            if (!clientId.equals(appointment.getClientId())) {
                System.err.println("!!! AgentService: Client " + clientId + " attempted to cancel appointment " + appointmentId + " belonging to client " + appointment.getClientId());
                throw new SecurityException("Client is not authorized to cancel this appointment.");
            }

            // Check if already cancelled or completed
            if (appointment.getStatus() == Appointment.AppointmentStatus.CANCELLED || appointment.getStatus() == Appointment.AppointmentStatus.COMPLETED) {
                System.out.println(">>> AgentService: Appointment " + appointmentId + " is already cancelled or completed.");
                return false; // Or true, depending on desired idempotency
            }
            String agentId = appointment.getAgentId();
            String timeSlotId = appointment.getTimeSlotId(); // Get the stored key
            LocalDateTime dateTime = appointment.getDateTime();
            // Update status
            appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);

            try {
                appointmentRepository.save(appointment); // Save the updated appointment
                System.out.println(">>> AgentService: Appointment " + appointmentId + " status set to CANCELLED.");
                // Optional: Notify the agent?
                return true;
            } catch (Exception e) {
                System.err.println("!!! AgentService: Failed to save cancelled appointment " + appointmentId + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            System.err.println("!!! AgentService: Appointment not found for cancellation: " + appointmentId);
            return false;
        }
    }

    public boolean confirmAppointment(String appointmentId, String agentId) {
        Optional<Appointment> apptOpt = appointmentRepository.findById(appointmentId);

        if (apptOpt.isPresent()) {
            Appointment appointment = apptOpt.get();

            // Security Check: Ensure the agent confirming owns this appointment
            if (!agentId.equals(appointment.getAgentId())) {
                throw new SecurityException("Agent is not authorized to confirm this appointment.");
            }

            // Check if status is PENDING
            if (appointment.getStatus() != Appointment.AppointmentStatus.PENDING) {
                return false; // Not in a confirmable state
            }

            // Update status
            appointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);

            try {
                appointmentRepository.save(appointment);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }


    // Get agents by location (now uses the optimized BST method)
    public List<Agent> getAgentsByLocation(String location) {
        // The BST is initialized in the constructor, no need to re-initialize here.
        return agentBST.findByLocation(location);
    }

    public boolean cancelAppointmentByAgent(String appointmentId, String agentId) {
        System.out.println(">>> AgentService: Attempting cancellation for Appt ID: " + appointmentId + " by Agent ID: " + agentId);
        Optional<Appointment> apptOpt = appointmentRepository.findById(appointmentId);

        if (apptOpt.isPresent()) {
            Appointment appointment = apptOpt.get();

            // **Security Check:** Ensure the agent requesting cancel is the agent assigned to this appointment
            if (!agentId.equals(appointment.getAgentId())) {
                System.err.println("!!! AgentService: Agent " + agentId + " attempted to cancel appointment " + appointmentId + " belonging to agent " + appointment.getAgentId());
                throw new SecurityException("Agent is not authorized to cancel this appointment.");
            }

            // Check if already cancelled or completed
            if (appointment.getStatus() == Appointment.AppointmentStatus.CANCELLED || appointment.getStatus() == Appointment.AppointmentStatus.COMPLETED) {
                System.out.println(">>> AgentService: Appointment " + appointmentId + " is already cancelled or completed.");
                return false;
            }

            String timeSlotId = appointment.getTimeSlotId(); // Get the stored time slot ID
            LocalDateTime appointmentDateTime = appointment.getDateTime(); // Get the appointment time

            // Update status
            appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);

            try {
                // 1. Save the updated appointment status
                appointmentRepository.save(appointment);
                System.out.println(">>> AgentService: Appointment " + appointmentId + " status set to CANCELLED.");

                // 2. *** Add Availability Back to the Agent ***
                // Fetch the agent to update their availability map
                Optional<Agent> agentOpt = agentRepository.findById(agentId);
                if (agentOpt.isPresent()) {
                    Agent agent = agentOpt.get();
                    // Use the addAvailability method in the Agent class
                    agent.addAvailability(timeSlotId, appointmentDateTime); // Add the slot back
                    // Save the agent with the updated availability
                    agentRepository.save(agent); // Save the agent specific data
                    // Update BST to reflect availability change
                    agentBST.insert(agent);
                    System.out.println(">>> AgentService: Availability slot " + timeSlotId + " added back for agent " + agentId);
                } else {
                    System.err.println("!!! AgentService: Could not find agent " + agentId + " to add back availability after cancellation.");
                }

                // Optional: Notify the client that the agent cancelled?
                return true;
            } catch (Exception e) {
                System.err.println("!!! AgentService: Failed during agent cancellation process for " + appointmentId + ": " + e.getMessage());
                e.printStackTrace();
                // IMPORTANT: Consider rollback if saving or adding availability fails.
                return false;
            }
        } else {
            System.err.println("!!! AgentService: Appointment not found for agent cancellation: " + appointmentId);
            return false;
        }
    }


    public Agent updateAgent(Agent agent) {

// Fetch existing complete agent to get full data and check for changes

        Optional<Agent> existingCompleteAgentOpt = getCompleteAgentById(agent.getUserId());

        if (existingCompleteAgentOpt.isEmpty()) {

            throw new RuntimeException("Agent not found with ID: " + agent.getUserId());

        }

        Agent existingAgent = existingCompleteAgentOpt.get();



// Update common user fields via UserService

// Assuming `userService.updateUser` correctly handles passwords (hashing new ones, not re-hashing existing ones)

        User updatedUser = userService.updateUser(agent); // Pass agent as User for update

        agent.setPassword(updatedUser.getPassword()); // Ensure agent object has the hashed password



// Save agent-specific details

        Agent updatedAgent = agentRepository.save(agent);



// Update BST. The BST's insert method will handle updating the existing node.

        agentBST.insert(updatedAgent);

        System.out.println("Agent updated and BST synchronized for userId: " + updatedAgent.getUserId());

        return updatedAgent;

    }



    // Delete agent
    public void deleteAgent(String agentId) {
        // First delete from user repository using UserService
        userService.deleteUser(agentId);

        // Then delete from agent repository
        agentRepository.deleteById(agentId);

        // NOW: Delete from BST efficiently, no need to re-initialize the whole tree
        agentBST.delete(agentId);
        System.out.println("Agent deleted from repository and BST for ID: " + agentId);
    }

    // Add rating to agent
    public void addRating(String agentId, double rating) {
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.addRating(rating);

            // Save agent-specific updates
            agentRepository.save(agent);

            // Update BST
            agentBST.insert(agent);
            System.out.println(">>> AgentService: Rating added and agent specific data saved for ID: " + agentId);
        } else {
            throw new RuntimeException("Agent not found with ID: " + agentId);
        }
    }

    public List<Appointment> getAppointmentsByIds(List<String> appointmentIds) {
        System.out.println(">>> AgentService: Fetching " + (appointmentIds == null ? 0 : appointmentIds.size()) + " appointments by IDs.");
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Consider findById loop if performance becomes an issue AND findAll is slow.
        List<Appointment> allAppointments = appointmentRepository.findAll();
        List<Appointment> foundAppointments = allAppointments.stream()
                .filter(appt -> appt != null && appointmentIds.contains(appt.getAppointmentId()))
                .collect(Collectors.toList());
        System.out.println(">>> AgentService: Found " + foundAppointments.size() + " matching appointments.");
        return foundAppointments;
    }

    // Add availability to agent
    public void addAvailability(String agentId, String timeSlotId, LocalDateTime dateTime) {
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.addAvailability(timeSlotId, dateTime);

            // No need to update user fields, so just save to agent repository
            agentRepository.save(agent);

            // Update BST
            agentBST.insert(agent);
        } else {
            throw new RuntimeException("Agent not found with ID: " + agentId);
        }
    }

    public boolean removeAvailability(String agentId, String timeSlotId) {
        System.out.println(">>> AgentService: Removing availability slot '" + timeSlotId + "' for agent " + agentId);
        // Fetch the agent using repository to ensure latest data
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            // Ensure Agent class has this method:
            boolean removed = agent.removeAvailability(timeSlotId); // Modifies the agent object's internal map

            if (removed) {
                try {
                    agentRepository.save(agent); // Save the updated agent back to agents.txt
                    // Update BST cache if you use it for availability (which you do indirectly via location/rating)
                    agentBST.insert(agent); // Insert handles updates as well
                    System.out.println(">>> AgentService: Availability removed and agent saved for ID: " + agentId);
                    return true;
                } catch (Exception e) {
                    System.err.println("!!! AgentService: Failed to save agent after removing availability for " + agentId + ": " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            } else {
                System.out.println(">>> AgentService: Time slot " + timeSlotId + " not found for removal for agent " + agentId + ".");
                return false;
            }
        } else {
            System.err.println("!!! AgentService: Agent not found for removing availability: " + agentId);
            return false;
        }
    }


    public Appointment bookNewAppointment(String agentId, String clientId, String timeSlotId, LocalDateTime dateTime, String notes) {
        System.out.println(">>> AgentService: Attempting booking for Agent " + agentId + ", Client " + clientId + ", Slot: " + timeSlotId);

        // --- Validation ---
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isEmpty()) {
            throw new IllegalArgumentException("Agent not found with ID: " + agentId);
        }
        // Check if the specific timeSlotId exists in the Agent's availability BEFORE booking
        if (!agentOpt.get().getAvailability().containsKey(timeSlotId)) {
            throw new IllegalStateException("Selected time slot (" + timeSlotId + ") is no longer available.");
        }
        // --- End Validation ---

        String newAppointmentId = UUID.randomUUID().toString();
        System.out.println(">>> AgentService: Generated new Appointment ID: " + newAppointmentId);


        // 1. Create Appointment Object (Adjust constructor call if needed)
        Appointment newAppointment = new Appointment(
                newAppointmentId, // Pass generated ID to constructor
                agentId,
                clientId,
                timeSlotId,
                dateTime,
                Appointment.AppointmentStatus.PENDING, // Set initial status
                notes
        );

        // Optional: Log the object just before saving
        System.out.println(">>> AgentService: Appointment object created, preparing to save: " + newAppointment);
        if (newAppointment.getAppointmentId() == null) {
            System.err.println("!!! CRITICAL AgentService: ID is STILL null right before save!");
        }


        Appointment savedAppointment = null;
        try {
            // 2. Save the new Appointment record
            savedAppointment = appointmentRepository.save(newAppointment);
            if (savedAppointment == null || savedAppointment.getAppointmentId() == null) {
                throw new RuntimeException("Failed to save appointment to repository.");
            }
            System.out.println(">>> AgentService: Appointment saved successfully with ID: " + savedAppointment.getAppointmentId());

            boolean removed = this.removeAvailability(agentId, timeSlotId); // Call the method above
            if (!removed) {
                System.err.println("!!! WARNING: Appointment " + savedAppointment.getAppointmentId() + " booked, but failed to remove availability slot " + timeSlotId + " for agent " + agentId);
                // Decide how critical this is. Maybe try again? Or just log.
            } else {
                System.out.println(">>> AgentService: Availability slot " + timeSlotId + " removed for agent " + agentId);
            }

            // *** INTEGRATION POINT 2: Update Client's History ***
            try {
                clientService.addAppointmentToHistory(clientId, savedAppointment.getAppointmentId());
                System.out.println(">>> AgentService: Added appointment " + savedAppointment.getAppointmentId() + " to history for client " + clientId);
            } catch (Exception historyEx) {
                // Log error but don't fail the entire booking because history failed
                System.err.println("!!! WARNING: Failed to add appointment " + savedAppointment.getAppointmentId() + " to client " + clientId + " history: " + historyEx.getMessage());
            }

            return savedAppointment;

        } catch (Exception e) {
            System.err.println("!!! AgentService: Failed during booking process: " + e.getMessage());
            e.printStackTrace();
            // Don't update history or remove slot if main save fails
            throw new RuntimeException("Appointment booking failed. Please try again.", e);
        }
    }

    public boolean authenticate(String username, String password) {
        // First check if user exists and credentials are valid
        if (userService.authenticate(username, password)) {
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Check role instead of instanceof
                return "AGENT".equals(user.getRole()) && user.isActive();
            }
        }
        return false;
    }
}
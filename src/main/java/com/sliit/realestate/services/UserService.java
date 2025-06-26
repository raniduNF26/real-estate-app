package com.sliit.realestate.services;

/*import com.sliit.realestate.models.Admin;*/
import com.sliit.realestate.models.Agent;
import com.sliit.realestate.models.Client;
import com.sliit.realestate.models.User;
/*import com.sliit.realestate.repositories.AdminRepository;*/
import com.sliit.realestate.repositories.AgentRepository;
import com.sliit.realestate.repositories.ClientRepository;
import com.sliit.realestate.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentRepository agentRepository;


    @Autowired
    private PasswordEncoder passwordEncoder;

    // Create a new user
    public User createUser(User user) {
        // Generate a unique ID if not provided
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            user.setUserId(UUID.randomUUID().toString());
        }

        // Hash password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    // Get user by ID
    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    // Get user by username
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Get user by email
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get agent by ID
    public Optional<Agent> getAgentById(String agentId) {
        // Fetch base user data
        Optional<User> userOpt = userRepository.findById(agentId);
        if (userOpt.isPresent() && "AGENT".equals(userOpt.get().getRole())) {
            // Fetch agent-specific data
            return agentRepository.findById(agentId);
        }
        return Optional.empty();
    }

    // Get all agents
    public List<Agent> getAllAgents() {
        List<Agent> agents = new ArrayList<>();
        List<User> users = userRepository.findAll();

        for (User user : users) {
            if ("AGENT".equals(user.getRole())) {
                // Fetch agent-specific data
                Optional<Agent> agentOpt = agentRepository.findById(user.getUserId());
                agentOpt.ifPresent(agents::add);
            }
        }
        return agents;
    }

    // Update user
    public User updateUser(User user) {
        // Check if user exists
        Optional<User> existingUser = userRepository.findById(user.getUserId());
        if (existingUser.isPresent()) {
            return userRepository.save(user);
        }
        throw new RuntimeException("User not found with ID: " + user.getUserId());
    }

    // Delete user
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    // Authenticate user
    public boolean authenticate(String username, String password) {
        System.out.println("Attempting authentication for: " + username);
        System.out.println("Input password: " + password);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("Found user: " + user.getUsername());
            System.out.println("Stored password hash: " + user.getPassword());
            // Add this debug check:
            boolean isPasswordValid = passwordEncoder.matches(password, user.getPassword());

            System.out.println("Password validation result: " + isPasswordValid);
            System.out.println("User active status: " + user.isActive());

            return passwordEncoder.matches(password, user.getPassword()) && user.isActive();
        }
        return false;
    }

    // Check if user exists
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    // Check if email exists
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
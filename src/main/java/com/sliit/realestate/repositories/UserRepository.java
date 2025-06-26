package com.sliit.realestate.repositories;

/*import com.sliit.realestate.models.Admin;*/
import com.sliit.realestate.models.Agent;
import com.sliit.realestate.models.Client;
import com.sliit.realestate.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class UserRepository {
    private static final String FILE_PATH = "src/main/resources/users.txt";

    // Save user to file
    public User save(User user) {
        List<User> users = findAll();
        // Check if user already exists
        boolean exists = users.stream().anyMatch(u -> u.getUserId().equals(user.getUserId()));

        if (exists) {
            // Update existing user
            users = users.stream()
                    .map(u -> u.getUserId().equals(user.getUserId()) ? user : u)
                    .collect(Collectors.toList());
        } else {
            // Add new user
            users.add(user);
        }

        writeToFile(users);
        return user;
    }

    // Find user by ID
    public Optional<User> findById(String userId) {
        return findAll().stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst();
    }

    public Optional<User> findByUsername(String username) {
        return findAll().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    // Find user by email
    public Optional<User> findByEmail(String email) {
        return findAll().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    // Get all users
    public List<User> findAll() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<User> users = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 8) {
                    System.err.println("Invalid user record: " + line);
                    continue;
                }

                try {
                    User user = new User(
                            parts[0], // userId
                            parts[1], // username
                            parts[2], // password
                            parts[3], // fullName
                            parts[4], // email
                            parts[5], // phone
                            parts[7]  // role
                    );
                    user.setActive(Boolean.parseBoolean(parts[6]));
                    users.add(user);
                } catch (Exception e) {
                    System.err.println("Error parsing user: " + e.getMessage());
                }
            }
            return users;
        } catch (IOException e) {
            System.err.println("Error reading users file: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    // Delete user
    public void deleteById(String userId) {
        List<User> users = findAll();
        users = users.stream()
                .filter(user -> !user.getUserId().equals(userId))
                .collect(Collectors.toList());
        writeToFile(users);
    }

    // Write users to file
    private void writeToFile(List<User> users) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (User user : users) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                        user.getUserId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.isActive(),
                        user.getRole()
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
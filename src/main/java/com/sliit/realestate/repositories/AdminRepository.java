package com.sliit.realestate.repositories;
/*



import com.sliit.realestate.models.Admin;
import com.sliit.realestate.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AdminRepository {
    private static final String FILE_PATH = "admins.txt";

    // Save admin to file
    public Admin save(Admin admin) {
        List<Admin> admins = findAll();
        // Check if admin already exists
        boolean exists = admins.stream().anyMatch(a -> a.getUserId().equals(admin.getUserId()));

        if (exists) {
            // Update existing admin
            admins = admins.stream()
                    .map(a -> a.getUserId().equals(admin.getUserId()) ? admin : a)
                    .collect(Collectors.toList());
        } else {
            // Add new admin
            admins.add(admin);
        }

        writeToFile(admins);
        return admin;
    }

    // Find admin by ID
    public Optional<Admin> findById(String adminId) {
        return findAll().stream()
                .filter(admin -> admin.getUserId().equals(adminId))
                .findFirst();
    }

    // Find admin by username
    public Optional<Admin> findByUsername(String username) {
        return findAll().stream()
                .filter(admin -> admin.getUsername().equals(username))
                .findFirst();
    }

    // Get all admins
    public List<Admin> findAll() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<Admin> admins = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    // Parse base user properties
                    String[] userParts = parts[0].split(",");
                    if (userParts.length >= 7) {
                        // Parse permissions
                        List<String> permissions = parts[2].isEmpty() ? new ArrayList<>() :
                                Arrays.asList(parts[2].split(";"));

                        Admin admin = new Admin(
                                userParts[0], // userId
                                userParts[1], // username
                                userParts[2], // password
                                userParts[3], // fullName
                                userParts[4], // email
                                userParts[5], // phone
                                parts[1]      // adminRole
                        );
                        admin.setActive(Boolean.parseBoolean(userParts[6])); // active

                        // Add permissions
                        for (String permission : permissions) {
                            admin.addPermission(permission);
                        }

                        admins.add(admin);
                    }
                }
            }
            return admins;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Delete admin
    public void deleteById(String adminId) {
        List<Admin> admins = findAll();
        admins = admins.stream()
                .filter(admin -> !admin.getUserId().equals(adminId))
                .collect(Collectors.toList());
        writeToFile(admins);
    }

    // Write admins to file
    private void writeToFile(List<Admin> admins) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Admin admin : admins) {
                // Format base user properties
                String userProps = String.format("%s,%s,%s,%s,%s,%s,%s",
                        admin.getUserId(),
                        admin.getUsername(),
                        admin.getPassword(),
                        admin.getFullName(),
                        admin.getEmail(),
                        admin.getPhone(),
                        admin.isActive()
                );

                // Format permissions
                String permissions = String.join(";", admin.getPermissions());

                writer.write(String.format("%s|%s|%s\n",
                        userProps,
                        admin.getRole(),
                        permissions
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
*/

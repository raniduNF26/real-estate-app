package com.sliit.realestate.repositories; // Ensure this matches your package

import com.sliit.realestate.models.Agent; // Ensure this import is correct
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class AgentRepository {
    private static final String FILE_PATH_STR = "src/main/resources/agents.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;


    private static final int EXPECTED_AGENT_PARTS = 11; // Updated for new fields

    public AgentRepository() {
        System.out.println(">>> AgentRepository: Initializing...");
        ensureFileExists();
        System.out.println(">>> AgentRepository: Initialization complete.");
    }

    private void ensureFileExists() {
        File file = new File(FILE_PATH_STR);
        File directory = file.getParentFile();
        System.out.println(">>> AgentRepository.ensureFileExists: Checking path: " + file.getAbsolutePath());

        if (directory != null && !directory.exists()) {
            System.out.println(">>> AgentRepository.ensureFileExists: Creating directory: " + directory.getAbsolutePath());
            boolean created = directory.mkdirs();
            if (!created) {
                System.err.println("!!! AgentRepository.ensureFileExists: FAILED to create directory: " + directory.getAbsolutePath());
            }
        }
        if (!file.exists()) {
            try {
                System.out.println(">>> AgentRepository.ensureFileExists: File does not exist, creating...");
                boolean created = file.createNewFile();
                System.out.println(">>> AgentRepository.ensureFileExists: New file created: " + created);
            } catch (IOException e) {
                System.err.println("!!! AgentRepository.ensureFileExists: FAILED to create file: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> AgentRepository.ensureFileExists: File already exists.");
        }
    }

    public Agent save(Agent agent) {
        if (agent == null || agent.getUserId() == null) {
            throw new IllegalArgumentException("Cannot save null agent or agent with null ID");
        }
        System.out.println(">>> AgentRepository.save: Saving agent ID: " + agent.getUserId());
        try {
            List<Agent> agents = findAll(); // findAll should call the internal version if this is used recursively
            Optional<Agent> existingAgentOpt = agents.stream()
                    .filter(a -> a != null && a.getUserId() != null && a.getUserId().equals(agent.getUserId()))
                    .findFirst();

            if (existingAgentOpt.isPresent()) {
                // Update: remove old and add new ensures the updated object is in the list
                agents.removeIf(a -> a != null && a.getUserId().equals(agent.getUserId()));
                agents.add(agent);
            } else {
                agents.add(agent);
            }

            writeToFile(agents);
            return agent;
        } catch (Exception e) {
            System.err.println("!!! AgentRepository.save: Failed to save agent data for ID " + agent.getUserId() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save agent data", e);
        }
    }

    public Optional<Agent> findById(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            return Optional.empty();
        }
        System.out.println(">>> AgentRepository.findById: Searching for agent ID: " + agentId);
        File file = new File(FILE_PATH_STR);
        if (!file.exists() || !file.canRead()) {
            System.err.println("!!! AgentRepository.findById: File not found or unreadable: " + file.getAbsolutePath());
            return Optional.empty();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1); // Keep trailing empty fields
                if (parts.length > 0 && agentId.equals(parts[0])) {
                    return Optional.ofNullable(parseAgent(parts));
                }
            }
        } catch (IOException e) {
            System.err.println("!!! AgentRepository.findById: Error reading agent data: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println(">>> AgentRepository.findById: Agent ID " + agentId + " not found.");
        return Optional.empty();
    }

    public List<Agent> findAll() {
        List<Agent> agents = new ArrayList<>();
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> AgentRepository.findAll: Accessing path: " + file.getAbsolutePath());

        if (!file.exists() || !file.canRead()) {
            System.err.println("!!! AgentRepository.findAll: File not found or unreadable: " + file.getAbsolutePath());
            return agents;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    lineNum++;
                    continue;
                }
                Agent agent = parseAgent(line.split("\\|", -1)); // Keep trailing empty fields
                if (agent != null) {
                    agents.add(agent);
                } else {
                    System.err.println("!!! AgentRepository.findAll: Failed to parse agent on line " + lineNum + ". Line: [" + line + "]");
                }
                lineNum++;
            }
            System.out.println(">>> AgentRepository.findAll: Loaded " + agents.size() + " agents.");
        } catch (IOException e) {
            System.err.println("!!! AgentRepository.findAll: Error reading agent data: " + e.getMessage());
            e.printStackTrace();
        }
        return agents;
    }

    private Agent parseAgent(String[] parts) {

        if (parts == null || parts.length != EXPECTED_AGENT_PARTS) {
            System.err.println("!!! parseAgent: Invalid agent data format. Expected " + EXPECTED_AGENT_PARTS + " fields, got " + (parts == null ? "null" : parts.length) + ". Parts: " + Arrays.toString(parts));
            return null;
        }

        try {
            String userId = parts[0];
            String licenseNumber = parts[1];
            String specialization = parts[2];
            String location = parts[3];
            double averageRating = Double.parseDouble(parts[4]);
            int totalRatings = Integer.parseInt(parts[5]);
            List<String> managedProperties = parts[6].isEmpty() ?
                    new ArrayList<>() :
                    new ArrayList<>(Arrays.asList(parts[6].split(","))); // Ensure mutable list

            Map<String, LocalDateTime> availability = new HashMap<>();
            if (!parts[7].isEmpty()) {
                String[] availabilityItems = parts[7].split(",");
                for (String item : availabilityItems) {
                    if (item.contains("=")) { // Expecting key=value format
                        String[] kv = item.split("=", 2);
                        try {
                            availability.put(kv[0].trim(), LocalDateTime.parse(kv[1].trim(), formatter));
                        } catch (DateTimeParseException e) {
                            System.err.println("!!! parseAgent: Invalid date format in availability: '" + kv[1] + "' for agent " + userId);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.err.println("!!! parseAgent: Invalid availability item format (missing '='): '" + item + "' for agent " + userId);
                        }
                    } else if (!item.trim().isEmpty()){
                        System.err.println("!!! parseAgent: Invalid availability item format (missing '='): '" + item + "' for agent " + userId);
                    }
                }
            }


            String profileImageUrl = parts[8];
            String experience = parts[9];
            String bio = parts[10];


            // Create Agent object (this one is primarily for data from agents.txt)
            // The full User details (username, email, etc.) are merged by AgentService
            Agent agentData = new Agent(
                    userId, "", "", "", "", "", // User fields (except ID) are placeholders
                    licenseNumber, specialization, location,
                    averageRating, totalRatings,
                    managedProperties, availability
            );
            // Set the new fields using setters (assuming they exist in Agent.java)
            agentData.setProfileImageUrl(profileImageUrl);
            agentData.setExperience(experience);
            agentData.setBio(bio);
            // Note: agentData.setActive() is not set here as 'active' status comes from users.txt

            return agentData;

        } catch (NumberFormatException e) {
            System.err.println("!!! parseAgent: Error parsing numeric data: " + e.getMessage() + ". Parts: " + Arrays.toString(parts));
            return null;
        } catch (Exception e) {
            System.err.println("!!! parseAgent: Unexpected error parsing agent data: " + e.getMessage() + ". Parts: " + Arrays.toString(parts));
            e.printStackTrace();
            return null;
        }
    }

    public void deleteById(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            return;
        }
        System.out.println(">>> AgentRepository.deleteById: Deleting agent ID: " + agentId);
        List<Agent> agents = findAll();
        List<Agent> updatedAgents = agents.stream()
                .filter(agent -> agent != null && !agentId.equals(agent.getUserId()))
                .collect(Collectors.toList());

        if (agents.size() != updatedAgents.size()) {
            writeToFile(updatedAgents);
            System.out.println(">>> AgentRepository.deleteById: Agent " + agentId + " deleted.");
        } else {
            System.out.println(">>> AgentRepository.deleteById: Agent " + agentId + " not found for deletion.");
        }
    }

    private void writeToFile(List<Agent> agents) {
        if (agents == null) {
            agents = new ArrayList<>();
        }
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> AgentRepository.writeToFile: Writing " + agents.size() + " agents to " + file.getAbsolutePath());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) { // false to overwrite
            for (Agent agent : agents) {
                if (agent == null) {
                    System.err.println("!!! writeToFile: Skipping null agent.");
                    continue;
                }

                String managedPropertiesStr = (agent.getManagedProperties() != null && !agent.getManagedProperties().isEmpty())
                        ? String.join(",", agent.getManagedProperties())
                        : "";

                String availabilityStr = "";
                if (agent.getAvailability() != null && !agent.getAvailability().isEmpty()) {
                    availabilityStr = agent.getAvailability().entrySet().stream()
                            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                            .map(entry -> entry.getKey() + "=" + entry.getValue().format(formatter)) // key=value format
                            .collect(Collectors.joining(","));
                }


                String profileImageUrl = agent.getProfileImageUrl() != null ? agent.getProfileImageUrl() : "";
                String experience = agent.getExperience() != null ? agent.getExperience() : "";
                String bio = agent.getBio() != null ? agent.getBio() : "";



                String lineToWrite = String.format("%s|%s|%s|%s|%f|%d|%s|%s|%s|%s|%s",
                        agent.getUserId(),
                        agent.getLicenseNumber() != null ? agent.getLicenseNumber() : "",
                        agent.getSpecialization() != null ? agent.getSpecialization() : "",
                        agent.getLocation() != null ? agent.getLocation() : "",
                        agent.getAverageRating(), // double
                        agent.getTotalRatings(),  // int
                        managedPropertiesStr,
                        availabilityStr,
                        profileImageUrl,         // New
                        experience,              // New
                        bio                      // New
                );
                writer.write(lineToWrite);
                writer.newLine();
            }
            System.out.println(">>> AgentRepository.writeToFile: Write operation completed.");
        } catch (IOException e) {
            System.err.println("!!! AgentRepository.writeToFile: Error writing agent data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
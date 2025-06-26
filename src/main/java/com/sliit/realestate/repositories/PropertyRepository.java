package com.sliit.realestate.repositories;




import com.sliit.realestate.models.Property; // Adjust import
import org.springframework.stereotype.Repository;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PropertyRepository {

    private static final String FILE_PATH_STR = "src/main/resources/properties.txt";
    // Define expected number of fields based on the chosen format
    private static final int EXPECTED_PROPERTY_PARTS = 11;

    public PropertyRepository() {
        System.out.println(">>> PropertyRepository: Initializing bean and ensuring file exists...");
        ensureFileExists();
        System.out.println(">>> PropertyRepository: Initialization complete.");
    }

    /**
     * Ensures the directory structure and the property data file exist.
     */
    private void ensureFileExists() {
        File file = new File(FILE_PATH_STR);
        File directory = file.getParentFile();

        System.out.println(">>> PropertyRepository.ensureFileExists: Checking relative path: " + FILE_PATH_STR);
        System.out.println(">>> PropertyRepository.ensureFileExists: Checking absolute path: " + file.getAbsolutePath());

        if (directory != null && !directory.exists()) {
            System.out.println(">>> PropertyRepository.ensureFileExists: Creating directory: " + directory.getAbsolutePath());
            boolean created = directory.mkdirs();
            System.out.println(">>> PropertyRepository.ensureFileExists: Directory created: " + created);
            if (!created) { System.err.println("!!! PropertyRepository.ensureFileExists: FAILED to create directory..."); }
        } else if (directory == null) {
            System.err.println("!!! PropertyRepository.ensureFileExists: Cannot determine parent directory...");
        }

        if (!file.exists()) {
            try {
                System.out.println(">>> PropertyRepository.ensureFileExists: File does not exist, creating: " + file.getAbsolutePath());
                boolean created = file.createNewFile();
                System.out.println(">>> PropertyRepository.ensureFileExists: Created new file: " + created);
                if (!created) { System.err.println("!!! PropertyRepository.ensureFileExists: FAILED to create new file..."); }
            } catch (IOException e) {
                System.err.println("!!! PropertyRepository.ensureFileExists: FAILED to create file: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> PropertyRepository.ensureFileExists: File already exists: " + file.getAbsolutePath());
            System.out.println(">>> PropertyRepository.ensureFileExists: Readable: " + file.canRead() + ", Writable: " + file.canWrite());
        }
    }

    /**
     * Saves a property (creates if new, updates if exists) to the file.
     */
    public Property save(Property property) {
        File file = new File(FILE_PATH_STR);
        System.out.println("===== PROPERTY REPOSITORY: SAVE =====");
        System.out.println(">>> PropertyRepository - save - Accessing path: " + file.getAbsolutePath());
        if (property == null || property.getPropertyId() == null) {
            System.err.println("!!! PropertyRepository.save: Attempted to save null property or property with null ID.");
            throw new IllegalArgumentException("Cannot save null property or property with null ID");
        }
        System.out.println("Saving property ID: " + property.getPropertyId() + " Address: " + property.getAddress());

        try {
            List<Property> properties = findAllInternal(); // Use internal to prevent loops if findAll calls save indirectly
            final String propertyIdToSave = property.getPropertyId();

            Optional<Property> existingPropOpt = properties.stream()
                    .filter(p -> p != null && p.getPropertyId() != null && p.getPropertyId().equals(propertyIdToSave))
                    .findFirst();

            List<Property> listToWrite;
            if (existingPropOpt.isPresent()) {
                System.out.println(">>> PropertyRepository.save: Updating existing property ID: " + propertyIdToSave);
                listToWrite = properties.stream()
                        .map(p -> (p != null && p.getPropertyId() != null && p.getPropertyId().equals(propertyIdToSave)) ? property : p)
                        .collect(Collectors.toList());
            } else {
                System.out.println(">>> PropertyRepository.save: Adding new property ID: " + propertyIdToSave);
                properties.add(property);
                listToWrite = properties;
            }

            writeToFile(listToWrite);
            System.out.println(">>> PropertyRepository.save: Property save operation completed for ID: " + propertyIdToSave);
            return property;

        } catch (Exception e) {
            System.err.println("!!! Error during save operation for property ID " + (property != null ? property.getPropertyId() : "null") + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save property data for ID: " + (property != null ? property.getPropertyId() : "null"), e);
        }
    }

    /**
     * Finds a property by its unique ID. Reads file line by line (inefficient).
     */
    public Optional<Property> findById(String propertyId) {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> PropertyRepository - findById - Accessing path: " + file.getAbsolutePath());
        if (propertyId == null || propertyId.trim().isEmpty()) return Optional.empty();

        if (!file.exists() || !file.canRead()) {
            System.err.println("!!! PropertyRepository - findById - File not found or unreadable: " + file.getAbsolutePath());
            return Optional.empty();
        }

        System.out.println(">>> PropertyRepository.findById: Searching for property ID: " + propertyId);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|", -1); // Keep trailing empty

                if (parts.length > 0 && propertyId.equals(parts[0])) {
                    System.out.println(">>> PropertyRepository.findById: Found potential match for ID " + propertyId + " on line " + lineNum);
                    Property property = parseProperty(parts);
                    if(property == null) {
                        System.err.println("!!! PropertyRepository.findById: parseProperty returned null for matching ID: " + propertyId + " line " + lineNum);
                    }
                    return Optional.ofNullable(property);
                }
            }
            System.out.println(">>> PropertyRepository.findById: Property ID " + propertyId + " not found after checking " + lineNum + " lines.");
        } catch (IOException e) {
            System.err.println("!!! IOException during findById for property ID " + propertyId + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("!!! Unexpected error during findById for property ID " + propertyId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Finds all properties managed by a specific agent. Reads entire file (inefficient).
     */
    public List<Property> findByAgentId(String agentId) {
        System.out.println(">>> PropertyRepository - findByAgentId: " + agentId);
        if (agentId == null || agentId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return findAll().stream()
                .filter(property -> property != null && agentId.equals(property.getAgentId()))
                .collect(Collectors.toList());
    }


    /** Gets all properties by reading and parsing the entire file. */
    public List<Property> findAll() {
        return findAllInternal(); // Use helper
    }

    /** Helper for internal use to avoid loops */
    private List<Property> findAllInternal() {
        File file = new File(FILE_PATH_STR);
        List<Property> properties = new ArrayList<>();
        if (!file.exists() || !file.canRead()) { return properties; }

        int lineNum = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                Property property = parseProperty(parts);
                if (property != null) {
                    properties.add(property);
                } else {
                    System.err.println("!!! PropertyRepository.findAllInternal: Skipping line " + lineNum + " due to parseProperty failure. Line: ["+line+"]");
                }
            }
        } catch (IOException e) {
            System.err.println("!!! IOException during findAllInternal properties: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("!!! Unexpected error during findAllInternal properties: " + e.getMessage());
        }
        return properties;
    }

    /**
     * Deletes a property by ID. Reads all, filters, writes back.
     */
    public void deleteById(String propertyId) {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> PropertyRepository - deleteById - Accessing path: " + file.getAbsolutePath());
        if (propertyId == null || propertyId.trim().isEmpty()) {
            System.err.println("!!! PropertyRepository.deleteById: Called with null or empty propertyId.");
            return;
        }
        System.out.println(">>> PropertyRepository.deleteById: Attempting to delete property ID: " + propertyId);
        List<Property> properties = findAllInternal();
        int initialSize = properties.size();

        List<Property> propertiesAfterDeletion = properties.stream()
                .filter(p -> p != null && p.getPropertyId() != null && !p.getPropertyId().equals(propertyId))
                .collect(Collectors.toList());
        int finalSize = propertiesAfterDeletion.size();

        if (initialSize > finalSize) {
            System.out.println(">>> PropertyRepository.deleteById: Property ID " + propertyId + " found and will be removed.");
            writeToFile(propertiesAfterDeletion);
            System.out.println(">>> PropertyRepository.deleteById: Wrote " + finalSize + " properties back to file after deletion.");
        } else {
            System.out.println(">>> PropertyRepository.deleteById: Property ID " + propertyId + " not found for deletion.");
        }
    }


    /**
     * Parses a String array into a Property object.
     * Assumes format: propertyId|agentId|address|price|type|status|description|imageUrl|bedrooms|bathrooms|areaSqFt
     */
    private Property parseProperty(String[] parts) {
        if (parts == null || parts.length != EXPECTED_PROPERTY_PARTS) {
            System.err.println(">>> parseProperty RETURNING NULL. Invalid property data: Expected " + EXPECTED_PROPERTY_PARTS + " fields, got " + (parts == null ? "null" : parts.length) + ". Parts: " + Arrays.toString(parts));
            return null;
        }

        try {
            String propertyId = parts[0];
            String agentId = parts[1];
            String address = parts[2];
            BigDecimal price = new BigDecimal(parts[3]); // Use BigDecimal constructor
            String type = parts[4];
            Property.PropertyStatus status = Property.PropertyStatus.valueOf(parts[5].toUpperCase()); // String to Enum
            String description = parts[6];
            String imageUrl = parts[7];
            int bedrooms = Integer.parseInt(parts[8]);
            int bathrooms = Integer.parseInt(parts[9]);
            double areaSqFt = Double.parseDouble(parts[10]);

            // Use the full constructor
            return new Property(propertyId, agentId, address, price, type, status, description, imageUrl, bedrooms, bathrooms, areaSqFt);

        } catch (NumberFormatException e) {
            System.err.println(">>> parseProperty RETURNING NULL due to NumberFormatException: " + e.getMessage() + ". Parts: " + Arrays.toString(parts));
            return null;
        } catch (IllegalArgumentException e) { // Catches invalid Enum value
            System.err.println(">>> parseProperty RETURNING NULL due to IllegalArgumentException (likely invalid Status Enum): " + e.getMessage() + ". Input: " + (parts.length > 5 ? parts[5] : "N/A") + ". Parts: " + Arrays.toString(parts));
            return null;
        } catch (Exception e) {
            System.err.println(">>> parseProperty RETURNING NULL due to unexpected Exception: " + e.getMessage() + ". Parts: " + Arrays.toString(parts));
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Writes the list of properties to the file, overwriting existing content.
     */
    private void writeToFile(List<Property> properties) {
        File file = new File(FILE_PATH_STR);
        System.out.println(">>> PropertyRepository - writeToFile - Accessing path: " + file.getAbsolutePath());
        if (properties == null) { properties = new ArrayList<>(); }
        System.out.println(">>> PropertyRepository.writeToFile: Attempting to write " + properties.size() + " properties to file...");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) { // Overwrite
            int lineCount = 0;
            for (Property p : properties) {
                if (p == null) {
                    System.err.println("!!! Skipping null property during writeToFile.");
                    continue;
                }
                // Format: propertyId|agentId|address|price|type|status|description|imageUrl|bedrooms|bathrooms|areaSqFt
                // Handle potential nulls before formatting
                String lineToWrite = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%d|%d|%f",
                        p.getPropertyId() != null ? p.getPropertyId() : "",
                        p.getAgentId() != null ? p.getAgentId() : "",
                        p.getAddress() != null ? p.getAddress() : "",
                        p.getPrice() != null ? p.getPrice().toPlainString() : "0.00", // Format BigDecimal
                        p.getType() != null ? p.getType() : "",
                        p.getStatus() != null ? p.getStatus().name() : Property.PropertyStatus.DRAFT.name(), // Default status if null
                        p.getDescription() != null ? p.getDescription() : "",
                        p.getImageUrl() != null ? p.getImageUrl() : "",
                        p.getBedrooms(), // int
                        p.getBathrooms(), // int
                        p.getAreaSqFt() // double (%f)
                );
                writer.write(lineToWrite);
                writer.newLine();
                lineCount++;
            }
            System.out.println(">>> PropertyRepository.writeToFile: Finished writing " + lineCount + " property lines.");
        } catch (IOException e) {
            System.err.println("!!! IOException during writeToFile properties: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("!!! Unexpected error during writeToFile properties: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
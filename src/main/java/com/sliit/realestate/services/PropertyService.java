package com.sliit.realestate.services;


import com.sliit.realestate.models.Property; // Adjust imports
import com.sliit.realestate.repositories.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    /**
     * Gets a single property by its ID.
     */
    public Optional<Property> getPropertyById(String propertyId) {
        System.out.println(">>> PropertyService: Getting property by ID: " + propertyId);
        return propertyRepository.findById(propertyId);
    }

    public List<Property> getPublishedAndFilteredProperties(String propertyType) {
        System.out.println(">>> PropertyService: Getting published properties. Type Filter: " + propertyType);
        List<Property> allProperties = propertyRepository.findAll();

        List<Property> publishedProperties = allProperties.stream()
                .filter(p -> p != null && (p.getStatus() == Property.PropertyStatus.FOR_SALE || p.getStatus() == Property.PropertyStatus.FOR_RENT))
                .collect(Collectors.toList());

        if (propertyType != null && !propertyType.trim().isEmpty()) {
            final String lowerTypeFilter = propertyType.toLowerCase();
            System.out.println(">>> PropertyService: Applying type filter: " + lowerTypeFilter);
            return publishedProperties.stream()
                    .filter(p -> p.getType() != null && p.getType().toLowerCase().equals(lowerTypeFilter))
                    .collect(Collectors.toList());
        }
        return publishedProperties;
    }
    /**
     * Gets a list of properties based on a list of IDs.
     * Note: This implementation calls findById repeatedly, which is inefficient
     * for file-based storage. A better repository method would be preferable.
     */
    public List<Property> getPropertiesByIds(List<String> propertyIds) {
        System.out.println(">>> PropertyService: Getting properties for " + (propertyIds == null ? 0 : propertyIds.size()) + " IDs.");
        if (propertyIds == null || propertyIds.isEmpty()) {
            return new ArrayList<>();
        }
        // Inefficient - reads file multiple times via findById -> findAll
        // return propertyIds.stream()
        //         .map(this::getPropertyById) // Calls repo findById for each ID
        //         .filter(Optional::isPresent)
        //         .map(Optional::get)
        //         .collect(Collectors.toList());

        // Slightly more efficient: Read all once, then filter in memory
        List<Property> allProperties = propertyRepository.findAll();
        return allProperties.stream()
                .filter(p -> p != null && propertyIds.contains(p.getPropertyId()))
                .collect(Collectors.toList());
    }

    /**
     * Gets all properties.
     */
    public List<Property> getAllProperties() {
        System.out.println(">>> PropertyService: Getting all properties.");
        return propertyRepository.findAll();
    }

    /**
     * Gets all properties managed by a specific agent.
     */
    public List<Property> getPropertiesByAgentId(String agentId) {
        System.out.println(">>> PropertyService: Getting properties for agent ID: " + agentId);
        return propertyRepository.findByAgentId(agentId); // Assumes findByAgentId exists in repo
    }

    /**
     * Adds a new property, generating an ID if necessary, and saves it.
     */
    public Property addProperty(Property property) {
        System.out.println(">>> PropertyService: Adding new property.");
        if (property == null) {
            throw new IllegalArgumentException("Cannot add null property.");
        }
        // Ensure ID is set
        if (property.getPropertyId() == null || property.getPropertyId().trim().isEmpty()) {
            property.setPropertyId(UUID.randomUUID().toString());
            System.out.println(">>> PropertyService: Generated new property ID: " + property.getPropertyId());
        }
        // Ensure status is set (optional default)
        if (property.getStatus() == null) {
            property.setStatus(Property.PropertyStatus.FOR_SALE);
        }
        return propertyRepository.save(property);
    }

    /**
     * Updates an existing property.
     */
    public Property updateProperty(Property property) {
        System.out.println(">>> PropertyService: Updating property ID: " + property.getPropertyId());
        if (property == null || property.getPropertyId() == null) {
            throw new IllegalArgumentException("Cannot update null property or property with null ID.");
        }
        // Check if property exists first? Optional, save will handle update vs create.
        // Optional<Property> existing = propertyRepository.findById(property.getPropertyId());
        // if (existing.isEmpty()) {
        //     throw new RuntimeException("Property not found for update with ID: " + property.getPropertyId());
        // }
        return propertyRepository.save(property); // save handles update
    }

    /**
     * Deletes a property by its ID.
     */
    public void deleteProperty(String propertyId) {
        System.out.println(">>> PropertyService: Deleting property ID: " + propertyId);
        if (propertyId == null || propertyId.trim().isEmpty()) {
            System.err.println("!!! PropertyService: Delete called with null or empty propertyId.");
            return;
        }
        // Optional: Check if property exists before calling delete?
        propertyRepository.deleteById(propertyId); // Assumes deleteById exists in repo
    }

}
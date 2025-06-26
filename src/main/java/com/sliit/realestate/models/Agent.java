package com.sliit.realestate.models;

import java.time.LocalDateTime;
import java.util.*;





public class Agent extends User {



    private String licenseNumber;

    private String specialization;

    private String location;

    private double averageRating;

    private int totalRatings;

    private List<String> managedProperties;

    private Map<String, LocalDateTime> availability;





    private List<Appointment> appointments;



    private String profileImageUrl; // For the profile picture URL

    private String experience; // For "Years of Experience"

    private String bio; // For "Professional Bio"





// Constructor with required fields (Flexible Initialization)

    public Agent(String userId, String username, String password, String fullName,

                 String email, String phone, String licenseNumber,

                 String specialization, String location) {

        super(userId, username, password, fullName, email, phone, "AGENT");

        this.licenseNumber = licenseNumber;

        this.specialization = specialization;

        this.location = location;

        this.averageRating = 0.0;

        this.totalRatings = 0;

        this.managedProperties = new ArrayList<>();

        this.availability = new HashMap<>();

        this.profileImageUrl = ""; // Default to empty or a placeholder URL

        this.experience = "0"; // Default to "0" or empty

        this.bio = "";

        this.appointments = new ArrayList<>();





    }



// Overloaded constructor with optional lists

    public Agent(String userId, String username, String password, String fullName,

                 String email, String phone, String licenseNumber,

                 String specialization, String location,

                 double averageRating, int totalRatings,

                 List<String> managedProperties, Map<String, LocalDateTime> availability) {

        super(userId, username, password, fullName, email, phone, "AGENT");

        this.licenseNumber = licenseNumber;

        this.specialization = specialization;

        this.location = location;

        this.averageRating = Math.max(0.0, averageRating); // Ensuring non-negative values

        this.totalRatings = Math.max(0, totalRatings);

        this.managedProperties = (managedProperties != null) ? new ArrayList<>(managedProperties) : new ArrayList<>();

        this.availability = (availability != null) ? new HashMap<>(availability) : new HashMap<>();

        this.profileImageUrl = ""; // Default to empty or a placeholder URL

        this.experience = "0"; // Default to "0" or empty

        this.bio = ""; // Default to empty

        this.appointments = new ArrayList<>();





    }



    public String getProfileImageUrl() {

        return profileImageUrl;

    }

    public void setProfileImageUrl(String profileImageUrl) {

        this.profileImageUrl = profileImageUrl;

    }



    public String getExperience() {

        return experience;

    }

    public void setExperience(String experience) {

        this.experience = experience;

    }



    public String getBio() {

        return bio;

    }

    public void setBio(String bio) {

        this.bio = bio;

    }



    public String getLicenseNumber() {

        return licenseNumber;

    }



    public String getSpecialization() {

        return specialization;

    }



    public void setSpecialization(String specialization) {

        if (specialization != null && !specialization.isEmpty()) {

            this.specialization = specialization;

        }

    }





    public List<Appointment> getAppointments() {

        return appointments == null ? new ArrayList<>() : new ArrayList<>(this.appointments);

    }



    public void setAppointments(List<Appointment> appointments) {

        this.appointments = (appointments != null) ? new ArrayList<>(appointments) : new ArrayList<>();

    }





    public String getLocation() {

        return location;

    }



    public void setLocation(String location) {

        if (location != null && !location.isEmpty()) {

            this.location = location;

        }

    }



    public double getAverageRating() {

        return averageRating;

    }



    public int getTotalRatings() {

        return totalRatings;

    }



// Method to add a new rating

    public void addRating(double rating) {

        if (rating < 0 || rating > 5) {

            throw new IllegalArgumentException("Rating must be between 0 and 5");

        }

        double totalScore = averageRating * totalRatings;

        totalRatings++;

        averageRating = (totalScore + rating) / totalRatings;

    }



    public List<String> getManagedProperties() {

        return new ArrayList<>(managedProperties); // Return a copy to prevent modification

    }



    public void addManagedProperty(String propertyId) {

        if (propertyId != null && !propertyId.isEmpty()) {

            managedProperties.add(propertyId);

        }

    }



    public void removeManagedProperty(String propertyId) {

        managedProperties.remove(propertyId);

    }



    public Map<String, LocalDateTime> getAvailability() {

        return new HashMap<>(availability); // Return a copy for safety

    }



    public void addAvailability(String timeSlotId, LocalDateTime dateTime) {

        if (timeSlotId != null && dateTime != null) {

            availability.put(timeSlotId, dateTime);

        }

    }



    public boolean removeAvailability(String timeSlotId) {

        if (this.availability == null) {

            this.availability = new HashMap<>(); // Ensure it's not null before trying to remove

        }

// Map.remove returns the value previously associated with the key, or null if no mapping.

// So, if it returns non-null, an item was removed.

        boolean wasRemoved = this.availability.remove(timeSlotId) != null;

        if (wasRemoved) {

            System.out.println("Agent " + this.getUserId() + ": Removed availability slot " + timeSlotId);

        } else {

            System.out.println("Agent " + this.getUserId() + ": Availability slot " + timeSlotId + " not found for removal.");

        }

        return wasRemoved;



    }



}


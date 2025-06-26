package com.sliit.realestate.models;

import java.util.ArrayList;
import java.util.List;


public class Client extends User {
    private List<String> favoriteProperties;
    private List<String> searchPreferences;
    private List<String> appointmentHistory;

    // Constructor with only required fields (flexible initialization)
    public Client(String userId, String username, String password, String fullName,
                  String email, String phone) {
        super(userId, username, password, fullName, email, phone,"CLIENT");
        this.favoriteProperties = new ArrayList<>();
        this.searchPreferences = new ArrayList<>();
        this.appointmentHistory = new ArrayList<>();
    }

    // Constructor that allows setting optional fields during initialization
    public Client(String userId, String username, String password, String fullName,
                  String email, String phone,
                  List<String> favoriteProperties, List<String> searchPreferences, List<String> appointmentHistory) {
        super(userId, username, password, fullName, email, phone,"CLIENT");
        this.favoriteProperties = (favoriteProperties != null) ? favoriteProperties : new ArrayList<>();
        this.searchPreferences = (searchPreferences != null) ? searchPreferences : new ArrayList<>();
        this.appointmentHistory = (appointmentHistory != null) ? appointmentHistory : new ArrayList<>();
    }

    // Additional getters and setters
    public List<String> getFavoriteProperties() {
        return new ArrayList<>(favoriteProperties); // Return a copy for safety
    }

    public void addFavoriteProperty(String propertyId) {
        if (propertyId != null && !propertyId.isEmpty()) {
            favoriteProperties.add(propertyId);
        }
    }

    public void removeFavoriteProperty(String propertyId) {
        favoriteProperties.remove(propertyId);
    }

    public List<String> getSearchPreferences() {
        return new ArrayList<>(searchPreferences);
    }

    public void setSearchPreferences(List<String> searchPreferences) {
        if (searchPreferences != null) {
            this.searchPreferences = new ArrayList<>(searchPreferences);
        }
    }

    public List<String> getAppointmentHistory() {
        return new ArrayList<>(appointmentHistory);
    }

    public void addAppointment(String appointmentId) {
        if (appointmentId != null && !appointmentId.isEmpty()) {
            appointmentHistory.add(appointmentId);
        }
    }
    public void setFavoriteProperties(List<String> favoriteProperties) {
        this.favoriteProperties = (favoriteProperties != null) ? new ArrayList<>(favoriteProperties) : new ArrayList<>();
    }

    public void setAppointmentHistory(List<String> appointmentHistory) {
        this.appointmentHistory = (appointmentHistory != null) ? new ArrayList<>(appointmentHistory) : new ArrayList<>();
    }


}

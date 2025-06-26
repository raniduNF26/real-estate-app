package com.sliit.realestate.models;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class User {
    private String userId;
    private String username;
    private String password; // Store hashed password
    private String fullName;
    private String email;
    private String phone;
    private LocalDateTime registrationDate;
    private boolean active;
    private String role;

    // Constructor: Hashes the password before storing it
    public User(String userId, String username, String password, String fullName, String email, String phone,String role) {
        this.userId = userId;
        this.username = username;
        this.password= password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.registrationDate = LocalDateTime.now();
        this.active = true;
        this.role = role != null ? role : "USER";
    }


    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password=password;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username != null && !username.isEmpty()) {
            this.username = username;
        }
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName != null && !fullName.isEmpty()) {
            this.fullName = fullName;
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email != null && email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            this.email = email;
        }
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone != null && phone.matches("^\\d{10}$")) {
            this.phone = phone;
        }
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
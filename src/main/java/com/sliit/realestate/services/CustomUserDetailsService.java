package com.sliit.realestate.services;

/*import com.sliit.realestate.models.Admin;*/
import com.sliit.realestate.models.User;
/*import com.sliit.realestate.repositories.AdminRepository;*/
import com.sliit.realestate.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Service // Ensure @Service annotation is present
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    // *** REMOVED: @Autowired AdminRepository ***

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println(">>> CustomUserDetailsService: Loading user: " + username);
        Optional<User> userOpt = userRepository.findByUsername(username);

        User user = userOpt.orElseThrow(() -> {
            System.err.println("!!! CustomUserDetailsService: User not found with username: " + username);
            return new UsernameNotFoundException("User not found with username: " + username);
        });

        System.out.println(">>> CustomUserDetailsService: Found user. Role: " + user.getRole() + ", Active: " + user.isActive());

        // Create Spring Security UserDetails object
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // The stored hash
                user.isActive(),    // enabled
                true,               // accountNonExpired
                true,               // credentialsNonExpired
                true,               // accountNonLocked
                getAuthorities(user)// Get roles/authorities
        );
    }

    /**
     * Gets the authorities (roles) for the user.
     * In this simplified version, it just adds ROLE_ADMIN, ROLE_AGENT, or ROLE_CLIENT.
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getRole() != null && !user.getRole().trim().isEmpty()) {
            // Add role-based authority (e.g., "ROLE_ADMIN", "ROLE_AGENT", "ROLE_CLIENT")
            String roleName = "ROLE_" + user.getRole().toUpperCase();
            authorities.add(new SimpleGrantedAuthority(roleName));
            System.out.println(">>> CustomUserDetailsService: Assigning authority: " + roleName);
        } else {
            System.err.println("!!! CustomUserDetailsService: User ID " + user.getUserId() + " has missing or empty role!");
            // Decide if user should have default role or no authorities if role is missing
        }


        // *** REMOVED: Logic block that tried to load permissions for ADMIN using AdminRepository ***

        return authorities;
    }

}
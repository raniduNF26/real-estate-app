package com.sliit.realestate.config;


import com.sliit.realestate.models.User;
import com.sliit.realestate.repositories.UserRepository;
import com.sliit.realestate.services.AdminService;
import com.sliit.realestate.services.AgentService;
import com.sliit.realestate.services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        System.out.println("CustomAuthenticationProvider: Attempting to authenticate...");

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        System.out.println("Attempting to authenticate user: " + username);

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String userType = user.getRole(); // Assuming getUserType() returns "ADMIN", "AGENT", or "CLIENT"
            System.out.println("User found in UserRepository with type: " + userType);

            boolean isAuthenticated = false;
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            switch (userType) {
                case "ADMIN":
                    isAuthenticated = adminService.authenticate(username, password);
                    if (isAuthenticated) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }
                    break;
                case "AGENT":
                    isAuthenticated = agentService.authenticate(username, password);
                    if (isAuthenticated) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_AGENT"));
                    }
                    break;
                case "CLIENT":
                    isAuthenticated = clientService.authenticate(username, password);
                    if (isAuthenticated) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_CLIENT"));
                    }
                    break;
            }

            if (isAuthenticated) {
                System.out.println("Authentication successful for user: " + username);
                System.out.println("Assigning authorities: " + authorities);


                Authentication authToken = new UsernamePasswordAuthenticationToken(username, password,authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("Created authentication token: " + authToken);
                System.out.println("Token authorities: " + authToken.getAuthorities());
                return authToken;
            } else {
                System.out.println("Authentication failed for user: " + username);
                throw new BadCredentialsException("Invalid credentials for user: " + username);
            }
        } else {
            System.out.println("User not found in UserRepository: " + username);
        }

        throw new BadCredentialsException("Invalid credentials");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
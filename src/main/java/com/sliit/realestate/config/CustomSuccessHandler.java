package com.sliit.realestate.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        System.out.println("✅ CustomSuccessHandler triggered");
        System.out.println("✅ User: " + authentication.getName());
        System.out.println("✅ Authorities: " + authentication.getAuthorities());

        // Get user roles
        String redirectUrl = "/dashboard";

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();

            if (role.equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/dashboard";
                break;
            } else if (role.equals("ROLE_AGENT")) {
                redirectUrl = "/agent/profile";
                break;
            } else if (role.equals("ROLE_CLIENT")) {
                redirectUrl = "/client/profile";
                break;
            }
        }
        System.out.println("Redirecting user to: " + redirectUrl);
        // fallback
        response.sendRedirect(redirectUrl);
    }
}

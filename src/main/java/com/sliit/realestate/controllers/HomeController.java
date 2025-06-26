package com.sliit.realestate.controllers; // Adjust package if needed

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal; // Import Principal

@Controller
public class HomeController {

    @GetMapping("/") // Map requests to the root path
    public String homePage(Principal principal) {
        // If user is already logged in, maybe redirect them to their dashboard? (Optional)
        if (principal != null) {
            // Note: We don't know the role here easily without fetching the user.
            // A simple redirect might be tricky. Let's just show the homepage for now.
            // Or, redirect to a generic logged-in page if you have one.
            // return "redirect:/user/dashboard"; // Example redirect if user is logged in
        }
        // Return the name of the new home page template
        return "index"; // This will look for templates/index.html
    }

    // Optional: Add mappings for About, Services, Contact later
    @GetMapping("/about")
    public String aboutPage() {
        return "about"; // Looks for templates/about.html
    }

    @GetMapping("/services")
    public String servicesPage() {
        return "services"; // Looks for templates/services.html
    }

    @GetMapping("/contact")
    public String contactPage() {
        return "contact"; // Looks for templates/contact.html
    }
}
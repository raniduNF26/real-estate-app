package com.sliit.realestate.controllers;




/*import com.sliit.realestate.models.Admin;*/
import com.sliit.realestate.models.Agent;
import com.sliit.realestate.models.Client;
import com.sliit.realestate.models.User;
import com.sliit.realestate.services.AdminService;
import com.sliit.realestate.services.AgentService;
import com.sliit.realestate.services.ClientService;
import com.sliit.realestate.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
public class AuthController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ClientService clientService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userType", "CLIENT"); // Default user type
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("userType") String userType,
            Model model) {

        // Generate a new UUID for user ID
        String userId = UUID.randomUUID().toString();

        try {
            switch (userType) {
                case "ADMIN":
                    // Only existing admins can create new admins in a real system
                    // For demo, allow admin creation via registration form
                    System.out.println(">>> AuthController: Registering ADMIN user: " + username);
                    // *** Create a standard User object with ADMIN role ***
                    User adminUser = new User(
                            userId,
                            username,
                            password, // UserService will hash this
                            fullName,
                            email,
                            phone,
                            "ADMIN" // Set the role explicitly
                    );
                    // *** Call UserService to create the user ***
                    userService.createUser(adminUser); // UserService saves to users.txt
                    System.out.println(">>> AuthController: ADMIN user created via UserService.");
                    break; // End of ADMIN cas

                case "AGENT":
                    // For agents, you'll need more details like license number, etc.
                    // For now use placeholder values that can be updated later
                    Agent agent = new Agent(
                            userId, username, password, fullName, email, phone,
                            "LICENSE-" + UUID.randomUUID().toString().substring(0, 8),
                            "General", "City Center"
                    );
                    agentService.createAgent(agent);
                    break;

                case "CLIENT":
                    Client client = new Client(userId, username, password, fullName, email, phone);
                    clientService.createClient(client);
                    break;

                default:
                    throw new IllegalArgumentException("Invalid user type");
            }

            return "redirect:/login?registered";

        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request) {
        HttpSession session = request.getSession();

        // Check the role attribute in session
        String userRole = (String) session.getAttribute("userRole");

        if (userRole == null) {
            return "redirect:/login";
        }

        // Redirect based on user role
        switch (userRole) {
            case "ROLE_ADMIN":
                return "redirect:/admin/dashboard";
            case "ROLE_AGENT":
                return "redirect:/agent/dashboard";
            case "ROLE_CLIENT":
                return "redirect:/client/dashboard";
            default:
                return "redirect:/home";
        }
    }
}
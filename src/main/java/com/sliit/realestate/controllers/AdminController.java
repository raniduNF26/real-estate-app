package com.sliit.realestate.controllers;
import com.sliit.realestate.models.Appointment; // Import necessary models
import com.sliit.realestate.models.Client;
import com.sliit.realestate.models.Agent;
import com.sliit.realestate.models.Property;
import com.sliit.realestate.models.User;
import com.sliit.realestate.services.AdminService; // Import AdminService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin") // Base path for all admin URLs
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * Displays the main admin dashboard.
     */
    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model) {
        System.out.println(">>> AdminController: Loading dashboard");
        // Example: Fetch some counts for the dashboard
        try {
            model.addAttribute("userCount", adminService.getAllUsers().size());
            model.addAttribute("propertyCount", adminService.getAllProperties().size());
            model.addAttribute("appointmentCount", adminService.getAllAppointments().size());
            // Add more stats as needed
        } catch (Exception e) {
            System.err.println("!!! AdminController: Error loading dashboard data: " + e.getMessage());
            model.addAttribute("dashboardError", "Failed to load some dashboard statistics.");
        }
        return "admin/dashboard"; // Renders templates/admin/dashboard.html
    }

    /**
     * Displays the list of all users (Admins, Agents, Clients).
     */
    @GetMapping("/users")
    public String listUsers(Model model) {
        System.out.println(">>> AdminController: Loading user list");
        try {
            List<User> users = adminService.getAllUsers();
            model.addAttribute("users", users);
        } catch (Exception e) {
            System.err.println("!!! AdminController: Error loading user list: " + e.getMessage());
            model.addAttribute("listError", "Failed to load user list.");
            model.addAttribute("users", new ArrayList<>()); // Send empty list
        }
        return "admin/user-list"; // Renders templates/admin/user-list.html
    }

    /**
     * Handles user activation request.
     */
    @PostMapping("/users/activate")
    public String activateUser(@RequestParam("userId") String userId, RedirectAttributes redirectAttributes) {
        System.out.println(">>> AdminController: Request to activate user ID: " + userId);
        try {
            boolean success = adminService.activateUser(userId);
            if (success) {
                redirectAttributes.addFlashAttribute("userSuccess", "User activated successfully.");
            } else {
                redirectAttributes.addFlashAttribute("userError", "User not found or already active.");
            }
        } catch (Exception e) {
            System.err.println("!!! AdminController: Error activating user " + userId + ": " + e.getMessage());
            redirectAttributes.addFlashAttribute("userError", "An error occurred during activation.");
        }
        return "redirect:/admin/users";
    }

    /**
     * Handles user deactivation request.
     */
    @PostMapping("/users/deactivate")
    public String deactivateUser(@RequestParam("userId") String userId, RedirectAttributes redirectAttributes) {
        System.out.println(">>> AdminController: Request to deactivate user ID: " + userId);
        try {
            boolean success = adminService.deactivateUser(userId);
            if (success) {
                redirectAttributes.addFlashAttribute("userSuccess", "User deactivated successfully.");
            } else {
                redirectAttributes.addFlashAttribute("userError", "User not found or already inactive.");
            }
        } catch (Exception e) {
            System.err.println("!!! AdminController: Error deactivating user " + userId + ": " + e.getMessage());
            redirectAttributes.addFlashAttribute("userError", "An error occurred during deactivation.");
        }
        return "redirect:/admin/users";
    }

    /**
     * Handles user deletion request. USE WITH CAUTION.
     */
    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam("userId") String userId, RedirectAttributes redirectAttributes) {
        System.out.println(">>> AdminController: Request to DELETE user ID: " + userId);
        try {
            // Add checks here to prevent admin from deleting themselves or last admin if needed
            adminService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("userSuccess", "User deleted successfully.");
        } catch (Exception e) {
            System.err.println("!!! AdminController: Error deleting user " + userId + ": " + e.getMessage());
            redirectAttributes.addFlashAttribute("userError", "An error occurred during deletion.");
        }
        return "redirect:/admin/users";
    }


    // --- Mappings for Properties and Appointments (similar structure) ---

    @GetMapping("/properties")
    public String listProperties(Model model) {
        System.out.println(">>> AdminController: Loading property list");
        try {
            List<Property> properties = adminService.getAllProperties();
            model.addAttribute("properties", properties);
        } catch (Exception e) {
            System.err.println("!!! AdminController: Error loading property list: " + e.getMessage());
            model.addAttribute("listError", "Failed to load property list.");
            model.addAttribute("properties", new ArrayList<>());
        }
        return "admin/property-list"; // Renders templates/admin/property-list.html
    }

    @PostMapping("/properties/delete")
    public String deleteProperty(@RequestParam("propertyId") String propertyId, RedirectAttributes redirectAttributes) {
        System.out.println(">>> AdminController: Request to DELETE property ID: " + propertyId);
        try {
            adminService.deleteProperty(propertyId);
            redirectAttributes.addFlashAttribute("propertySuccess", "Property deleted successfully.");
        } catch (Exception e) {
            System.err.println("!!! AdminController: Error deleting property " + propertyId + ": " + e.getMessage());
            redirectAttributes.addFlashAttribute("propertyError", "An error occurred during deletion.");
        }
        return "redirect:/admin/properties";
    }


    @GetMapping("/appointments")
    public String listAppointments(Model model) {
        System.out.println(">>> AdminController: Loading appointment list");
        try {
            List<Appointment> appointments = adminService.getAllAppointments();
            // Optional: Fetch client/agent names here if needed for display, similar to agent dashboard
            model.addAttribute("appointments", appointments);
        } catch (Exception e) {
            System.err.println("!!! AdminController: Error loading appointment list: " + e.getMessage());
            model.addAttribute("listError", "Failed to load appointment list.");
            model.addAttribute("appointments", new ArrayList<>());
        }
        return "admin/appointment-list"; // Renders templates/admin/appointment-list.html
    }

    @PostMapping("/appointments/delete")
    public String deleteAppointment(@RequestParam("appointmentId") String appointmentId, RedirectAttributes redirectAttributes) {
        System.out.println(">>> AdminController: Request to DELETE appointment ID: " + appointmentId);
        try {
            adminService.deleteAppointment(appointmentId);
            redirectAttributes.addFlashAttribute("appointmentSuccess", "Appointment deleted successfully.");
        } catch (Exception e) {
            System.err.println("!!! AdminController: Error deleting appointment " + appointmentId + ": " + e.getMessage());
            redirectAttributes.addFlashAttribute("appointmentError", "An error occurred during deletion.");
        }
        return "redirect:/admin/appointments";
    }

}
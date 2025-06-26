package com.sliit.realestate.controllers;

import com.sliit.realestate.models.Agent;
import com.sliit.realestate.models.Appointment;
import com.sliit.realestate.models.Client;
import com.sliit.realestate.models.Property;
import com.sliit.realestate.repositories.AppointmentRepository;
import com.sliit.realestate.services.AgentService;
import com.sliit.realestate.services.ClientService;
import com.sliit.realestate.services.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;
    @Autowired
    private PropertyService propertyService;
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ClientService clientService;

    private Optional<Agent> getLoggedInAgent(Principal principal) {
        if (principal == null || principal.getName() == null) return Optional.empty();
        // AgentService needs a method like getCompleteAgentByUsername
        // Assuming agentService.getAgentByUsername now returns the complete merged object
        return Optional.ofNullable(agentService.getAgentByUsername(principal.getName()));
    }

    @GetMapping("/dashboard") // This mapping should be correct
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        System.out.println(">>> Agent Dashboard: Loading for username: " + username);

        Agent currentAgent = agentService.getAgentByUsername(username); // Assumes this gets complete agent
        if (currentAgent == null) {
            System.out.println("!!! Agent Dashboard: Agent not found for username: " + username);
            return "redirect:/login?error=AgentNotFound";
        }
        model.addAttribute("agent", currentAgent);
        System.out.println(">>> Agent Dashboard: Found agent: " + currentAgent.getFullName());

        // --- Get Appointments & Client Names
        List<Appointment> appointments = new ArrayList<>();
        Map<String, Client> clientDetailsMap = new HashMap<>(); // Store full Client for more details
        try {
            appointments = appointmentRepository.findByAgentId(currentAgent.getUserId());
            System.out.println(">>> Agent Dashboard: Found " + appointments.size() + " appointments.");
            if (!appointments.isEmpty()) {
                Set<String> clientIds = appointments.stream().map(Appointment::getClientId).filter(Objects::nonNull).collect(Collectors.toSet());
                for (String clientId : clientIds) {
                    Optional<Client> clientOpt = clientService.getCompleteClientById(clientId);
                    clientDetailsMap.put(clientId, clientOpt.orElse(null));
                }
            }
        } catch (Exception e) {
            System.err.println("!!! Agent Dashboard: Error fetching appointments/client details: " + e.getMessage());
            model.addAttribute("dashboardError", "Could not load appointment details.");
        }
        model.addAttribute("appointments", appointments);
        model.addAttribute("clientDetailsMap", clientDetailsMap);

        List<Property> managedPropertyList = new ArrayList<>();
        List<String> managedPropertyIds = currentAgent.getManagedProperties();
        if (managedPropertyIds != null && !managedPropertyIds.isEmpty()) {
            try {
                managedPropertyList = propertyService.getPropertiesByIds(managedPropertyIds);
                System.out.println(">>> Agent Dashboard: Fetched " + managedPropertyList.size() + " managed property objects.");
            } catch (Exception e) {
                System.err.println("!!! Agent Dashboard: Error fetching managed properties: " + e.getMessage());
                model.addAttribute("propertyError", "Could not load managed property details.");
            }
        }
        model.addAttribute("managedPropertyList", managedPropertyList);






        List<Map.Entry<String, LocalDateTime>> upcomingAvailability = new ArrayList<>();
        if (currentAgent.getAvailability() != null) {
            upcomingAvailability = currentAgent.getAvailability().entrySet().stream()
                    .filter(entry -> entry.getValue().isAfter(LocalDateTime.now())) // Only future slots
                    .sorted(Map.Entry.comparingByValue()) // Sort by date-time
                    .limit(5) // Take the next 5
                    .collect(Collectors.toList());
            System.out.println(">>> Agent Dashboard: Fetched " + upcomingAvailability.size() + " upcoming availability slots.");
        }
        model.addAttribute("upcomingAvailability", upcomingAvailability);


        model.addAttribute("currentDateFormatted", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        System.out.println(">>> Agent Dashboard: Rendering template with all data.");
        return "agent/dashboard";
    }



    @GetMapping("appointments")
    public String viewAgentAppointments(Principal principal, Model model) {
        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) {
            return "redirect:/login";
        }
        Agent agent = agentOpt.get();
        model.addAttribute("agent", agent);

        System.out.println(">>> Agent Appointments Page: Loading for agent ID: " + agent.getUserId());

        // --- Get Appointments ---
        List<Appointment> appointments = new ArrayList<>();
        // *** Map to store full Client objects by Client ID ***
        Map<String, Client> clientDetailsMap = new HashMap<>();
        try {
            // Fetch ALL appointments for the agent
            appointments = appointmentRepository.findByAgentId(agent.getUserId());
            System.out.println(">>> Agent Appointments Page: Found " + appointments.size() + " total appointments.");

            // --- Get Full Client Details ---
            if (!appointments.isEmpty()) {
                Set<String> clientIds = appointments.stream()
                        .map(Appointment::getClientId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                System.out.println(">>> Agent Appointments Page: Fetching details for client IDs: " + clientIds);
                for (String clientId : clientIds) {
                    Optional<Client> clientOpt = clientService.getCompleteClientById(clientId);
                    clientDetailsMap.put(clientId, clientOpt.orElse(null)); // Store Client object or null
                }
                System.out.println(">>> Agent Appointments Page: Client details map created. Size: " + clientDetailsMap.size());
            }
        } catch (Exception e) {
            System.err.println("!!! Agent Appointments Page: Error fetching appointments or client details: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("appointmentError", "Could not load appointment details.");
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("clientDetailsMap", clientDetailsMap); // Pass the map

        return "agent/appointments"; // Return name of the NEW template
    }

    @PostMapping("/appointments/confirm")
    public String handleConfirmAppointment(
            @RequestParam("appointmentId") String appointmentId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) {
            return "redirect:/login";
        }
        String agentId = agentOpt.get().getUserId();

        try {
            boolean confirmed = agentService.confirmAppointment(appointmentId, agentId);

            if (confirmed) {
                redirectAttributes.addFlashAttribute("success", "Appointment confirmed successfully.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Appointment could not be confirmed (may not be PENDING or not found).");
            }
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to confirm this appointment.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during confirmation.");
        }

        return "redirect:/agent/appointments";
    }

    @PostMapping("/appointments/cancel") // Matches the form action
    public String handleCancelAppointmentByAgent( // Use a distinct name
                                                  @RequestParam("appointmentId") String appointmentId,
                                                  Principal principal,
                                                  RedirectAttributes redirectAttributes) {

        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) {
            return "redirect:/login";
        }
        String agentId = agentOpt.get().getUserId(); // ID of agent cancelling

        System.out.println(">>> AgentController: Received agent request to cancel appointment: " + appointmentId + " by agent ID: " + agentId);

        try {

            boolean cancelled = agentService.cancelAppointmentByAgent(appointmentId, agentId);

            if (cancelled) {
                redirectAttributes.addFlashAttribute("appointmentSuccess", "Appointment cancelled successfully.");
            } else {
                // This case covers appointment not found or already in a non-cancellable state
                redirectAttributes.addFlashAttribute("appointmentError", "Appointment could not be cancelled (may already be cancelled/completed or not found).");
            }
        } catch (SecurityException e) {
            // Catch the specific security exception thrown by the service method
            System.err.println("!!! AgentController: Agent " + agentId + " authorization failed for cancelling appointment " + appointmentId + ": " + e.getMessage());
            redirectAttributes.addFlashAttribute("appointmentError", e.getMessage()); // Pass the specific message from the service
        } catch (Exception e) {
            System.err.println("!!! AgentController: Error cancelling appointment " + appointmentId + " by agent: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("appointmentError", "An unexpected error occurred while cancelling the appointment.");
        }

        // Redirect back to the agent's appointments list
        return "redirect:/agent/appointments";
    }


    @GetMapping("/profile")
    public String viewProfile(Principal principal, Model model) {
        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) {
            return "redirect:/login";
        }
        Agent agent = agentOpt.get();
        model.addAttribute("agent", agent);

        long activeListingsCount = 0;
        if (agent.getManagedProperties() != null && !agent.getManagedProperties().isEmpty()) {
            List<Property> properties = propertyService.getPropertiesByIds(agent.getManagedProperties());
            activeListingsCount = properties.stream()
                    .filter(p -> p.getStatus() == Property.PropertyStatus.FOR_SALE || p.getStatus() == Property.PropertyStatus.FOR_RENT)
                    .count();
        }
        model.addAttribute("activeListings", activeListingsCount); // Name matches new HTML

        long upcomingAppointmentsCount = 0;
        if (appointmentRepository != null) { // Check if injected
            List<Appointment> appointments = appointmentRepository.findByAgentId(agent.getUserId());
            upcomingAppointmentsCount = appointments.stream()
                    .filter(a -> a.getDateTime().isAfter(LocalDateTime.now()) &&
                            (a.getStatus() == Appointment.AppointmentStatus.PENDING || a.getStatus() == Appointment.AppointmentStatus.CONFIRMED))
                    .count();
        }
        model.addAttribute("upcomingAppointments", upcomingAppointmentsCount); // Name matches new HTML

        long closedDealsCount = 0; // Placeholder - Define what "Closed Deals" means
        if (appointmentRepository != null) {
            List<Appointment> appointments = appointmentRepository.findByAgentId(agent.getUserId());
            closedDealsCount = appointments.stream()
                    .filter(a-> a.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                    .count();
        }
        model.addAttribute("closedDeals", closedDealsCount); // Name matches new HTML

        return "agent/profile";
    }



    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("userId") String userId,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("licenseNumber") String licenseNumber,
            @RequestParam("specialization") String specialization,
            @RequestParam("location") String location,

            @RequestParam(value="experience", required=false) String experience,
            @RequestParam(value="bio", required=false) String bio,

            @RequestParam(value="profileImageUrl", required=false) String profileImageUrl,

            Principal principal,
            RedirectAttributes redirectAttributes) {

        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty() || !agentOpt.get().getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "Authorization error.");
            return "redirect:/login";
        }

        Agent agent = agentOpt.get();
        agent.setFullName(fullName);
        agent.setEmail(email);
        agent.setPhone(phone);
        agent.setSpecialization(specialization);
        agent.setLocation(location);

        // *** SET NEW FIELDS ***
        if (experience != null) agent.setExperience(experience);
        if (bio != null) agent.setBio(bio);
        if (profileImageUrl != null) agent.setProfileImageUrl(profileImageUrl);
        // **********************

        try {
            agentService.updateAgent(agent);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully."); // Changed key
        } catch (Exception e) {
            System.err.println("!!! AgentController: Error updating agent profile: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Profile update failed."); // Changed key
        }
        return "redirect:/agent/profile";
    }

    @GetMapping("/availability")
    public String manageAvailability(Authentication authentication, Model model) {
        String username= authentication.getName();


        List<Agent> agents = agentService.getAllAgentsSortedByRating();
        Agent agent = agents.stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (agent == null) {
            return "redirect:/login";
        }

        model.addAttribute("agent", agent);
        model.addAttribute("availability", agent.getAvailability());

        return "agent/availability";
    }

    @PostMapping("/availability/add")
    public String addAvailability(
            @RequestParam("date") String date,
            @RequestParam("time") String time,
           Authentication authentication) {

        String username= authentication.getName();


        List<Agent> agents = agentService.getAllAgentsSortedByRating();
        Agent agent = agents.stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (agent == null) {
            return "redirect:/login";
        }


        LocalDateTime dateTime = LocalDateTime.parse(date + "T" + time + ":00");
        String timeSlotId = date + "-" + time;

        agentService.addAvailability(agent.getUserId(), timeSlotId, dateTime);

        return "redirect:/agent/availability?added";
    }

    @PostMapping("/availability/remove")
    public String removeAvailabilitySlot(
            @RequestParam("timeSlotId") String timeSlotId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) {
            return "redirect:/login";
        }

        try {
            boolean removed = agentService.removeAvailability(agentOpt.get().getUserId(), timeSlotId);

            if (removed) {
                redirectAttributes.addFlashAttribute("success", "Availability slot removed successfully.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Could not remove the specified time slot.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred.");
        }

        return "redirect:/agent/availability";
    }

    @GetMapping("/properties")
    public String managedProperties(Principal principal, Model model) { // Use Principal
        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) { return "redirect:/login"; }
        Agent agent = agentOpt.get();

        model.addAttribute("agent", agent);

        // Get the list of property IDs managed by the agent
        List<String> managedPropertyIds = agent.getManagedProperties();

        // *** Fetch the full Property objects using the PropertyService ***
        List<Property> managedPropertyList = new ArrayList<>(); // Initialize empty list
        if (managedPropertyIds != null && !managedPropertyIds.isEmpty()) {
            try {
                System.out.println(">>> AgentController: Fetching property details for IDs: " + managedPropertyIds);
                // Call the service method to get properties by IDs
                managedPropertyList = propertyService.getPropertiesByIds(managedPropertyIds);
                System.out.println(">>> AgentController: Fetched " + managedPropertyList.size() + " property objects.");
            } catch (Exception e) {
                System.err.println("!!! AgentController: Error fetching property details for agent " + agent.getUserId() + ": " + e.getMessage());
                e.printStackTrace(); // Log stack trace for debugging
                model.addAttribute("propertyError", "Could not load property details. Please try again later.");
            }
        } else {
            System.out.println(">>> AgentController: Agent " + agent.getUserId() + " manages 0 properties.");
        }

        // *** Add the LIST OF ACTUAL PROPERTY OBJECTS to the model ***
        model.addAttribute("properties", managedPropertyList); // Name it "properties" as expected by template

        return "agent/properties"; // Ensure templates/agent/properties.html exists and uses Property objects
    }

    @PostMapping("/properties/add")
    public String addProperty(
            // Request Parameters matching the new form fields
            @RequestParam("address") String address,
            @RequestParam("price") BigDecimal price, // Spring can convert String to BigDecimal
            @RequestParam("type") String type,         // e.g., HOUSE, APARTMENT
            @RequestParam("description") String description,
            @RequestParam(value="imageUrl", required=false) String imageUrl, // Optional image URL
            @RequestParam("bedrooms") int bedrooms,
            @RequestParam("bathrooms") int bathrooms,
            @RequestParam("areaSqFt") double areaSqFt,
            @RequestParam("status") Property.PropertyStatus status, // Expecting FOR_SALE, FOR_RENT etc.
            Principal principal,
            RedirectAttributes redirectAttributes) { // For success/error messages

        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) {
            // Should not happen if security is correct, but good practice
            redirectAttributes.addFlashAttribute("error", "Agent not logged in.");
            return "redirect:/login";
        }
        Agent agent = agentOpt.get();

        // Basic validation (add more as needed)
        if (address == null || address.trim().isEmpty() || price == null || price.compareTo(BigDecimal.ZERO) <= 0 || type == null || type.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Address, Price, and Type are required.");
            // Redirect back to the form page (need a GET mapping for it)
            return "redirect:/agent/properties/add"; // Redirect to the GET mapping below
        }


        try {

            Property newProperty = new Property(

                    null,
                    agent.getUserId(),
                    address,
                    price,
                    type,
                    status, // Status from the form
                    description,
                    imageUrl,
                    bedrooms,
                    bathrooms,
                    areaSqFt
            );

            // 2. Save the new Property using the PropertyService
            Property savedProperty = propertyService.addProperty(newProperty); // addProperty handles setting ID and saving
            System.out.println(">>> AgentController: Added new property ID: " + savedProperty.getPropertyId());

            // 3. Update the Agent's list of managed property IDs
            agent.addManagedProperty(savedProperty.getPropertyId()); // Assumes Agent has addManagedProperty method
            agentService.updateAgent(agent); // Save the updated agent (updates users.txt and agents.txt)
            System.out.println(">>> AgentController: Updated agent's managed properties list.");

            redirectAttributes.addFlashAttribute("success", "Property listing created successfully!");
            return "redirect:/agent/properties"; // Redirect to the properties list page

        } catch (Exception e) {
            System.err.println("!!! AgentController: Error adding property for agent " + agent.getUserId() + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to create property listing. Please check details and try again.");
            // Redirect back to the form page
            return "redirect:/agent/properties/add";
        }
    }

    @GetMapping("/properties/add")
    public String showAddPropertyForm(Principal principal, Model model) {
        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) {
            // Redirect to login if agent not found (shouldn't happen if secured)
            return "redirect:/login";
        }

        model.addAttribute("property", new Property());
        model.addAttribute("agent", agentOpt.get());
        model.addAttribute("propertyStatuses", Property.PropertyStatus.values());

        return "agent/add-property";
    }


    @PostMapping("/properties/delete")
    public String deleteProperty(@RequestParam("propertyId") String propertyId, Principal principal, RedirectAttributes redirectAttributes) {
        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) { return "redirect:/login"; }
        Agent agent = agentOpt.get();

        // Security Check: Ensure the agent requesting delete actually manages this property
        if (agent.getManagedProperties() == null || !agent.getManagedProperties().contains(propertyId)) {
            System.err.println("!!! AgentController: SECURITY ALERT - Agent " + agent.getUserId() + " attempted to delete property " + propertyId + " they don't manage.");
            redirectAttributes.addFlashAttribute("error", "You are not authorized to delete this property.");
            return "redirect:/agent/properties";
        }

        try {
            propertyService.deleteProperty(propertyId);

            // 2. Remove from agent's managed list
            agent.removeManagedProperty(propertyId);
            agentService.updateAgent(agent);

            redirectAttributes.addFlashAttribute("success", "Property deleted successfully.");

        } catch (Exception e) {
            System.err.println("!!! AgentController: Error deleting property " + propertyId + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to delete property.");
        }
        return "redirect:/agent/properties";
    }

    @GetMapping("/properties/view/{id}")
    public String viewAgentPropertyDetails(
            @PathVariable("id") String propertyId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) {
            return "redirect:/login";
        }

        Agent agent = agentOpt.get();
        model.addAttribute("agent", agent);

        Optional<Property> propertyOpt = propertyService.getPropertyById(propertyId);

        if (propertyOpt.isPresent()) {
            Property property = propertyOpt.get();

            // Security Check: Verify property belongs to the agent
            if (!agent.getUserId().equals(property.getAgentId())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to view this property.");
                return "redirect:/agent/properties";
            }

            model.addAttribute("property", property);
            return "agent/property-view-detail";

        } else {
            redirectAttributes.addFlashAttribute("error", "Property not found.");
            return "redirect:/agent/properties";
        }
    }


    @GetMapping("/properties/edit/{id}")
    public String showEditPropertyForm(
            @PathVariable("id") String propertyId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Agent> agentOpt = getLoggedInAgent(principal);
        if (agentOpt.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("agent", agentOpt.get());

        Optional<Property> propertyOpt = propertyService.getPropertyById(propertyId);

        if (propertyOpt.isPresent()) {
            Property property = propertyOpt.get();
            // Security Check: Verify property belongs to the agent
            if (!agentOpt.get().getUserId().equals(property.getAgentId())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to edit this property.");
                return "redirect:/agent/properties";
            }

            model.addAttribute("property", property);
            model.addAttribute("propertyStatuses", Property.PropertyStatus.values());
            return "agent/property-edit-form";

        } else {
            redirectAttributes.addFlashAttribute("error", "Property not found.");
            return "redirect:/agent/properties";
        }
    }

    @PostMapping("/properties/update")
    public String updateProperty(
            @RequestParam("propertyId") String propertyId,
            @RequestParam("address") String address,
            @RequestParam("price") BigDecimal price,
            @RequestParam("type") String type,
            @RequestParam("description") String description,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(name = "bedrooms", defaultValue = "0") int bedrooms,
            @RequestParam(name = "bathrooms", defaultValue = "0") int bathrooms,
            @RequestParam(name = "areaSqFt", defaultValue = "0.0") double areaSqFt,
            @RequestParam("status") Property.PropertyStatus status,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Optional<Agent> agentOpt = getLoggedInAgent(principal);

        try {
            Optional<Property> originalPropertyOpt = propertyService.getPropertyById(propertyId);
            if (originalPropertyOpt.isEmpty()) { /* handle error */ }
            Property propertyToUpdate = originalPropertyOpt.get();
            if (!agentOpt.get().getUserId().equals(propertyToUpdate.getAgentId())) { /* handle error */ }

            propertyToUpdate.setAddress(address);
            propertyToUpdate.setPrice(price);
            propertyToUpdate.setType(type);
            propertyToUpdate.setDescription(description);
            propertyToUpdate.setImageUrl(imageUrl);
            propertyToUpdate.setBedrooms(bedrooms);
            propertyToUpdate.setBathrooms(bathrooms);
            propertyToUpdate.setAreaSqFt(areaSqFt);
            propertyToUpdate.setStatus(status);

            propertyService.updateProperty(propertyToUpdate);

            redirectAttributes.addFlashAttribute("success", "Property updated successfully!");
            return "redirect:/agent/properties";

        } catch (Exception e) {
            System.err.println("!!! AgentController: Error updating property " + propertyId + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to update property.");
            return "redirect:/agent/properties/edit/" + propertyId;
        }
    }
}
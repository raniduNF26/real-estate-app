package com.sliit.realestate.controllers; // Ensure this matches your package structure

import com.sliit.realestate.models.Agent; // Import Agent model
import com.sliit.realestate.models.Appointment; // Import Appointment model
import com.sliit.realestate.models.Client; // Import Client model
import com.sliit.realestate.models.Property; // Import Property model (if needed elsewhere, already present)
import com.sliit.realestate.services.AgentService; // Ensure AgentService is imported
import com.sliit.realestate.services.ClientService; // Ensure ClientService is imported
import com.sliit.realestate.services.PropertyService; // Ensure PropertyService is imported
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*; // Import necessary collections
import java.util.stream.Collectors; // Import Collectors

@Controller
@RequestMapping("/client")
public class ClientController {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private AgentService agentService;

    private Optional<Client> getLoggedInClient(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return Optional.empty();
        }
        return clientService.getCompleteClientByUsername(principal.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty()) {
            return "redirect:/login?error=ClientNotFound";
        }
        model.addAttribute("client", clientOpt.get());
        return "client/dashboard";
    }

    @GetMapping("/profile")
    public String viewProfile(Principal principal, Model model) {
        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty()) {
            return "redirect:/login?error=ClientNotFound";
        }
        model.addAttribute("client", clientOpt.get());
        return "client/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("userId") String userId,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            Principal principal) {

        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty() || !clientOpt.get().getUserId().equals(userId)) {
            return "redirect:/login?error=AuthMismatch";
        }

        Client client = clientOpt.get();
        client.setFullName(fullName);
        client.setEmail(email);
        client.setPhone(phone);

        try {
            clientService.updateClient(client);
            return "redirect:/client/profile?updated";
        } catch (Exception e) {
            return "redirect:/client/profile?error=UpdateFailed";
        }
    }

    @GetMapping("/favorites")
    public String viewFavorites(Principal principal, Model model) {
        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty()) {
            return "redirect:/login?error=ClientNotFound";
        }
        Client client = clientOpt.get();
        model.addAttribute("client", client); // Add client object

        // 1. Get the list of favorite property IDs
        List<String> favoritePropertyIds = client.getFavoriteProperties();
        System.out.println(">>> ClientController.viewFavorites: Found " + (favoritePropertyIds == null ? 0 : favoritePropertyIds.size()) + " favorite IDs.");

        // 2. Fetch the full Property objects using the PropertyService
        List<Property> actualFavoriteProperties = new ArrayList<>(); // Initialize empty list
        if (favoritePropertyIds != null && !favoritePropertyIds.isEmpty()) {
            try {
                actualFavoriteProperties = propertyService.getPropertiesByIds(favoritePropertyIds);
                System.out.println(">>> ClientController.viewFavorites: Fetched " + actualFavoriteProperties.size() + " full favorite property objects.");
            } catch (Exception e) {
                System.err.println("!!! ClientController: Error fetching favorite property details for client " + client.getUserId() + ": " + e.getMessage());
                model.addAttribute("propertyError", "Could not load favorite property details.");
            }
        }

        // *** 3. CRITICAL: Add the List<Property> to the model ***
        // Use the SAME attribute name "favoriteProperties" that the template expects
        model.addAttribute("favoriteProperties", actualFavoriteProperties);

        return "client/favorites";
    }

    @PostMapping("/favorites/add")
    public String addFavorite(
            @RequestParam("propertyId") String propertyId,
            Principal principal) {
        System.out.println(">>> ClientController: addFavorite POST request received.");
        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty()) {
            return "redirect:/login?error=ClientNotFound";
        }

        try {
            clientService.addFavoriteProperty(clientOpt.get().getUserId(), propertyId);
            return "redirect:/client/favorites?added";
        } catch (Exception e) {
            return "redirect:/client/favorites?error=AddFailed";
        }
    }

    @PostMapping("/favorites/remove")
    public String removeFavorite(
            @RequestParam("propertyId") String propertyId,
            Principal principal) {

        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty()) {
            return "redirect:/login?error=ClientNotFound";
        }

        try {
            clientService.removeFavoriteProperty(clientOpt.get().getUserId(), propertyId);
            return "redirect:/client/favorites?removed";
        } catch (Exception e) {
            return "redirect:/client/favorites?error=RemoveFailed";
        }
    }

    @GetMapping("/agents")
    public String viewAgents(
            Model model,
            Principal principal,
            // Accept both optional parameters
            @RequestParam(name="minRating", required=false) Double minRating,
            @RequestParam(name="searchTerm", required=false) String searchTerm
    ) {

        System.out.println(">>> ClientController.viewAgents: Loading agents. minRating=" + minRating + ", searchTerm=" + searchTerm);

        getLoggedInClient(principal).ifPresent(client -> model.addAttribute("client", client));

        try {
            // Call the service method, passing BOTH filter values
            List<Agent> agents = agentService.getAgentsFilteredAndSorted(minRating, searchTerm);
            model.addAttribute("agents", agents);

            // Pass parameters back to view to keep form state
            model.addAttribute("selectedMinRating", minRating);
            model.addAttribute("searchTerm", searchTerm); // Allows th:value on input

        } catch (Exception e) {
            System.err.println("!!! ClientController: Error fetching filtered/sorted agents: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("agentListError", "Could not load agents.");
            model.addAttribute("agents", new ArrayList<>()); // Send empty list on error
        }

        return "client/agents";
    }

    @GetMapping("/agent/{agentProfileId}")
    public String viewAgentProfileForClient(
            @PathVariable("agentProfileId") String agentProfileId,
            Principal principal,
            Model model) {

        System.out.println(">>> ClientController: Request received for viewAgentProfileForClient, ID: " + agentProfileId);

        // Add logged-in client info to model if available
        getLoggedInClient(principal).ifPresent(client -> model.addAttribute("client", client));

        // Fetch the COMPLETE agent details
        Optional<Agent> agentOpt = agentService.getCompleteAgentById(agentProfileId);

        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            System.out.println(">>> ClientController: Found agent: " + agent.getUsername());
            // Add the full agent object to the model
            model.addAttribute("agent", agent);

            // Fetch the properties managed by this agent
            List<String> managedPropertyIds = agent.getManagedProperties(); // Get IDs from Agent object
            List<Property> managedPropertyList = new ArrayList<>();
            if (managedPropertyIds != null && !managedPropertyIds.isEmpty()) {
                try {
                    // Use PropertyService to get full Property objects
                    managedPropertyList = propertyService.getPropertiesByIds(managedPropertyIds);
                    System.out.println(">>> ClientController: Fetched " + managedPropertyList.size() + " managed properties for agent.");
                } catch (Exception e) {
                    System.err.println("!!! ClientController: Error fetching managed properties for agent " + agent.getUserId() + ": " + e.getMessage());
                    model.addAttribute("propertyError", "Could not load managed properties.");
                }
            }
            // Add the List<Property> to the model
            model.addAttribute("managedProperties", managedPropertyList);

            // Return the view template name
            return "client/agent-profile-view";

        } else {
            // Agent not found
            System.out.println(">>> ClientController: Agent not found for ID: " + agentProfileId);
            return "redirect:/client/agents?error=AgentNotFound";
        }
    }

    @PostMapping("/agent/{agentProfileId}/rate") // Matches the form action
    public String submitAgentRating(
            @PathVariable("agentProfileId") String agentId,       // Agent being rated
            @RequestParam("ratingValue") double ratingValue,     // Rating value from radio buttons
            // @RequestParam(value="comments", required=false) String comments, // If adding comments
            Principal principal,                                // To verify client is logged in
            RedirectAttributes redirectAttributes) {

        // Ensure a client is logged in
        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("ratingError", "You must be logged in to rate an agent.");
            return "redirect:/login";
        }
        // Optional: Check if rating value is valid (1-5)
        if (ratingValue < 1 || ratingValue > 5) {
            redirectAttributes.addFlashAttribute("ratingError", "Invalid rating value submitted.");
            return "redirect:/client/agent/" + agentId; // Redirect back to agent profile
        }

        System.out.println(">>> ClientController: Received rating " + ratingValue + " for agent " + agentId + " from client " + clientOpt.get().getUserId());

        try {
            // Call the existing AgentService method to add the rating
            agentService.addRating(agentId, ratingValue);
            System.out.println(">>> ClientController: Rating added successfully via AgentService.");
            redirectAttributes.addFlashAttribute("ratingSuccess", "Thank you for your rating!");

        } catch (RuntimeException e) { // Catch exceptions from service (e.g., agent not found)
            System.err.println("!!! ClientController: Error adding rating: " + e.getMessage());
            redirectAttributes.addFlashAttribute("ratingError", "Could not submit rating: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("!!! ClientController: Unexpected error adding rating: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("ratingError", "An unexpected error occurred while submitting your rating.");
        }

        // Redirect back to the agent's profile page after submission
        return "redirect:/client/agent/" + agentId;
    }


    // GET Mapping to view client's appointments
    @GetMapping("/appointments")
    public String viewClientAppointments(Principal principal, Model model) { // Renamed method for clarity
        Optional<Client> clientOpt = getLoggedInClient(principal); // Use helper
        if (clientOpt.isEmpty()) {
            return "redirect:/login?error=ClientNotFound";
        }
        Client client = clientOpt.get();
        model.addAttribute("client", client); // Add client object itself if needed in template

        // 1. Get the list of appointment IDs from the client object
        List<String> appointmentHistoryIds = client.getAppointmentHistory();

        // 2. Fetch the full Appointment objects using the IDs via AgentService
        List<Appointment> actualAppointments = new ArrayList<>();
        if (appointmentHistoryIds != null && !appointmentHistoryIds.isEmpty()) {
            try {
                actualAppointments = agentService.getAppointmentsByIds(appointmentHistoryIds);
            } catch (Exception e) {
                System.err.println("!!! ClientController: Error fetching appointment details for client " + client.getUserId() + ": " + e.getMessage());
                model.addAttribute("appointmentError", "Could not load appointment details.");
            }
        }

        // 3. Add the list of actual Appointment objects to the model
        model.addAttribute("appointments", actualAppointments); // Use this list in the template

        // --- NEW: Fetch Agent Details for Appointments ---
        // Collect unique agent IDs from the fetched appointments
        Set<String> agentIds = actualAppointments.stream()
                .map(Appointment::getAgentId)
                .collect(Collectors.toSet());

        // Create a map to store agent details, keyed by agent ID
        Map<String, Agent> agentDetailsMap = new HashMap<>();
        if (!agentIds.isEmpty()) {
            try {
                // Fetch complete Agent objects for each unique agent ID
                for (String agentId : agentIds) {
                    agentService.getCompleteAgentById(agentId).ifPresent(agent -> agentDetailsMap.put(agentId, agent));
                }

                // Note: If your AgentService had a method like List<Agent> getAgentsByIds(Collection<String> ids),
                // that might be more efficient than fetching one by one.
                // Example:
                // List<Agent> agents = agentService.getAgentsByIds(agentIds);
                // agents.forEach(agent -> agentDetailsMap.put(agent.getUserId(), agent));

            } catch (Exception e) {
                System.err.println("!!! ClientController: Error fetching agent details for appointments: " + e.getMessage());
                // Optionally add an error message to the model if fetching agent details fails
                // model.addAttribute("agentDetailsError", "Could not load agent details.");
            }
        }
        // Add the map of agent details to the model so the Thymeleaf template can access it
        model.addAttribute("agentDetailsMap", agentDetailsMap);
        // --- END NEW ---

        return "client/appointments"; // Ensure templates/client/appointments.html exists
    }

    @PostMapping("/appointment/book")
    public String handleBookingRequest(
            @RequestParam("agentId") String agentId,
            @RequestParam("timeSlotId") String timeSlotId,
            @RequestParam("appointmentDateTime") String dateTimeString,
            @RequestParam(value="notes", required=false) String notes,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("bookingError", "You must be logged in to book.");
            return "redirect:/login";
        }
        String clientId = clientOpt.get().getUserId();

        System.out.println(">>> handleBookingRequest: Received parameters -> agentId=[" + agentId + "], timeSlotId=[" + timeSlotId + "], dateTimeString=[" + dateTimeString + "], notes=[" + notes + "]");

        LocalDateTime appointmentDateTime;
        try {
            // First try parsing with offset if present
            if (dateTimeString.contains("+") || dateTimeString.contains("-")) {
                // Handle timezone offset (with or without colon)
                String normalizedDateTime = dateTimeString.replaceFirst("(\\+\\d{2})(\\d{2})$", "$1:$2");
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(normalizedDateTime);
                appointmentDateTime = offsetDateTime.toLocalDateTime();
            } else {
                // Fall back to local date time parsing
                appointmentDateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            System.out.println(">>> handleBookingRequest: Parsed dateTime successfully: " + appointmentDateTime);

            if (appointmentDateTime.isBefore(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("bookingError", "Appointment date must be in the future.");
                return "redirect:/client/agents?error=PastDate";
            }
        } catch (DateTimeParseException e) {
            System.err.println("!!! handleBookingRequest: DateTimeParseException for input string [" + dateTimeString + "]: " + e.getMessage());
            redirectAttributes.addFlashAttribute("bookingError", "Invalid date/time format selected. Please use format: YYYY-MM-DDTHH:MM:SS");
            return "redirect:/client/agents?error=BadDateFormat";
        }

        try {
            Appointment bookedAppointment = agentService.bookNewAppointment(agentId, clientId, timeSlotId, appointmentDateTime, notes);
            redirectAttributes.addFlashAttribute("bookingSuccess", "Appointment booked successfully!");
            return "redirect:/client/appointments";

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("bookingError", e.getMessage());
            return "redirect:/client/agents?error=BookingFailed";
        } catch (Exception e) {
            System.err.println("!!! ClientController: Booking failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("bookingError", "Booking failed unexpectedly. Please try again.");
            return "redirect:/client/agents?error=BookingFailed";
        }
    }

    @GetMapping("/properties") // Or just "/properties" if not under "/client" prefix
    public String browseProperties(Model model, Principal principal) {

        // Optionally add client info if logged in
        getLoggedInClient(principal).ifPresent(client -> model.addAttribute("client", client));

        try {
            // Fetch all properties (or maybe just FOR_SALE/FOR_RENT ones)
            List<Property> properties = propertyService.getAllProperties().stream()
                    // Optional: Filter for active statuses if needed
                    .filter(p -> p.getStatus() == Property.PropertyStatus.FOR_SALE || p.getStatus() == Property.PropertyStatus.FOR_RENT)
                    .collect(Collectors.toList());

            model.addAttribute("properties", properties); // Add the list to the model
            System.out.println(">>> ClientController: Fetched " + properties.size() + " properties for Browse.");

        } catch (Exception e) {
            System.err.println("!!! ClientController: Error fetching properties for Browse: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("propertyError", "Could not load properties at this time.");
            // Optionally return an error view, or just return the view with an empty list/error message
        }

        return "property-browse";
    }

    @PostMapping("/appointment/cancel")
    public String handleCancelAppointment(
            @RequestParam("appointmentId") String appointmentId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty()) {
            return "redirect:/login?error=NotLoggedIn";
        }
        String clientId = clientOpt.get().getUserId();

        System.out.println(">>> ClientController: Received request to cancel appointment: " + appointmentId + " by client: " + clientId);

        try {
            boolean cancelled = agentService.cancelClientAppointment(appointmentId, clientId); // Call the service method
            if (cancelled) {
                redirectAttributes.addFlashAttribute("cancelSuccess", "Appointment cancelled successfully."); // Use a different attribute name
            } else {
                redirectAttributes.addFlashAttribute("cancelError", "Appointment could not be cancelled (it may already be cancelled/completed or not found).");
            }
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("cancelError", "You are not authorized to cancel this appointment.");
        } catch (Exception e) {
            System.err.println("!!! ClientController: Error cancelling appointment " + appointmentId + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("cancelError", "An unexpected error occurred while cancelling.");
        }

        return "redirect:/client/appointments"; // Redirect back to the appointments list
    }

    @PostMapping("/preferences/update")
    public String updatePreferences(
            @RequestParam("preferencesString") String preferencesString,
            Principal principal) {

        Optional<Client> clientOpt = getLoggedInClient(principal);
        if (clientOpt.isEmpty()) {
            return "redirect:/login";
        }

        try {
            List<String> preferences = preferencesString.isEmpty() ?
                    new ArrayList<>() : Arrays.asList(preferencesString.split(","));
            clientService.setSearchPreferences(clientOpt.get().getUserId(), preferences);
            return "redirect:/client/dashboard?preferences_updated";
        } catch (Exception e) {
            return "redirect:/client/dashboard?error=PrefsUpdateFailed";
        }
    }
}
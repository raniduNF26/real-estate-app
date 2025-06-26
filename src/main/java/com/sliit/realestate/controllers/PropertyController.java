package com.sliit.realestate.controllers;



import com.sliit.realestate.models.Agent;
import com.sliit.realestate.models.Client;
import com.sliit.realestate.models.Property;
import com.sliit.realestate.services.AgentService;
import com.sliit.realestate.services.ClientService;
import com.sliit.realestate.services.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // For filtering later

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/properties") // Base path for all property-related views
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private AgentService agentService; // Needed to show agent details on property view

    @Autowired
    private ClientService clientService; // Needed for client context (e.g., favorites)

    // Helper to get logged-in client (can be copied from ClientController)
    private Optional<Client> getLoggedInClient(Principal principal) {
        if (principal == null || principal.getName() == null) return Optional.empty();
        // Assuming ClientService has getCompleteClientByUsername which merges User+Client data
        return clientService.getCompleteClientByUsername(principal.getName());
    }

    /**
     * Displays a list of properties available for Browse (e.g., FOR_SALE, FOR_RENT).
     * Handles GET requests to /properties
     */
    @GetMapping // Maps to "/properties"
    public String browseProperties(
            Model model,
            Principal principal,
            // *** MODIFIED: Changed "typeFilter" to "type" to match form name ***
            @RequestParam(name = "type", required = false) String propertyType
    ) {
        System.out.println(">>> PropertyController: Request for browseProperties. Type Filter: " + propertyType);

        Optional<Client> clientOpt = getLoggedInClient(principal);
        clientOpt.ifPresent(client -> {
            model.addAttribute("client", client);
            // Pass client's favorite property IDs to the view
            model.addAttribute("clientFavorites", client.getFavoriteProperties());
        });
        // If no client is logged in, clientFavorites will be absent or you can pass an empty list
        if (clientOpt.isEmpty()) {
            model.addAttribute("clientFavorites", new ArrayList<String>());
        }


        try {
            // Pass the propertyType from the request to the service
            List<Property> properties = propertyService.getPublishedAndFilteredProperties(propertyType);

            model.addAttribute("properties", properties);
            // Pass the selected filter back to the view for the dropdown
            model.addAttribute("selectedType", propertyType);

            System.out.println(">>> PropertyController: Fetched " + properties.size() + " properties for Browse.");

        } catch (Exception e) {
            System.err.println("!!! PropertyController: Error fetching properties for Browse: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("propertyError", "Could not load properties at this time.");
            model.addAttribute("properties", new ArrayList<>()); // Send empty list on error
        }
        return "property-browse"; // Name of your template
    }

    /**
     * Displays the details page for a single property.
     * Handles GET requests to /properties/view/{id}
     */
    @GetMapping("/view/{id}")
    public String viewPropertyDetails(
            @PathVariable("id") String propertyId,
            Model model,
            Principal principal) { // Get principal to check if client is logged in

        System.out.println(">>> PropertyController: Request received for view property ID: " + propertyId);

        // Add client info to the model if logged in (for favorites button)
        Optional<Client> clientOpt = getLoggedInClient(principal);
        clientOpt.ifPresent(client -> model.addAttribute("client", client));
        List<String> clientFavorites = clientOpt.map(Client::getFavoriteProperties).orElse(new ArrayList<>());
        model.addAttribute("clientFavorites", clientFavorites); // Pass favorite IDs

        // Fetch the specific property
        Optional<Property> propertyOpt = propertyService.getPropertyById(propertyId);

        if (propertyOpt.isPresent()) {
            Property property = propertyOpt.get();
            model.addAttribute("property", property);
            System.out.println(">>> PropertyController: Found property: " + property.getAddress());

            // Fetch details of the agent managing this property
            if (property.getAgentId() != null) {
                Optional<Agent> agentOpt = agentService.getCompleteAgentById(property.getAgentId()); // Use the complete method
                if (agentOpt.isPresent()) {
                    model.addAttribute("agent", agentOpt.get());
                    System.out.println(">>> PropertyController: Found managing agent: " + agentOpt.get().getUsername());
                } else {
                    System.out.println(">>> PropertyController: Managing agent not found for ID: " + property.getAgentId());
                }
            }

            return "property-detail"; // Return name of the detail view template

        } else {
            // Property not found
            System.out.println(">>> PropertyController: Property not found ID: " + propertyId);
            // Redirect back to the main property browse page with an error
            return "redirect:/properties?error=NotFound";
        }
    }

}
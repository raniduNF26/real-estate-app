package com.sliit.realestate.util;


import com.sliit.realestate.models.Agent;

import java.util.ArrayList;
import java.util.List;
import java.util.*;

public class AgentBST {
    private Node root;
    // Auxiliary map for fast location-based lookups. Key: lowercase location, Value: List of Agents in that location.
    private Map<String, List<Agent>> agentsByLocation;

    public AgentBST() {
        this.root = null;
        this.agentsByLocation = new HashMap<>();
    }

    private class Node {
        Agent agent;
        Node left;
        Node right;
        public Node(Agent agent) {
            this.agent = agent;
            this.left = null;
            this.right = null;
        }
    }
    // Insert agent into BST based on agentId and update auxiliary map
    public void insert(Agent agent) {
        if (agent == null) {
            System.err.println("!!! AgentBST.insert: Attempted to insert a null agent.");
            return;
        }

        // Before inserting/updating in the BST, manage the auxiliary location map
        Agent existingAgentInBST = search(agent.getUserId());
        if (existingAgentInBST != null) {
            // If agent exists, remove the old instance from its current location list
            // This is crucial if the agent's location has changed
            String oldLocation = existingAgentInBST.getLocation();
            List<Agent> agentsInOldLocation = agentsByLocation.get(oldLocation.toLowerCase());
            if (agentsInOldLocation != null) {
                agentsInOldLocation.removeIf(a -> a.getUserId().equals(agent.getUserId()));
                if (agentsInOldLocation.isEmpty()) {
                    agentsByLocation.remove(oldLocation.toLowerCase());
                }
            }
        }

        // Perform the standard BST insert/update operation
        root = insertRec(root, agent);

        // Add the new/updated agent to its current location list in the auxiliary map

        agentsByLocation.computeIfAbsent(agent.getLocation().toLowerCase(), k -> new ArrayList<>()).add(agent);
    }

    private Node insertRec(Node root, Agent agent) {
        if (root == null) {
            return new Node(agent);
        }

        int cmp = agent.getUserId().compareTo(root.agent.getUserId());
        if (cmp < 0) {
            root.left = insertRec(root.left, agent);
        } else if (cmp > 0) {
            root.right = insertRec(root.right, agent);
        } else {
            // If userId is already present, update the agent data in the existing node
            root.agent = agent;
        }
        return root;
    }

    // Search agent by ID (efficient O(log N) average case)
    public Agent search(String agentId) {
        return searchRec(root, agentId);
    }

    private Agent searchRec(Node root, String agentId) {
        if (root == null) {
            return null;
        }

        int cmp = agentId.compareTo(root.agent.getUserId());
        if (cmp < 0) {
            return searchRec(root.left, agentId);
        } else if (cmp > 0) {
            return searchRec(root.right, agentId);
        } else {
            return root.agent;
        }
    }

    // Get all agents in order (O(N) traversal)
    public List<Agent> getAllAgents() {
        List<Agent> agents = new ArrayList<>();
        inOrderTraversal(root, agents);
        return agents;
    }

    private void inOrderTraversal(Node root, List<Agent> agents) {
        if (root != null) {
            inOrderTraversal(root.left, agents);
            agents.add(root.agent);
            inOrderTraversal(root.right, agents);
        }
    }

    // Find agents by location (efficient O(1) average case using auxiliary map)
    public List<Agent> findByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Agent> agents = agentsByLocation.get(location.trim().toLowerCase());
        return (agents != null) ? new ArrayList<>(agents) : new ArrayList<>(); // Return a copy to prevent external modification
    }


    public static List<Agent> sortByRating(List<Agent> agents) {
        if (agents == null || agents.isEmpty()) {
            return new ArrayList<>();
        }
        List<Agent> sortedAgents = new ArrayList<>(agents); // Create a mutable copy

        int n = sortedAgents.size();

        // Selection sort algorithm - sorting in descending order of ratings
        for (int i = 0; i < n - 1; i++) {
            int maxIndex = i;
            for (int j = i + 1; j < n; j++) {
                if (sortedAgents.get(j).getAverageRating() > sortedAgents.get(maxIndex).getAverageRating()) {
                    maxIndex = j;
                }
            }

            // Swap the agents
            Agent temp = sortedAgents.get(maxIndex);
            sortedAgents.set(maxIndex, sortedAgents.get(i));
            sortedAgents.set(i, temp);
        }

        return sortedAgents;
    }

    // New: Delete agent from BST and update auxiliary map (O(log N) average case for BST deletion)
    public void delete(String agentId) {
        if (agentId == null) {
            System.err.println("!!! AgentBST.delete: Attempted to delete with a null agentId.");
            return;
        }

        // Find the agent first to get its location for the auxiliary map update
        Agent agentToDelete = search(agentId);
        if (agentToDelete == null) {
            System.out.println(">>> AgentBST.delete: Agent " + agentId + " not found in BST for deletion.");
            return; // Agent not found
        }

        // Perform the standard BST delete operation
        root = deleteRec(root, agentId);

        // Remove from auxiliary map
        String locationToRemoveFrom = agentToDelete.getLocation().toLowerCase();
        List<Agent> agentsInLocation = agentsByLocation.get(locationToRemoveFrom);
        if (agentsInLocation != null) {
            agentsInLocation.removeIf(a -> a.getUserId().equals(agentId));
            if (agentsInLocation.isEmpty()) {
                agentsByLocation.remove(locationToRemoveFrom); // Remove list if empty
            }
        }
        System.out.println(">>> AgentBST.delete: Agent " + agentId + " deleted from BST and location map.");
    }

    private Node deleteRec(Node root, String agentId) {
        if (root == null) {
            return null;
        }

        int cmp = agentId.compareTo(root.agent.getUserId());
        if (cmp < 0) {
            root.left = deleteRec(root.left, agentId);
        } else if (cmp > 0) {
            root.right = deleteRec(root.right, agentId);
        } else {
            // Node to be deleted found
            if (root.left == null) {
                return root.right;
            } else if (root.right == null) {
                return root.left;
            }

            // Node with two children: Get the inorder successor (smallest in the right subtree)
            root.agent = minValue(root.right);
            // Delete the inorder successor
            root.right = deleteRec(root.right, root.agent.getUserId());
        }
        return root;
    }

    // Helper for deleteRec: finds the agent with the minimum value in a subtree
    private Agent minValue(Node root) {
        Agent minval = root.agent;
        while (root.left != null) {
            minval = root.left.agent;
            root = root.left;
        }
        return minval;
    }

    // New: Clear the BST and auxiliary map - useful for re-initialization
    public void clear() {
        root = null;
        agentsByLocation.clear();
        System.out.println(">>> AgentBST.clear: BST and auxiliary location map cleared.");
    }
}
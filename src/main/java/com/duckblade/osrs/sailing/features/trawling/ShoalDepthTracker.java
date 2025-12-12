package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Service component that tracks the current depth state of active shoals based entirely on chat messages
 */
@Slf4j
@Singleton
public class ShoalDepthTracker implements PluginLifecycleComponent {

    // Shoal object IDs - used to detect shoal presence for activation
    private static final Set<Integer> SHOAL_OBJECT_IDS = ImmutableSet.of(
        TrawlingData.ShoalObjectID.MARLIN,
        TrawlingData.ShoalObjectID.BLUEFIN,
        TrawlingData.ShoalObjectID.VIBRANT,
        TrawlingData.ShoalObjectID.HALIBUT,
        TrawlingData.ShoalObjectID.GLISTENING,
        TrawlingData.ShoalObjectID.YELLOWFIN,
        TrawlingData.ShoalObjectID.GIANT_KRILL,
        TrawlingData.ShoalObjectID.HADDOCK,
        TrawlingData.ShoalObjectID.SHIMMERING
    );

    private final Client client;
    private final ShoalTracker shoalTracker;

    // State fields
    private NetDepth currentDepth;

    @Inject
    public ShoalDepthTracker(Client client, ShoalTracker shoalTracker) {
        this.client = client;
        this.shoalTracker = shoalTracker;
        // Initialize with default values
        this.currentDepth = null;
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        // Service component - always enabled
        return true;
    }

    @Override
    public void startUp() {
        log.debug("ShoalDepthTracker started");
    }

    @Override
    public void shutDown() {
        log.debug("ShoalDepthTracker shut down");
        clearState();
    }

    // Public getter methods
    public NetDepth getCurrentDepth() {
        return currentDepth;
    }

    public boolean isShoalActive() {
        return shoalTracker.hasShoal();
    }

    /**
     * Legacy method for compatibility - three-depth areas are no longer tracked
     * @deprecated Use isShoalActive() instead
     */
    @Deprecated
    public boolean isThreeDepthArea() {
        return shoalTracker.hasShoal();
    }

    /**
     * Legacy method for compatibility - movement direction is no longer tracked
     * @deprecated Movement direction is determined from chat messages in real-time
     */
    @Deprecated
    public MovementDirection getNextMovementDirection() {
        return MovementDirection.UNKNOWN;
    }



    @Subscribe
    public void onChatMessage(ChatMessage e) {
        // Only process when shoal is active
        if (!shoalTracker.hasShoal()) {
            return;
        }

        // Only process game messages
        if (e.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String message = e.getMessage();
        if (message == null) {
            return;
        }

        String lowerMessage = message.toLowerCase();
        log.debug("=== CHAT MESSAGE ANALYSIS ===");
        log.debug("Message: '{}'", message);
        log.debug("Current depth: {}", currentDepth);

        // Only set shoal depth when we have definitive information
        if (lowerMessage.contains("correct depth for the nearby")) {
            // DEFINITIVE: Net is at correct depth - shoal matches current net depth
            // Note: Cannot determine net depth without NetDepthTracker - rely on movement messages instead
            log.debug("CONFIRMED: Net at correct depth - but cannot determine exact depth without NetDepthTracker");
        }
        else if (lowerMessage.contains("closer to the surface")) {
            // DEFINITIVE: Shoal moved shallower (only if we already know current depth)
            if (currentDepth != null) {
                NetDepth newDepth = moveDepthShallower(currentDepth);
                updateShoalDepth(newDepth, "CONFIRMED: Shoal moved closer to surface");
            } else {
                log.debug("Shoal moved closer to surface, but current depth unknown - cannot update");
            }
        }
        else if (lowerMessage.contains("shoal swims deeper into")) {
            // DEFINITIVE: Shoal moved deeper (only if we already know current depth)
            if (currentDepth != null) {
                NetDepth newDepth = moveDepthDeeper(currentDepth);
                updateShoalDepth(newDepth, "CONFIRMED: Shoal swims deeper");
            } else {
                log.debug("Shoal swims deeper, but current depth unknown - cannot update");
            }
        }
        else if (lowerMessage.contains("your net is not deep enough") || 
                 lowerMessage.contains("your net is too shallow")) {
            // INFORMATIONAL ONLY: Net needs to go deeper, but we don't know exact shoal depth
            log.debug("FEEDBACK: Net too shallow - shoal is deeper than current net position");
        }
        else if (lowerMessage.contains("your net is too deep")) {
            // INFORMATIONAL ONLY: Net needs to go shallower, but we don't know exact shoal depth
            log.debug("FEEDBACK: Net too deep - shoal is shallower than current net position");
        }

        log.debug("=== END CHAT MESSAGE ANALYSIS ===");
    }

    /**
     * Update the tracked shoal depth
     */
    private void updateShoalDepth(NetDepth newDepth, String reason) {
        if (newDepth != null && newDepth != currentDepth) {
            NetDepth oldDepth = currentDepth;
            currentDepth = newDepth;
            log.debug("*** SHOAL DEPTH UPDATED *** {} -> {} ({})", oldDepth, newDepth, reason);
        } else if (newDepth == currentDepth) {
            log.debug("*** SHOAL DEPTH CONFIRMED *** {} ({})", currentDepth, reason);
        }
    }





    /**
     * Move depth one level shallower
     */
    private NetDepth moveDepthShallower(NetDepth currentDepth) {
        if (currentDepth == null) {
            return null;
        }
        
        switch (currentDepth) {
            case DEEP:
                return NetDepth.MODERATE;
            case MODERATE:
                return NetDepth.SHALLOW;
            case SHALLOW:
                return NetDepth.SHALLOW; // Can't go shallower
        }
        
        return currentDepth;
    }

    /**
     * Move depth one level deeper
     */
    private NetDepth moveDepthDeeper(NetDepth currentDepth) {
        if (currentDepth == null) {
            return null;
        }
        
        switch (currentDepth) {
            case SHALLOW:
                return NetDepth.MODERATE;
            case MODERATE:
                return NetDepth.DEEP;
            case DEEP:
                return NetDepth.DEEP; // Can't go deeper
        }
        
        return currentDepth;
    }

    /**
     * Clear all tracking state
     */
    private void clearState() {
        this.currentDepth = null;
        log.debug("ShoalDepthTracker state cleared");
    }
    
    void setCurrentDepthForTesting(NetDepth depth) {
        this.currentDepth = depth;
    }
}
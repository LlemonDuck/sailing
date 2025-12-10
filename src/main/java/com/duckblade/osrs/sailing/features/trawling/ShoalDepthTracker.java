package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Service component that tracks the current depth state of active shoals
 */
@Slf4j
@Singleton
public class ShoalDepthTracker implements PluginLifecycleComponent {

    // WorldEntity config ID for moving shoals
    private static final int SHOAL_WORLD_ENTITY_CONFIG_ID = 4;
    
    // Shoal object IDs - used to detect any shoal presence
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

    // State fields
    private NetDepth currentDepth;
    private boolean isThreeDepthArea;
    private MovementDirection nextMovementDirection;
    private WorldPoint activeShoalLocation;

    @Inject
    public ShoalDepthTracker(Client client) {
        this.client = client;
        // Initialize with default values
        this.currentDepth = null;
        this.isThreeDepthArea = false;
        this.nextMovementDirection = MovementDirection.UNKNOWN;
        this.activeShoalLocation = null;
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

    public boolean isThreeDepthArea() {
        return isThreeDepthArea;
    }

    public MovementDirection getNextMovementDirection() {
        return nextMovementDirection;
    }

    // Called by NetDepthTimer when depth changes
    public void notifyDepthChange(NetDepth newDepth) {
        this.currentDepth = newDepth;
        // Clear movement direction after depth transitions
        this.nextMovementDirection = MovementDirection.UNKNOWN;
        log.debug("Depth changed to: {}, movement direction cleared", newDepth);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        log.debug("GameObject spawned: ID={}, location={}", objectId, obj.getWorldLocation());
        
        if (SHOAL_OBJECT_IDS.contains(objectId)) {
            log.info("*** SHOAL GAMEOBJECT DETECTED *** ID={}, location={}", objectId, obj.getWorldLocation());
            // Don't initialize state yet - wait for WorldEntity spawn to get proper top-level coordinates
        }
    }

    @Subscribe
    public void onWorldEntitySpawned(WorldEntitySpawned e) {
        WorldEntity entity = e.getWorldEntity();
        
        log.info("*** ShoalDepthTracker - WorldEntity spawned: config={}, configId={} ***", 
                 entity.getConfig(), 
                 entity.getConfig() != null ? entity.getConfig().getId() : "null");
        
        // Only track shoal WorldEntity
        if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
            LocalPoint localPos = entity.getCameraFocus();
            log.info("*** SHOAL WORLDENTITY DETECTED IN SHOALDEPTHTRACKER *** configId={}, localPos={}", 
                    entity.getConfig().getId(), localPos);
            
            if (localPos != null) {
                WorldPoint worldPos = WorldPoint.fromLocal(client, localPos);
                log.info("Converted to WorldPoint: {}", worldPos);
                if (worldPos != null) {
                    initializeShoalState(worldPos);
                }
            }
            log.info("Shoal WorldEntity spawned, initialized depth tracking");
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        if (SHOAL_OBJECT_IDS.contains(objectId)) {
            // Shoal left world view - clear state
            log.debug("Shoal despawned (left world view): ID={}", objectId);
            clearState();
        }
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        // Track timing-based transitions
        // This is a simplified implementation - in practice, NetDepthTimer handles the complex timing
        // and calls notifyDepthChange when transitions occur
        // This method is here to support the interface and future enhancements
    }

    @Subscribe
    public void onChatMessage(ChatMessage e) {
        String message = e.getMessage();
        log.info("=== CHAT MESSAGE DEBUG ===");
        log.info("Type: {}", e.getType());
        log.info("Message: '{}'", message);
        log.info("Current state - threeDepthArea: {}, currentDepth: {}, nextMovementDirection: {}", 
                 isThreeDepthArea, currentDepth, nextMovementDirection);
        log.info("Active shoal location: {}", activeShoalLocation);
        
        // Only process messages when in three-depth areas
        if (!isThreeDepthArea || currentDepth == null) {
            log.info("IGNORING: Not in three-depth area ({}) or no current depth ({})", isThreeDepthArea, currentDepth);
            return;
        }

        // Only process game messages
        if (e.getType() != ChatMessageType.GAMEMESSAGE) {
            log.info("IGNORING: Not a game message (type: {})", e.getType());
            return;
        }

        if (message == null) {
            log.info("IGNORING: Null message");
            return;
        }

        String lowerMessage = message.toLowerCase();
        log.info("Checking message for depth keywords: '{}'", lowerMessage);

        // Parse messages for "deeper" keywords and set nextMovementDirection to DEEPER
        if (lowerMessage.contains("deeper")) {
            MovementDirection oldDirection = this.nextMovementDirection;
            this.nextMovementDirection = MovementDirection.DEEPER;
            log.info("*** DEEPER MOVEMENT DETECTED *** - changed from {} to {}", oldDirection, this.nextMovementDirection);
            log.info("Full message: '{}'", message);
        }
        // Parse messages for "shallower" keywords and set nextMovementDirection to SHALLOWER
        else if (lowerMessage.contains("shallower")) {
            MovementDirection oldDirection = this.nextMovementDirection;
            this.nextMovementDirection = MovementDirection.SHALLOWER;
            log.info("*** SHALLOWER MOVEMENT DETECTED *** - changed from {} to {}", oldDirection, this.nextMovementDirection);
            log.info("Full message: '{}'", message);
        } else {
            log.info("No depth keywords found in message");
        }
        log.info("=== END CHAT MESSAGE DEBUG ===");
    }

    private void initializeShoalState(WorldPoint location) {
        this.activeShoalLocation = location;
        
        // Determine fishing area from shoal location
        int stopDuration = TrawlingData.FishingAreas.getStopDurationForLocation(location);
        
        if (stopDuration <= 0) {
            log.warn("Shoal spawned at unknown location: {} (not in any defined fishing area)", location);
            clearState();
            return;
        }

        // Determine if this is a three-depth area (Bluefin/Marlin areas)
        this.isThreeDepthArea = (stopDuration == TrawlingData.ShoalStopDuration.BLUEFIN || 
                                stopDuration == TrawlingData.ShoalStopDuration.MARLIN);
        
        // Initialize currentDepth based on area's depth pattern
        if (stopDuration == TrawlingData.ShoalStopDuration.MARLIN) {
            // Marlin areas: start at MODERATE, transition to DEEP
            this.currentDepth = NetDepth.MODERATE;
        } else {
            // All other areas (Bluefin, Halibut, Yellowfin): start at SHALLOW, transition to MODERATE
            this.currentDepth = NetDepth.SHALLOW;
        }
        
        // Reset movement direction
        this.nextMovementDirection = MovementDirection.UNKNOWN;
        
        log.info("Initialized shoal depth tracking at {}: depth={}, threeDepthArea={}, stopDuration={}", 
                 location, currentDepth, isThreeDepthArea, stopDuration);
    }

    private void clearState() {
        this.currentDepth = null;
        this.isThreeDepthArea = false;
        this.nextMovementDirection = MovementDirection.UNKNOWN;
        this.activeShoalLocation = null;
        log.debug("ShoalDepthTracker state cleared");
    }

    // Package-private methods for testing
    void initializeShoalStateForTesting(WorldPoint location) {
        initializeShoalState(location);
    }
    
    void setMovementDirectionForTesting(MovementDirection direction) {
        this.nextMovementDirection = direction;
    }
}
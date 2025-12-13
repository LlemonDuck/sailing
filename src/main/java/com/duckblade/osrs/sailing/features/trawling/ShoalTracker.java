package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.duckblade.osrs.sailing.model.ShoalDepth;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.gameval.AnimationID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.api.gameval.NpcID.SAILING_SHOAL_RIPPLES;

/**
 * Centralized tracker for shoal WorldEntity and GameObject instances.
 * Provides a single source of truth for shoal state across all trawling components.
 */
@Slf4j
@Singleton
public class ShoalTracker implements PluginLifecycleComponent {

    // WorldEntity config ID for moving shoals
    private static final int SHOAL_WORLD_ENTITY_CONFIG_ID = 4;
    private static final int SHOAL_DEPTH_SHALLOW = AnimationID.DEEP_SEA_TRAWLING_SHOAL_SHALLOW;
    private static final int SHOAL_DEPTH_MODERATE = AnimationID.DEEP_SEA_TRAWLING_SHOAL_MID;
    private static final int SHOAL_DEPTH_DEEP = AnimationID.DEEP_SEA_TRAWLING_SHOAL_DEEP;
    
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

    private NPC currentShoalNpc;

    /**
     * -- GETTER --
     *  Get the current shoal WorldEntity (for movement tracking)
     */
    // Tracked state
    @Getter
    private WorldEntity currentShoalEntity = null;
    private final Set<GameObject> shoalObjects = new HashSet<>();
    /**
     * -- GETTER --
     *  Get the current shoal location
     */
    @Getter
    private WorldPoint currentLocation = null;
    /**
     * -- GETTER --
     *  Get the shoal duration for the current location
     */
    @Getter
    private int shoalDuration = 0;
    
    // Movement tracking
    private WorldPoint previousLocation = null;
    private boolean wasMoving = false;
    /**
     * -- GETTER --
     *  Get the number of ticks the shoal has been stationary
     */
    @Getter
    private int stationaryTicks = 0;
    
    // Depth tracking
    /**
     * -- GETTER --
     *  Get the current shoal depth based on NPC animation
     */
    @Getter
    private ShoalDepth currentShoalDepth = ShoalDepth.UNKNOWN;

    @Inject
    public ShoalTracker(Client client) {
        this.client = client;
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        // Service component - always enabled
        return true;
    }

    @Override
    public void startUp() {
        log.debug("ShoalTracker started");
    }

    @Override
    public void shutDown() {
        log.debug("ShoalTracker shut down");
        clearState();
    }

    // Public API methods

    /**
     * Get all current shoal GameObjects (for rendering/highlighting)
     */
    public Set<GameObject> getShoalObjects() {
        return new HashSet<>(shoalObjects); // Return copy to prevent external modification
    }

    /**
     * Check if any shoal is currently active
     */
    public boolean hasShoal() {
        return currentShoalEntity != null || !shoalObjects.isEmpty();
    }

    /**
     * Check if the shoal WorldEntity is valid and trackable
     */
    public boolean isShoalEntityValid() {
        return currentShoalEntity != null && currentShoalEntity.getCameraFocus() != null;
    }

    /**
     * Get animation ID from any Renderable object (supports multiple types)
     * @param renderable The renderable object to check
     * @return Animation ID, or -1 if no animation or unsupported type
     */
    public int getAnimationIdFromRenderable(Renderable renderable) {
        if (renderable == null) {
            return -1;
        }

        // DynamicObject (GameObjects with animations)
        if (renderable instanceof DynamicObject) {
            DynamicObject dynamicObject = (DynamicObject) renderable;
            if (dynamicObject.getAnimation() != null) {
                return dynamicObject.getAnimation().getId();
            }
        }
        // Actor types (NPCs, Players) - they have direct getAnimation() method
        else if (renderable instanceof Actor) {
            Actor actor = (Actor) renderable;
            return actor.getAnimation(); // Returns int directly, -1 if no animation
        }
        // Note: Other Renderable types like Model, GraphicsObject may exist but are less common
        // Add more types here as needed

        return -1;
    }

    /**
     * Determine shoal depth based on animation ID
     * @param animationId The animation ID to check
     * @return The corresponding ShoalDepth
     */
    public ShoalDepth getShoalDepthFromAnimation(int animationId) {
        if (animationId == SHOAL_DEPTH_SHALLOW) {
            return ShoalDepth.SHALLOW;
        } else if (animationId == SHOAL_DEPTH_MODERATE) {
            return ShoalDepth.MODERATE;
        } else if (animationId == SHOAL_DEPTH_DEEP) {
            return ShoalDepth.DEEP;
        } else {
            return ShoalDepth.UNKNOWN;
        }
    }

    /**
     * Update the current shoal depth based on the NPC animation
     */
    private void updateShoalDepth() {
        if (currentShoalNpc != null) {
            int animationId = currentShoalNpc.getAnimation();
            ShoalDepth newDepth = getShoalDepthFromAnimation(animationId);
            
            if (newDepth != currentShoalDepth) {
                ShoalDepth previousDepth = currentShoalDepth;
                currentShoalDepth = newDepth;
                log.debug("Shoal depth changed from {} to {} (animation: {})", 
                    previousDepth, currentShoalDepth, animationId);
            }
        } else {
            if (currentShoalDepth != ShoalDepth.UNKNOWN) {
                currentShoalDepth = ShoalDepth.UNKNOWN;
                log.debug("Shoal depth reset to UNKNOWN (no NPC)");
            }
        }
    }

    /**
     * Check if the shoal depth is currently known
     * @return true if depth is not UNKNOWN
     */
    public boolean isShoalDepthKnown() {
        return currentShoalDepth != ShoalDepth.UNKNOWN;
    }

    /**
     * Update the current location from the WorldEntity
     */
    public void updateLocation() {
        if (currentShoalEntity != null) {
            LocalPoint localPos = currentShoalEntity.getCameraFocus();
            if (localPos != null) {
                WorldPoint newLocation = WorldPoint.fromLocal(client, localPos);
                if (!newLocation.equals(currentLocation)) {
                    previousLocation = currentLocation;
                    currentLocation = newLocation;
                    // Update duration when location changes
                    shoalDuration = TrawlingData.FishingAreas.getStopDurationForLocation(currentLocation);
                }
            }
        }

        // Track movement state
        trackMovement();
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        if (!hasShoal()) {
            // Reset movement tracking when no shoal
            resetMovementTracking();
            return;
        }
        
        // Update shoal depth based on NPC animation
        updateShoalDepth();
        
        // updateLocation() is called by other components, so we don't need to call it here
        // Just ensure movement tracking happens each tick
        trackMovement();
    }
    
    /**
     * Track shoal movement and count stationary ticks
     */
    private void trackMovement() {
        if (currentLocation == null) {
            return;
        }
        
        // Check if shoal moved this tick
        boolean isMoving = previousLocation != null && !currentLocation.equals(previousLocation);
        
        if (isMoving) {
            wasMoving = true;
            stationaryTicks = 0;
        } else {
            if (wasMoving) {
                wasMoving = false;
                stationaryTicks = 1; // Start counting from 1
            } else {
                // Shoal continues to be stationary
                stationaryTicks++;
            }
        }
    }
    
    /**
     * Reset movement tracking state
     */
    private void resetMovementTracking() {
        previousLocation = null;
        wasMoving = false;
        stationaryTicks = 0;
    }

    // Event handlers

    @Subscribe
    public void onNpcSpawned(NpcSpawned e) {
        NPC npc = e.getNpc();
        if (npc.getId() == SAILING_SHOAL_RIPPLES) {
            currentShoalNpc = npc;
            log.debug("Shoal NPC spawned (ID={})", npc.getId());
            // Update depth immediately when NPC spawns
            updateShoalDepth();
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned e) {
        NPC npc = e.getNpc();
        if (npc == currentShoalNpc) {
            log.debug("Shoal NPC despawned (ID={})", npc.getId());
            currentShoalNpc = null;
            // Reset depth when NPC despawns
            updateShoalDepth();
        }
    }

    @Subscribe
    public void onWorldEntitySpawned(WorldEntitySpawned e) {
        WorldEntity entity = e.getWorldEntity();
        
        // Only track shoal WorldEntity
        if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
            boolean hadExistingShoal = currentShoalEntity != null;
            currentShoalEntity = entity;
            
            // Update location and duration
            updateLocation();
            
            if (!hadExistingShoal) {
                log.debug("Shoal WorldEntity spawned at {}", currentLocation);
            }
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        if (SHOAL_OBJECT_IDS.contains(objectId)) {
            shoalObjects.add(obj);
            log.debug("Shoal GameObject spawned (ID={})", objectId);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        
        if (shoalObjects.remove(obj)) {
            log.debug("Shoal GameObject despawned (ID={})", obj.getId());
        }
    }

    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded e) {
        // Only clear shoals when we're not actively sailing
        if (!e.getWorldView().isTopLevel()) {
            return;
        }
        
        // Check if player and worldview are valid before calling isSailing
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getWorldView() == null) {
            log.debug("Top-level world view unloaded (player/worldview null), clearing shoal state");
            clearState();
            return;
        }
        
        if (!SailingUtil.isSailing(client)) {
            log.debug("Top-level world view unloaded while not sailing, clearing shoal state");
            clearState();
        }
    }

    /**
     * Try to find the shoal WorldEntity if we lost track of it
     */
    public void findShoalEntity() {
        if (client.getTopLevelWorldView() != null) {
            for (WorldEntity entity : client.getTopLevelWorldView().worldEntities()) {
                if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
                    currentShoalEntity = entity;
                    updateLocation();
                    log.debug("Found shoal WorldEntity in scene");
                    return;
                }
            }
        }
        
        // If we can't find it, clear the entity reference
        if (currentShoalEntity != null) {
            log.debug("Shoal WorldEntity no longer exists");
            currentShoalEntity = null;
            currentLocation = null;
            shoalDuration = 0;
        }
    }

    /**
     * Clear all tracking state
     */
    private void clearState() {
        currentShoalEntity = null;
        shoalObjects.clear();
        currentLocation = null;
        shoalDuration = 0;
        currentShoalNpc = null;
        currentShoalDepth = ShoalDepth.UNKNOWN;
        resetMovementTracking();
        log.debug("ShoalTracker state cleared");
    }
}
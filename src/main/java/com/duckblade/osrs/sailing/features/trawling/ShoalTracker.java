package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

/**
 * Centralized tracker for shoal WorldEntity and GameObject instances.
 * Provides a single source of truth for shoal state across all trawling components.
 */
@Slf4j
@Singleton
public class ShoalTracker implements PluginLifecycleComponent {

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

    // Tracked state
    private WorldEntity currentShoalEntity = null;
    private final Set<GameObject> shoalObjects = new HashSet<>();
    private WorldPoint currentLocation = null;
    private int shoalDuration = 0;

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
     * Get the current shoal WorldEntity (for movement tracking)
     */
    public WorldEntity getCurrentShoalEntity() {
        return currentShoalEntity;
    }

    /**
     * Get all current shoal GameObjects (for rendering/highlighting)
     */
    public Set<GameObject> getShoalObjects() {
        return new HashSet<>(shoalObjects); // Return copy to prevent external modification
    }

    /**
     * Get the current shoal location
     */
    public WorldPoint getCurrentLocation() {
        return currentLocation;
    }

    /**
     * Get the shoal duration for the current location
     */
    public int getShoalDuration() {
        return shoalDuration;
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
     * Update the current location from the WorldEntity
     */
    public void updateLocation() {
        if (currentShoalEntity != null) {
            LocalPoint localPos = currentShoalEntity.getCameraFocus();
            if (localPos != null) {
                WorldPoint newLocation = WorldPoint.fromLocal(client, localPos);
                if (newLocation != null && !newLocation.equals(currentLocation)) {
                    currentLocation = newLocation;
                    // Update duration when location changes
                    shoalDuration = TrawlingData.FishingAreas.getStopDurationForLocation(currentLocation);
                    log.debug("Shoal location updated to {}, duration: {} ticks", currentLocation, shoalDuration);
                }
            }
        }
    }

    // Event handlers

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
                log.debug("New shoal WorldEntity spawned at {}, duration: {} ticks", currentLocation, shoalDuration);
            } else {
                log.debug("Shoal WorldEntity updated (type change) at {}", currentLocation);
            }
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        if (SHOAL_OBJECT_IDS.contains(objectId)) {
            shoalObjects.add(obj);
            log.debug("Shoal GameObject spawned (ID={}) at {} (total objects: {})", 
                     objectId, obj.getLocalLocation(), shoalObjects.size());
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        
        if (shoalObjects.remove(obj)) {
            log.debug("Shoal GameObject despawned (ID={}) (remaining objects: {})", 
                     obj.getId(), shoalObjects.size());
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
                    log.debug("Found shoal WorldEntity in scene at {}", currentLocation);
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
        log.debug("ShoalTracker state cleared");
    }
}
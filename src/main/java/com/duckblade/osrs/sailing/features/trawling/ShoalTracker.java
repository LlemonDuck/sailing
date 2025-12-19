package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.duckblade.osrs.sailing.model.ShoalDepth;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Renderable;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.Notifier;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.gameval.AnimationID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.runelite.api.gameval.NpcID.SAILING_SHOAL_RIPPLES;

/**
 * Centralized tracker for shoal WorldEntity and GameObject instances.
 * Provides a single source of truth for shoal state across all trawling components.
 * Shoals are a WorldEntity (moving object), GameObject, and an NPC (renderable). All
 * three are required for detection of movement, spawn/despawn, and shoal depth.
 */
@Slf4j
@Singleton
public class ShoalTracker implements PluginLifecycleComponent {

    // WorldEntity config ID for moving shoals
    private static final int SHOAL_WORLD_ENTITY_CONFIG_ID = 4;
    private static final int SHOAL_DEPTH_SHALLOW = AnimationID.DEEP_SEA_TRAWLING_SHOAL_SHALLOW;
    private static final int SHOAL_DEPTH_MODERATE = AnimationID.DEEP_SEA_TRAWLING_SHOAL_MID;
    private static final int SHOAL_DEPTH_DEEP = AnimationID.DEEP_SEA_TRAWLING_SHOAL_DEEP;
    
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
	private final Notifier notifier;
	private final SailingConfig config;
	private final BoatTracker boatTracker;

    /**
     * -- GETTER --
     *  Get the current shoal WorldEntity (for movement tracking)
     */
    // Tracked state
    @Getter
    private WorldEntity currentShoalEntity = null;
    private final Map<Integer, GameObject> shoalObjects = new HashMap<>();
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

    /**
     * -- GETTER --
     *  Get the current shoal NPC (for rendering/highlighting)
     */
    @Getter
    private NPC currentShoalNpc;

    /**
     * Creates a new ShoalTracker with the specified client.
     *
     * @param client the RuneLite client instance
     */
    
    @Inject
    public ShoalTracker(Client client, Notifier notifier, SailingConfig config, BoatTracker boatTracker) {
        this.client = client;
		this.notifier = notifier;
		this.config = config;
		this.boatTracker = boatTracker;
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
     * Gets all current shoal GameObjects for rendering/highlighting.
     *
     * @return a copy of the current shoal objects set
     */
    public Set<GameObject> getShoalObjects() {
        return new HashSet<>(shoalObjects.values());
    }

    /**
     * Checks if any shoal is currently active.
     *
     * @return true if a shoal entity or objects are present, false otherwise
     */
    public boolean hasShoal() {
        return currentShoalEntity != null || !shoalObjects.isEmpty();
    }

    /**
     * Checks if the shoal WorldEntity is valid and trackable.
     *
     * @return true if the shoal entity exists and has a valid camera focus, false otherwise
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

    private void updateShoalDepth() {
        if (currentShoalNpc != null) {
            updateDepthFromNpc();
        } else {
            resetDepthToUnknown();
        }
    }

    private void updateDepthFromNpc() {
        int animationId = currentShoalNpc.getAnimation();
        ShoalDepth newDepth = getShoalDepthFromAnimation(animationId);
        
        if (newDepth != currentShoalDepth) {
			checkDepthNotification();
            currentShoalDepth = newDepth;
        }
    }

	private void checkDepthNotification()
	{
		Boat boat = boatTracker.getBoat();
		if (boat == null || boat.getNetTiers().isEmpty()) {
			return;
		}
		notifier.notify(config.notifyDepthChange(), "Shoal depth changed");
	}

	private void checkMovementNotification()
	{
		Boat boat = boatTracker.getBoat();
		if (boat == null) {
			return;
		}
		notifier.notify(config.notifyShoalMove(), "Shoal started moving");
	}

	private void resetDepthToUnknown() {
        if (currentShoalDepth != ShoalDepth.UNKNOWN) {
            currentShoalDepth = ShoalDepth.UNKNOWN;
        }
    }

    /**
     * Checks if the shoal depth is currently known.
     *
     * @return true if depth is not UNKNOWN, false otherwise
     */
    public boolean isShoalDepthKnown() {
        return currentShoalDepth != ShoalDepth.UNKNOWN;
    }

    /**
     * Updates the shoal location and tracks movement.
     */
    public void updateLocation() {
        updateLocationFromEntity();
    }

    private void updateLocationFromEntity() {
        if (currentShoalEntity == null) {
            return;
        }

        LocalPoint localPos = currentShoalEntity.getCameraFocus();
        if (localPos != null) {
            WorldPoint newLocation = WorldPoint.fromLocal(client, localPos);
            updateLocationIfChanged(newLocation);
        }
    }

    private void updateLocationIfChanged(WorldPoint newLocation) {
        if (newLocation == null) {
            return;
        }
        
        if (!newLocation.equals(currentLocation)) {
            previousLocation = currentLocation;
            currentLocation = newLocation;
            updateShoalDuration();
        }
    }

    private void updateShoalDuration() {
        shoalDuration = TrawlingData.FishingAreas.getStopDurationForLocation(currentLocation);
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        if (!hasShoal()) {
            resetMovementTracking();
            return;
        }
        
        updateLocation();
        updateShoalDepth();
        trackMovement();
    }
    
    private void trackMovement() {
        if (currentLocation == null) {
            return;
        }
        
        boolean isMoving = hasShoalMoved();
        
        if (isMoving) {
            handleShoalMoving();
        } else {
            handleShoalStationary();
        }
    }

    private boolean hasShoalMoved() {
        return previousLocation != null && !currentLocation.equals(previousLocation);
    }

    private void handleShoalMoving() {
        if (!wasMoving) {
            checkMovementNotification();
        }
        wasMoving = true;
        stationaryTicks = 0;
    }

    private void handleShoalStationary() {
        if (wasMoving) {
            startStationaryCount();
        } else {
            incrementStationaryCount();
        }
        // Reset previousLocation when stationary so we can detect movement again
        previousLocation = currentLocation;
    }

    private void startStationaryCount() {
        wasMoving = false;
        stationaryTicks = 1;
    }

    private void incrementStationaryCount() {
        stationaryTicks++;
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
        if (isShoalNpc(npc)) {
            handleShoalNpcSpawned(npc);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned e) {
        NPC npc = e.getNpc();
        if (npc == currentShoalNpc) {
            handleShoalNpcDespawned(npc);
        }
    }

    private boolean isShoalNpc(NPC npc) {
        return npc.getId() == SAILING_SHOAL_RIPPLES;
    }

    private void handleShoalNpcSpawned(NPC npc) {
        currentShoalNpc = npc;
        updateShoalDepth();
    }

    private void handleShoalNpcDespawned(NPC npc) {
        currentShoalNpc = null;
        updateShoalDepth();
    }

    @Subscribe
    public void onWorldEntitySpawned(WorldEntitySpawned e) {
        WorldEntity entity = e.getWorldEntity();
        
        if (isShoalWorldEntity(entity)) {
            handleShoalWorldEntitySpawned(entity);
        }
    }

    private boolean isShoalWorldEntity(WorldEntity entity) {
        return entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID;
    }

    private void handleShoalWorldEntitySpawned(WorldEntity entity) {
        boolean hadExistingShoal = currentShoalEntity != null;
        currentShoalEntity = entity;
        
        updateLocation();
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        
        if (isShoalGameObject(obj)) {
            handleShoalGameObjectSpawned(obj);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        
        GameObject removedObject = shoalObjects.remove(obj.getId());
        if (removedObject != null && removedObject == obj) {
            handleShoalGameObjectDespawned(obj);
        }
    }

    private boolean isShoalGameObject(GameObject obj) {
        return SHOAL_OBJECT_IDS.contains(obj.getId());
    }

    private void handleShoalGameObjectSpawned(GameObject obj) {
        int objectId = obj.getId();
        shoalObjects.put(objectId, obj);
    }

    private void handleShoalGameObjectDespawned(GameObject obj) {
        // GameObject removed from map in onGameObjectDespawned
    }



    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded e) {
        if (!e.getWorldView().isTopLevel()) {
            return;
        }
        
        if (shouldClearStateOnWorldViewUnload()) {
            clearState();
        }
    }

    private boolean shouldClearStateOnWorldViewUnload() {
        if (isPlayerOrWorldViewInvalid()) {
            log.debug("Top-level world view unloaded (player/worldview null), clearing shoal state");
            return true;
        }
        
        if (!SailingUtil.isSailing(client)) {
            log.debug("Top-level world view unloaded while not sailing, clearing shoal state");
            return true;
        }
        
        return false;
    }

    private boolean isPlayerOrWorldViewInvalid() {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getWorldView() == null;
    }

    /**
     * Attempts to find and set the current shoal WorldEntity.
     */
    public void findShoalEntity() {
        WorldEntity foundEntity = searchForShoalEntity();
        
        if (foundEntity != null) {
            handleFoundShoalEntity(foundEntity);
        } else {
            handleMissingShoalEntity();
        }
    }

    private WorldEntity searchForShoalEntity() {
        if (client.getTopLevelWorldView() == null) {
            return null;
        }
        
        for (WorldEntity entity : client.getTopLevelWorldView().worldEntities()) {
            if (isShoalWorldEntity(entity)) {
                return entity;
            }
        }
        
        return null;
    }

    private void handleFoundShoalEntity(WorldEntity entity) {
        currentShoalEntity = entity;
        updateLocation();
        log.debug("Found shoal WorldEntity in scene");
    }

    private void handleMissingShoalEntity() {
        if (currentShoalEntity != null) {
            log.debug("Shoal WorldEntity no longer exists");
            clearShoalEntityState();
        }
    }

    private void clearShoalEntityState() {
        currentShoalEntity = null;
        currentLocation = null;
        shoalDuration = 0;
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
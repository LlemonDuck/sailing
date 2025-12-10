package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
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
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Singleton
public class NetDepthTimer extends Overlay
        implements PluginLifecycleComponent {

    // WorldEntity config ID for moving shoals
    private static final int SHOAL_WORLD_ENTITY_CONFIG_ID = 4;
    
    // Number of ticks at same position to consider shoal "stopped"
    private static final int STOPPED_THRESHOLD_TICKS = 2;
    
    // Shoal object IDs - used to detect any shoal presence
    private static final Set<Integer> SHOAL_OBJECT_IDS = ImmutableSet.of(
        TrawlingData.ShoalObjectID.MARLIN,
        TrawlingData.ShoalObjectID.BLUEFIN,
        TrawlingData.ShoalObjectID.VIBRANT,
        TrawlingData.ShoalObjectID.HALIBUT,
        TrawlingData.ShoalObjectID.GLISTENING,
        TrawlingData.ShoalObjectID.YELLOWFIN
    );


    
    // Depth transitions based on stop duration
    private static final Map<Integer, ShoalTiming> DURATION_TO_TIMING = new HashMap<>();
    
    static {
        // Marlin areas: 50 ticks, Moderate -> Deep
        DURATION_TO_TIMING.put(TrawlingData.ShoalStopDuration.MARLIN, 
            new ShoalTiming(TrawlingData.ShoalStopDuration.MARLIN, NetDepth.MODERATE, NetDepth.DEEP));
        
        // Bluefin areas: 66 ticks, Shallow -> Moderate
        DURATION_TO_TIMING.put(TrawlingData.ShoalStopDuration.BLUEFIN, 
            new ShoalTiming(TrawlingData.ShoalStopDuration.BLUEFIN, NetDepth.SHALLOW, NetDepth.MODERATE));
        
        // Halibut areas: 80 ticks, Shallow -> Moderate
        DURATION_TO_TIMING.put(TrawlingData.ShoalStopDuration.HALIBUT, 
            new ShoalTiming(TrawlingData.ShoalStopDuration.HALIBUT, NetDepth.SHALLOW, NetDepth.MODERATE));
        
        // Yellowfin areas: 100 ticks, Shallow -> Moderate
        DURATION_TO_TIMING.put(TrawlingData.ShoalStopDuration.YELLOWFIN, 
            new ShoalTiming(TrawlingData.ShoalStopDuration.YELLOWFIN, NetDepth.SHALLOW, NetDepth.MODERATE));
    }
    


    private final Client client;
    private final SailingConfig config;
    private final BoatTracker boatTracker;
    private final ShoalDepthTracker shoalDepthTracker;

    // Track WorldEntity (moving shoal) for position monitoring
    private WorldEntity movingShoal = null;
    private WorldPoint lastShoalPosition = null;
    private int ticksAtSamePosition = 0;
    private boolean hasSeenShoalStop = false;
    
    // Track the active shoal timer
    private ShoalTracker activeTracker = null;
    


    @Inject
    public NetDepthTimer(Client client, SailingConfig config, BoatTracker boatTracker, ShoalDepthTracker shoalDepthTracker) {
        this.client = client;
        this.config = config;
        this.boatTracker = boatTracker;
        this.shoalDepthTracker = shoalDepthTracker;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(1000.0f);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        return config.trawlingShowNetDepthTimer();
    }

    @Override
    public void startUp() {
        log.debug("NetDepthTimer started");
    }

    @Override
    public void shutDown() {
        log.debug("NetDepthTimer shut down");
        movingShoal = null;
        lastShoalPosition = null;
        ticksAtSamePosition = 0;
        hasSeenShoalStop = false;
        activeTracker = null;
    }

    /**
     * Get current timer information for display in overlay
     * @return TimerInfo object with current state, or null if no shoal is being tracked
     */
    public TimerInfo getTimerInfo() {
        if (activeTracker == null) {
            return null;
        }
        TimerInfo info = activeTracker.getTimerInfo();
        return info;
    }

    @Subscribe
    public void onWorldEntitySpawned(WorldEntitySpawned e) {
        WorldEntity entity = e.getWorldEntity();
        
        // Log all WorldEntity spawns
        if (entity.getConfig() != null) {
            log.debug("WorldEntity spawned - Config ID: {}", entity.getConfig().getId());
        }
        
        // Only track shoal WorldEntity
        if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
            movingShoal = entity;
            lastShoalPosition = null;
            ticksAtSamePosition = 0;
            log.info("Shoal WorldEntity spawned, tracking movement");
            
            // Create tracker if we don't have one yet, using WorldEntity's position
            if (activeTracker == null) {
                LocalPoint localPos = entity.getCameraFocus();
                if (localPos != null) {
                    WorldPoint worldPos = WorldPoint.fromLocal(client, localPos);
                    int stopDuration = TrawlingData.FishingAreas.getStopDurationForLocation(worldPos);
                    
                    log.info("Shoal WorldEntity at location: {}, StopDuration: {}", worldPos, stopDuration);
                    
                    if (stopDuration > 0) {
                        activeTracker = new ShoalTracker(stopDuration, worldPos);
                        log.info("Created ShoalTracker at location {}: stop duration = {} ticks", 
                                 worldPos, stopDuration);
                    } else {
                        log.warn("Shoal spawned at unknown location: {} (not in any defined fishing area)", worldPos);
                    }
                }
            }
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        if (SHOAL_OBJECT_IDS.contains(objectId)) {
            log.debug("Shoal GameObject detected (ID={}), waiting for WorldEntity to get proper coordinates", objectId);
            // Don't create tracker yet - wait for WorldEntity spawn to get proper top-level coordinates
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        if (SHOAL_OBJECT_IDS.contains(objectId)) {
            // Shoal left world view - reset everything
            log.debug("Shoal despawned (left world view): ID={}", objectId);
            activeTracker = null;
            movingShoal = null;
            lastShoalPosition = null;
            ticksAtSamePosition = 0;
            hasSeenShoalStop = false;
        }
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        // If we don't have a moving shoal but have an active tracker, try to find it
        if (movingShoal == null && activeTracker != null) {
            if (client.getTopLevelWorldView() != null) {
                for (WorldEntity entity : client.getTopLevelWorldView().worldEntities()) {
                    if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
                        movingShoal = entity;
                        lastShoalPosition = null;
                        ticksAtSamePosition = 0;
                        log.debug("Found shoal WorldEntity in scene, tracking movement");
                        break;
                    }
                }
            }
        }
        
        // Track WorldEntity movement to detect when it stops
        if (movingShoal != null && activeTracker != null) {
            net.runelite.api.coords.LocalPoint localPos = movingShoal.getCameraFocus();
            if (localPos != null) {
                WorldPoint currentPos = WorldPoint.fromLocal(client, localPos);
                if (currentPos != null) {
                    if (currentPos.equals(lastShoalPosition)) {
                        ticksAtSamePosition++;
                        log.debug("Shoal at same position: {} ticks", ticksAtSamePosition);
                        
                        if (ticksAtSamePosition == STOPPED_THRESHOLD_TICKS && !hasSeenShoalStop) {
                            // First time seeing shoal stop
                            hasSeenShoalStop = true;
                            log.info("Shoal stopped at {} (first stop observed, waiting for movement)", currentPos);
                        } else if (ticksAtSamePosition == STOPPED_THRESHOLD_TICKS && hasSeenShoalStop) {
                            // Shoal stopped again after moving - restart timer
                            activeTracker.restart();
                            log.info("Shoal stopped at {}, timer restarted", currentPos);
                        }
                    } else {
                        if (lastShoalPosition != null) {
                            log.info("Shoal moved from {} to {}", lastShoalPosition, currentPos);
                        }
                        lastShoalPosition = currentPos;
                        ticksAtSamePosition = 0;
                    }
                }
            }
        }
        
        // Update timer
        if (activeTracker != null) {
            activeTracker.tick();
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Timer overlay display is handled by other components if needed
        // This component now focuses only on timing logic and depth change notifications
        return null;
    }



    /**
     * Data class for exposing timer information to overlay
     */
    public static class TimerInfo {
        private final boolean active;
        private final boolean waiting;
        private final int ticksUntilDepthChange;

        public TimerInfo(boolean active, boolean waiting, int ticksUntilDepthChange) {
            this.active = active;
            this.waiting = waiting;
            this.ticksUntilDepthChange = ticksUntilDepthChange;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isWaiting() {
            return waiting;
        }

        public int getTicksUntilDepthChange() {
            return ticksUntilDepthChange;
        }
    }

    // Data class for shoal timing information
    private static class ShoalTiming {
        final int totalDuration; // Total ticks at each waypoint
        final NetDepth startDepth;
        final NetDepth endDepth;

        ShoalTiming(int totalDuration, NetDepth startDepth, NetDepth endDepth) {
            this.totalDuration = totalDuration;
            this.startDepth = startDepth;
            this.endDepth = endDepth;
        }

        int getDepthChangeTime() {
            return totalDuration / 2;
        }
    }

    // Tracker for shoal timer state - now based on location instead of shoal type
    private class ShoalTracker {
        final int stopDuration;
        final WorldPoint location;
        int ticksAtWaypoint;
        boolean timerActive;

        ShoalTracker(int stopDuration, WorldPoint location) {
            this.stopDuration = stopDuration;
            this.location = location;
            this.ticksAtWaypoint = 0;
            this.timerActive = false; // Don't start timer until we've seen a complete cycle
        }

        void restart() {
            this.ticksAtWaypoint = 0;
            this.timerActive = true; // Activate timer when restarting (after stop→move→stop)
            log.info("Shoal at {} timer restarted and activated (duration: {} ticks)", 
                     location, stopDuration);
        }

        void tick() {
            if (!timerActive) {
                return; // Don't tick until timer is active
            }
            
            ticksAtWaypoint++;
            if (ticksAtWaypoint == 1) {
                log.debug("Shoal at {} timer TICK 1 - timer is now running", location);
            }
            if (ticksAtWaypoint % 10 == 0) {
                NetDepth requiredDepth = getCurrentRequiredDepth();
                log.debug("Shoal at {} at tick {}: required depth = {}", 
                         location, ticksAtWaypoint, requiredDepth);
            }
            
            // Check if we've reached the depth change point - deactivate timer after first depth change
            ShoalTiming timing = DURATION_TO_TIMING.get(stopDuration);
            if (timing != null) {
                int depthChangeTime = timing.getDepthChangeTime();
                
                if (ticksAtWaypoint == depthChangeTime) {
                    // Depth change has occurred - notify ShoalDepthTracker
                    NetDepth newDepth = timing.endDepth;
                    shoalDepthTracker.notifyDepthChange(newDepth);
                    log.debug("Shoal at {} depth change occurred at tick {}, notified ShoalDepthTracker of new depth: {}", 
                             location, ticksAtWaypoint, newDepth);
                }
                
                if (ticksAtWaypoint >= depthChangeTime) {
                    // Deactivate timer until shoal moves and stops again
                    timerActive = false;
                    log.debug("Shoal at {} timer deactivated after depth change", location);
                }
            }
        }

        NetDepth getCurrentRequiredDepth() {
            if (!timerActive) {
                return null; // Don't provide depth until timer is active
            }
            
            ShoalTiming timing = DURATION_TO_TIMING.get(stopDuration);
            if (timing == null) {
                return null;
            }

            int depthChangeTime = timing.getDepthChangeTime();
            
            if (ticksAtWaypoint < depthChangeTime) {
                return timing.startDepth;
            } else {
                return timing.endDepth;
            }
        }

        TimerInfo getTimerInfo() {
            ShoalTiming timing = DURATION_TO_TIMING.get(stopDuration);
            if (timing == null) {
                return new TimerInfo(false, false, 0);
            }

            // Check if shoal is currently moving (not stopped)
            boolean shoalIsMoving = ticksAtSamePosition < STOPPED_THRESHOLD_TICKS;
            
            if (!timerActive) {
                // Waiting for shoal to stop (either first time or after moving again)
                return new TimerInfo(false, shoalIsMoving, 0);
            }

            int depthChangeTime = timing.getDepthChangeTime();
            
            // Only show timer until first depth change
            int ticksUntilChange = depthChangeTime - ticksAtWaypoint;

            return new TimerInfo(true, false, ticksUntilChange);
        }
    }
}

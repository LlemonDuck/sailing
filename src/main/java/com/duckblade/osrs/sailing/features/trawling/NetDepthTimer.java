package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Slf4j
@Singleton
public class NetDepthTimer extends Overlay implements PluginLifecycleComponent {

    // Number of ticks shoal must be moving before we consider it "was moving"
    private static final int MOVEMENT_THRESHOLD_TICKS = 5;
    
    // Number of ticks at same position to consider shoal "stopped"
    private static final int STOPPED_THRESHOLD_TICKS = 2;

    private final Client client;
    private final SailingConfig config;
    private final ShoalTracker shoalTracker;

    // Movement tracking
    private WorldPoint lastShoalPosition = null;
    private int ticksAtSamePosition = 0;
    private int ticksMoving = 0;
    private boolean hasBeenMoving = false;
    
    // Timer state
    private int timerTicks = 0;
    private boolean timerActive = false;

    @Inject
    public NetDepthTimer(Client client, SailingConfig config, ShoalTracker shoalTracker) {
        this.client = client;
        this.config = config;
        this.shoalTracker = shoalTracker;
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
        resetState();
    }

    /**
     * Get current timer information for display in overlay
     */
    public TimerInfo getTimerInfo() {
        if (!shoalTracker.hasShoal()) {
            return null;
        }
        
        // Disable timer in ONE_DEPTH areas (Giant Krill areas)
        WorldPoint playerLocation = SailingUtil.getTopLevelWorldPoint(client);
        FishingAreaType areaType = TrawlingData.FishingAreas.getFishingAreaType(playerLocation);
        if (areaType == FishingAreaType.ONE_DEPTH) {
            return null; // Timer disabled in krill areas
        }
        
        boolean shoalIsMoving = ticksAtSamePosition < STOPPED_THRESHOLD_TICKS;
        
        if (!timerActive) {
            if (shoalIsMoving) {
                return new TimerInfo(false, true, 0); // Waiting for shoal to stop
            } else {
                return new TimerInfo(false, false, 0); // Calibrating
            }
        }
        
        // Timer counts down to depth change (half duration)
        int shoalDuration = shoalTracker.getShoalDuration();
        int depthChangeTime = shoalDuration / 2;
        int ticksUntilDepthChange = depthChangeTime - timerTicks;
        return new TimerInfo(true, false, Math.max(0, ticksUntilDepthChange));
    }



    @Subscribe
    public void onGameTick(GameTick e) {
        if (!shoalTracker.hasShoal()) {
            // No shoal - reset state
            if (timerActive || hasBeenMoving) {
                log.debug("No shoal detected - resetting timer state");
                resetState();
            }
            return;
        }
        
        // Disable timer processing in ONE_DEPTH areas (Giant Krill areas)
        WorldPoint playerLocation = SailingUtil.getTopLevelWorldPoint(client);
        FishingAreaType areaType = TrawlingData.FishingAreas.getFishingAreaType(playerLocation);
        if (areaType == FishingAreaType.ONE_DEPTH) {
            // Reset timer state if we're in a krill area
            if (timerActive || hasBeenMoving) {
                resetState();
            }
            return;
        }
        
        // Check if WorldEntity is valid, try to find it if not
        if (!shoalTracker.isShoalEntityValid()) {
            shoalTracker.findShoalEntity();
            if (!shoalTracker.isShoalEntityValid()) {
                // WorldEntity is truly gone - reset state
                log.debug("WorldEntity no longer exists - resetting timer state");
                resetState();
                return;
            }
        }
        
        // Update location in tracker and track movement
        shoalTracker.updateLocation();
        WorldPoint currentPos = shoalTracker.getCurrentLocation();
        if (currentPos != null) {
            trackMovement(currentPos);
        }
        
        // Update timer if active
        if (timerActive) {
            timerTicks++;
            int shoalDuration = shoalTracker.getShoalDuration();
            int depthChangeTime = shoalDuration / 2;
            if (timerTicks >= depthChangeTime) {
                // Depth change reached - stop timer
                timerActive = false;
                log.debug("Depth change reached at {} ticks (half of {} total duration)", timerTicks, shoalDuration);
            }
        }
    }

    private void trackMovement(WorldPoint currentPos) {
        if (currentPos.equals(lastShoalPosition)) {
            // Shoal is stationary
            ticksAtSamePosition++;
            ticksMoving = 0;
            
            // Check if shoal just stopped after being in motion
            if (ticksAtSamePosition == STOPPED_THRESHOLD_TICKS && hasBeenMoving) {
                startTimer();
            }
        } else {
            // Shoal is moving
            lastShoalPosition = currentPos;
            ticksAtSamePosition = 0;
            ticksMoving++;
            
            // Mark as having been moving if it's moved for enough ticks
            if (ticksMoving >= MOVEMENT_THRESHOLD_TICKS) {
                hasBeenMoving = true;
            }
            
            // Stop timer if shoal starts moving again
            if (timerActive) {
                timerActive = false;
                log.debug("Timer stopped - shoal started moving");
            }
        }
    }

    private void startTimer() {
        int shoalDuration = shoalTracker.getShoalDuration();
        if (shoalDuration > 0) {
            timerActive = true;
            timerTicks = 0;
            log.debug("Timer started - shoal stopped after movement (duration: {} ticks)", shoalDuration);
        }
    }

    private void resetState() {
        lastShoalPosition = null;
        ticksAtSamePosition = 0;
        ticksMoving = 0;
        hasBeenMoving = false;
        timerActive = false;
        timerTicks = 0;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Timer display is handled by TrawlingOverlay
        return null;
    }

    /**
     * Data class for exposing timer information to overlay
     */
    public static class TimerInfo {
        @Getter
        private final boolean active;
        @Getter
        private final boolean waiting;
        private final int ticksRemaining;

        public TimerInfo(boolean active, boolean waiting, int ticksRemaining) {
            this.active = active;
            this.waiting = waiting;
            this.ticksRemaining = ticksRemaining;
        }

        public int getTicksUntilDepthChange() {
            return ticksRemaining;
        }
    }
}
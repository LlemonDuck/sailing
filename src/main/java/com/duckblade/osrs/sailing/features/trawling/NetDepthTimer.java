package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class NetDepthTimer extends Overlay
        implements PluginLifecycleComponent {

    // WorldEntity config ID for moving shoals
    private static final int SHOAL_WORLD_ENTITY_CONFIG_ID = 4;
    
    // Number of ticks at same position to consider shoal "stopped"
    private static final int STOPPED_THRESHOLD_TICKS = 2;
    
    // Shoal object IDs
    private static final int SHOAL_MARLIN = 59740;
    private static final int SHOAL_BLUEFIN = 59738;
    private static final int SHOAL_VIBRANT = 59742;
    private static final int SHOAL_HALIBUT = 59737;
    private static final int SHOAL_GLISTENING = 59741;
    private static final int SHOAL_YELLOWFIN = 59736;

    // Grace period in ticks before depth change is required
    private static final int GRACE_PERIOD_TICKS = 6;
    
    // Shoal timing data (in ticks)
    private static final Map<Integer, ShoalTiming> SHOAL_TIMINGS = new HashMap<>();
    
    static {
        SHOAL_TIMINGS.put(SHOAL_MARLIN, new ShoalTiming(54, NetDepth.MODERATE, NetDepth.DEEP));
        SHOAL_TIMINGS.put(SHOAL_BLUEFIN, new ShoalTiming(66, NetDepth.SHALLOW, NetDepth.MODERATE));
        SHOAL_TIMINGS.put(SHOAL_VIBRANT, new ShoalTiming(66, NetDepth.SHALLOW, NetDepth.MODERATE));
        SHOAL_TIMINGS.put(SHOAL_HALIBUT, new ShoalTiming(80, NetDepth.SHALLOW, NetDepth.MODERATE));
        SHOAL_TIMINGS.put(SHOAL_GLISTENING, new ShoalTiming(80, NetDepth.SHALLOW, NetDepth.MODERATE));
        SHOAL_TIMINGS.put(SHOAL_YELLOWFIN, new ShoalTiming(100, NetDepth.SHALLOW, NetDepth.MODERATE));
    }

    // Widget indices for fishing net controls
    private static final int STARBOARD_DOWN = 97;
    private static final int STARBOARD_UP = 108;
    private static final int PORT_DOWN = 132;
    private static final int PORT_UP = 143;
    
    // Widget indices for net depth indicators
    private static final int STARBOARD_DEPTH_WIDGET_INDEX = 96;
    private static final int PORT_DEPTH_WIDGET_INDEX = 131;
    
    // Sprite IDs for each depth level
    private static final int SPRITE_SHALLOW = 7081;
    private static final int SPRITE_MODERATE = 7082;
    private static final int SPRITE_DEEP = 7083;

    private final Client client;
    private final SailingConfig config;
    private final BoatTracker boatTracker;
    private final ShoalPathTracker shoalPathTracker;

    // Track WorldEntity (moving shoal) for position monitoring
    private WorldEntity movingShoal = null;
    private WorldPoint lastShoalPosition = null;
    private int ticksAtSamePosition = 0;
    private boolean hasSeenShoalStop = false; // Track if we've seen the shoal stop at least once
    
    // Track the active shoal timer
    private ShoalTracker activeTracker = null;
    
    // Track last known waypoint count to detect new stops
    private int lastWaypointCount = 0;

    @Inject
    public NetDepthTimer(Client client, SailingConfig config, BoatTracker boatTracker, ShoalPathTracker shoalPathTracker) {
        this.client = client;
        this.config = config;
        this.boatTracker = boatTracker;
        this.shoalPathTracker = shoalPathTracker;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(1000.0f);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        return config.trawlingHighlightNetButtons();
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
        return activeTracker.getTimerInfo();
    }

    @Subscribe
    public void onWorldEntitySpawned(WorldEntitySpawned e) {
        WorldEntity entity = e.getWorldEntity();
        
        // Log all WorldEntity spawns to debug
        if (entity.getConfig() != null) {
            log.debug("WorldEntity spawned - Config ID: {}", entity.getConfig().getId());
        }
        
        // Only track shoal WorldEntity
        if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
            movingShoal = entity;
            lastShoalPosition = null;
            ticksAtSamePosition = 0;
            log.debug("Shoal WorldEntity spawned, tracking movement");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        log.debug("GameObject spawned: ID={}, isShoal={}", objectId, SHOAL_TIMINGS.containsKey(objectId));
        
        if (SHOAL_TIMINGS.containsKey(objectId)) {
            // Store the shoal type when we first see it
            if (activeTracker == null || activeTracker.objectId != objectId) {
                activeTracker = new ShoalTracker(objectId);
                log.debug("Tracking shoal type: ID={}, movingShoal={}, hasSeenStop={}, activeTracker created", 
                         objectId, movingShoal != null, hasSeenShoalStop);
                log.debug("Overlay should now show calibration status");
            }
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        if (SHOAL_TIMINGS.containsKey(objectId)) {
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
            // Try to find the WorldEntity in the scene
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
                        if (ticksAtSamePosition == STOPPED_THRESHOLD_TICKS && !hasSeenShoalStop) {
                            // First time seeing shoal stop
                            hasSeenShoalStop = true;
                            log.debug("Shoal stopped at {} (first stop observed, waiting for movement)", currentPos);
                        } else if (ticksAtSamePosition == STOPPED_THRESHOLD_TICKS && hasSeenShoalStop) {
                            // Shoal stopped again after moving - restart timer
                            activeTracker.restart();
                            log.debug("Shoal stopped at {}, timer restarted", currentPos);
                        }
                    } else {
                        if (lastShoalPosition != null) {
                            log.debug("Shoal moved from {} to {}", lastShoalPosition, currentPos);
                            // Shoal started moving - this will trigger "waiting for stop" in overlay
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
        if (!config.trawlingHighlightNetButtons()) {
            return null;
        }

        Boat boat = boatTracker.getBoat();
        if (boat == null || boat.getNetTiers().isEmpty()) {
            return null;
        }

        Widget widgetSailingRows = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
        if (widgetSailingRows == null) {
            return null;
        }

        // Check if we have an active tracker and highlight buttons if needed
        if (activeTracker != null) {
            NetDepth requiredDepth = activeTracker.getCurrentRequiredDepth();
            if (requiredDepth != null) {
                highlightButtonsForDepth(graphics, widgetSailingRows, requiredDepth);
            }
        }

        return null;
    }

    private void highlightButtonsForDepth(Graphics2D graphics, Widget parent, NetDepth requiredDepth) {
        Color highlightColor = config.trawlingHighlightColour();

        // Check starboard net - only highlight if opacity is 0 (player can interact)
        Widget starboardDepthWidget = parent.getChild(STARBOARD_DEPTH_WIDGET_INDEX);
        if (starboardDepthWidget != null && starboardDepthWidget.getOpacity() == 0) {
            NetDepth currentDepth = getNetDepth(parent, STARBOARD_DEPTH_WIDGET_INDEX);
            if (currentDepth != null && currentDepth != requiredDepth) {
                highlightNetButton(graphics, parent, currentDepth, requiredDepth, 
                                  STARBOARD_UP, STARBOARD_DOWN, highlightColor);
            }
        }

        // Check port net - only highlight if opacity is 0 (player can interact)
        Widget portDepthWidget = parent.getChild(PORT_DEPTH_WIDGET_INDEX);
        if (portDepthWidget != null && portDepthWidget.getOpacity() == 0) {
            NetDepth currentDepth = getNetDepth(parent, PORT_DEPTH_WIDGET_INDEX);
            if (currentDepth != null && currentDepth != requiredDepth) {
                highlightNetButton(graphics, parent, currentDepth, requiredDepth,
                                  PORT_UP, PORT_DOWN, highlightColor);
            }
        }
    }

    private void highlightNetButton(Graphics2D graphics, Widget parent, NetDepth current, 
                                    NetDepth required, int upIndex, int downIndex, Color color) {
        // Determine which button to highlight
        int buttonIndex;
        if (required.ordinal() < current.ordinal()) {
            // Need to go shallower (up)
            buttonIndex = upIndex;
        } else {
            // Need to go deeper (down)
            buttonIndex = downIndex;
        }

        Widget button = getNetWidget(parent, buttonIndex);
        if (button != null && !button.isHidden()) {
            Rectangle bounds = button.getBounds();
            if (bounds.width > 0 && bounds.height > 0) {
                // Check if button is actually visible in the viewport (not scrolled out of view)
                if (isWidgetInViewport(button, parent)) {
                    graphics.setColor(color);
                    graphics.setStroke(new BasicStroke(3));
                    graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
                }
            }
        }
    }

    private boolean isWidgetInViewport(Widget widget, Widget scrollContainer) {
        if (widget == null || scrollContainer == null) {
            return false;
        }
        
        Rectangle widgetBounds = widget.getBounds();
        
        // Find the actual scroll viewport by looking for the parent with scroll properties
        Widget scrollViewport = scrollContainer;
        while (scrollViewport != null && scrollViewport.getScrollHeight() == 0) {
            scrollViewport = scrollViewport.getParent();
        }
        
        if (scrollViewport == null) {
            // No scroll container found, use the original container
            Rectangle containerBounds = scrollContainer.getBounds();
            return containerBounds.contains(widgetBounds);
        }
        
        // Get the visible viewport bounds (accounting for scroll position)
        Rectangle viewportBounds = scrollViewport.getBounds();
        int scrollY = scrollViewport.getScrollY();
        
        // Adjust the viewport to account for scroll position
        Rectangle visibleArea = new Rectangle(
            viewportBounds.x,
            viewportBounds.y,
            viewportBounds.width,
            viewportBounds.height
        );
        
        // Check if the widget is fully visible within the scrolled viewport
        return visibleArea.contains(widgetBounds);
    }

    private NetDepth getNetDepth(Widget parent, int widgetIndex) {
        Widget depthWidget = parent.getChild(widgetIndex);
        if (depthWidget == null) {
            return null;
        }

        int spriteId = depthWidget.getSpriteId();
        
        if (spriteId == SPRITE_SHALLOW) {
            return NetDepth.SHALLOW;
        } else if (spriteId == SPRITE_MODERATE) {
            return NetDepth.MODERATE;
        } else if (spriteId == SPRITE_DEEP) {
            return NetDepth.DEEP;
        }

        return null;
    }

    private Widget getNetWidget(Widget parent, int index) {
        Widget parentWidget = parent.getChild(index);
        if (parentWidget == null) {
            return null;
        }

        Rectangle bounds = parentWidget.getBounds();

        // Parent widgets have invalid bounds, get their children
        if (bounds.x == -1 && bounds.y == -1) {
            Widget[] children = parentWidget.getChildren();
            if (children != null && children.length > 0) {
                for (Widget child : children) {
                    if (child != null) {
                        Rectangle childBounds = child.getBounds();
                        if (childBounds.x != -1 && childBounds.y != -1) {
                            return child;
                        }
                    }
                }
            }
        } else {
            return parentWidget;
        }

        return null;
    }

    // Enum for net depths
    private enum NetDepth {
        SHALLOW,
        MODERATE,
        DEEP
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

    // Tracker for shoal timer state
    private class ShoalTracker {
        final int objectId;
        int ticksAtWaypoint;
        boolean timerActive;

        ShoalTracker(int objectId) {
            this.objectId = objectId;
            this.ticksAtWaypoint = 0;
            this.timerActive = false; // Don't start timer until we've seen a complete cycle
        }

        void restart() {
            this.ticksAtWaypoint = 0;
            this.timerActive = true; // Activate timer when restarting (after stop→move→stop)
            log.debug("Shoal {} timer restarted and activated", objectId);
        }

        void tick() {
            if (!timerActive) {
                return; // Don't tick until timer is active
            }
            
            ticksAtWaypoint++;
            if (ticksAtWaypoint == 1) {
                log.debug("Shoal {} timer TICK 1 - timer is now running", objectId);
            }
            if (ticksAtWaypoint % 10 == 0) {
                NetDepth requiredDepth = getCurrentRequiredDepth();
                log.debug("Shoal {} at tick {}: required depth = {}", objectId, ticksAtWaypoint, requiredDepth);
            }
            
            // Check if we've reached the depth change point - deactivate timer after first depth change
            ShoalTiming timing = SHOAL_TIMINGS.get(objectId);
            if (timing != null) {
                int depthChangeTime = timing.getDepthChangeTime();
                int actualChangeTime = depthChangeTime + GRACE_PERIOD_TICKS;
                
                if (ticksAtWaypoint >= actualChangeTime) {
                    // Depth change has occurred, deactivate timer until shoal moves and stops again
                    timerActive = false;
                    log.debug("Shoal {} depth change occurred at tick {}, timer deactivated", objectId, ticksAtWaypoint);
                }
            }
        }

        NetDepth getCurrentRequiredDepth() {
            if (!timerActive) {
                return null; // Don't provide depth until timer is active
            }
            
            ShoalTiming timing = SHOAL_TIMINGS.get(objectId);
            if (timing == null) {
                return null;
            }

            int depthChangeTime = timing.getDepthChangeTime();
            
            // Account for grace period: change happens at midpoint + grace period
            int actualChangeTime = depthChangeTime + GRACE_PERIOD_TICKS;
            
            if (ticksAtWaypoint < actualChangeTime) {
                return timing.startDepth;
            } else {
                return timing.endDepth;
            }
        }

        TimerInfo getTimerInfo() {
            ShoalTiming timing = SHOAL_TIMINGS.get(objectId);
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
            int actualChangeTime = depthChangeTime + GRACE_PERIOD_TICKS;
            
            // Only show timer until first depth change
            int ticksUntilChange = actualChangeTime - ticksAtWaypoint;

            return new TimerInfo(true, false, ticksUntilChange);
        }
    }
}

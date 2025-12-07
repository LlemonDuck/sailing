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
    private static final int SHOAL_BLUEFIN = 59737;
    private static final int SHOAL_HALIBUT = 59739;
    private static final int SHOAL_YELLOWFIN = 59736;

    // Shoal timing data (in ticks)
    private static final Map<Integer, ShoalTiming> SHOAL_TIMINGS = new HashMap<>();
    
    static {
        SHOAL_TIMINGS.put(SHOAL_MARLIN, new ShoalTiming(54, NetDepth.MODERATE, NetDepth.DEEP));
        SHOAL_TIMINGS.put(SHOAL_BLUEFIN, new ShoalTiming(70, NetDepth.SHALLOW, NetDepth.MODERATE));
        SHOAL_TIMINGS.put(SHOAL_HALIBUT, new ShoalTiming(80, NetDepth.SHALLOW, NetDepth.MODERATE));
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

    // Track WorldEntity (moving shoal) for position monitoring
    private WorldEntity movingShoal = null;
    private WorldPoint lastShoalPosition = null;
    private int ticksAtSamePosition = 0;
    private boolean hasSeenShoalStop = false; // Track if we've seen the shoal stop at least once
    
    // Track the active shoal timer
    private ShoalTracker activeTracker = null;

    @Inject
    public NetDepthTimer(Client client, SailingConfig config, BoatTracker boatTracker) {
        this.client = client;
        this.config = config;
        this.boatTracker = boatTracker;
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

    @Subscribe
    public void onWorldEntitySpawned(WorldEntitySpawned e) {
        WorldEntity entity = e.getWorldEntity();
        
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
        
        if (SHOAL_TIMINGS.containsKey(objectId)) {
            // Store the shoal type when we first see it
            if (activeTracker == null || activeTracker.objectId != objectId) {
                activeTracker = new ShoalTracker(objectId);
                log.debug("Tracking shoal type: ID={}", objectId);
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
                            // If we've seen a stop and now it's moving, next stop will start the timer
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
            } else {
                // Timer not active yet - show calibration message
                renderCalibrationMessage(graphics, widgetSailingRows);
            }
        }

        return null;
    }

    private void renderCalibrationMessage(Graphics2D graphics, Widget parent) {
        // Render "Calibrating Nets..." text on the sailing interface
        Rectangle bounds = parent.getBounds();
        if (bounds.width > 0 && bounds.height > 0) {
            String message = "Calibrating Nets...";
            FontMetrics fm = graphics.getFontMetrics();
            int textWidth = fm.stringWidth(message);
            int textHeight = fm.getHeight();
            
            // Center the text in the widget area
            int x = bounds.x + (bounds.width - textWidth) / 2;
            int y = bounds.y + (bounds.height + textHeight) / 2 - fm.getDescent();
            
            // Draw shadow for better visibility
            graphics.setColor(Color.BLACK);
            graphics.drawString(message, x + 1, y + 1);
            
            // Draw main text
            graphics.setColor(Color.YELLOW);
            graphics.drawString(message, x, y);
        }
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
                graphics.setColor(color);
                graphics.setStroke(new BasicStroke(3));
                graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }
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
            
            if (ticksAtWaypoint < depthChangeTime) {
                return timing.startDepth;
            } else {
                return timing.endDepth;
            }
        }
    }
}

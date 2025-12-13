package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.model.ShoalDepth;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

/**
 * Overlay component that highlights net depth adjustment buttons when shoal depth is known.
 * Highlights buttons to guide players toward matching their net depth to the current shoal depth.
 */
@Slf4j
@Singleton
public class NetDepthButtonHighlighter extends Overlay
        implements PluginLifecycleComponent {

    // Widget indices for fishing net controls
    private static final int STARBOARD_DOWN = 97;
    private static final int STARBOARD_UP = 108;
    private static final int PORT_DOWN = 132;
    private static final int PORT_UP = 143;
    
    // Widget indices for net depth indicators
    private static final int STARBOARD_DEPTH_WIDGET_INDEX = 96;
    private static final int PORT_DEPTH_WIDGET_INDEX = 131;

    private final ShoalTracker shoalTracker;
    private final NetDepthTracker netDepthTracker;
    private final BoatTracker boatTracker;
    private final Client client;
    private final SailingConfig config;

    // Cached highlighting state to avoid recalculating every frame
    private boolean shouldHighlightPort = false;
    private boolean shouldHighlightStarboard = false;
    private ShoalDepth cachedRequiredDepth = null;
    private ShoalDepth cachedPortDepth = null;
    private ShoalDepth cachedStarboardDepth = null;
    private boolean highlightingStateValid = false;

    @Inject
    public NetDepthButtonHighlighter(ShoalTracker shoalTracker,
                                   NetDepthTracker netDepthTracker,
                                   BoatTracker boatTracker, 
                                   Client client, 
                                   SailingConfig config) {
        this.shoalTracker = shoalTracker;
        this.netDepthTracker = netDepthTracker;
        this.boatTracker = boatTracker;
        this.client = client;
        this.config = config;
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
        log.debug("NetDepthButtonHighlighter started");
        invalidateHighlightingState();
    }

    @Override
    public void shutDown() {
        log.debug("NetDepthButtonHighlighter shut down");
        invalidateHighlightingState();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Check basic prerequisites
        if (!canHighlightButtons()) {
            // If prerequisites changed, invalidate cache
            if (highlightingStateValid) {
                log.debug("Prerequisites no longer met, invalidating highlighting state");
                invalidateHighlightingState();
            }
            return null;
        }

        // Update highlighting state if needed (only when events occur)
        if (!highlightingStateValid) {
            updateHighlightingState();
        }

        // Only render if there's something to highlight
        if (!shouldHighlightPort && !shouldHighlightStarboard) {
            return null;
        }

        Widget widgetSailingRows = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
        if (widgetSailingRows == null) {
            return null;
        }

        // Render cached highlighting decisions
        renderCachedHighlights(graphics, widgetSailingRows);

        return null;
    }

    /**
     * Check if button highlighting is possible (basic prerequisites)
     */
    private boolean canHighlightButtons() {
        // Check if we have a boat with nets
        Boat boat = boatTracker.getBoat();
        if (boat == null || boat.getNetTiers().isEmpty()) {
            return false;
        }

        // Check if shoal is active and we know its depth
        if (!shoalTracker.hasShoal()) {
            return false;
        }
        
        if (!shoalTracker.isShoalDepthKnown()) {
            return false;
        }

        return true;
    }

    /**
     * Invalidate the cached highlighting state, forcing recalculation on next render
     */
    private void invalidateHighlightingState() {
        highlightingStateValid = false;
        shouldHighlightPort = false;
        shouldHighlightStarboard = false;
        cachedRequiredDepth = null;
        cachedPortDepth = null;
        cachedStarboardDepth = null;
    }

    /**
     * Update the cached highlighting state based on current shoal and net depths
     */
    private void updateHighlightingState() {
        log.debug("Updating highlighting state");
        
        // Force refresh net depth cache to ensure we have latest values
        netDepthTracker.refreshCache();
        
        // Get current depths
        cachedRequiredDepth = determineRequiredDepth();
        cachedPortDepth = netDepthTracker.getPortNetDepth();
        cachedStarboardDepth = netDepthTracker.getStarboardNetDepth();
        
        log.debug("Current depths - Required: {}, Port: {}, Starboard: {}", 
                 cachedRequiredDepth, cachedPortDepth, cachedStarboardDepth);
        
        // Calculate highlighting decisions
        shouldHighlightPort = cachedRequiredDepth != null && 
                             cachedRequiredDepth != ShoalDepth.UNKNOWN &&
                             cachedPortDepth != null && 
                             cachedPortDepth != cachedRequiredDepth;
                             
        shouldHighlightStarboard = cachedRequiredDepth != null && 
                                  cachedRequiredDepth != ShoalDepth.UNKNOWN &&
                                  cachedStarboardDepth != null && 
                                  cachedStarboardDepth != cachedRequiredDepth;
        
        highlightingStateValid = true;
        
        log.debug("Highlighting decisions - Port: {} ({}), Starboard: {} ({})",
                 shouldHighlightPort, cachedPortDepth != cachedRequiredDepth ? "mismatch" : "match",
                 shouldHighlightStarboard, cachedStarboardDepth != cachedRequiredDepth ? "mismatch" : "match");
    }

    /**
     * Render highlights based on cached state
     */
    private void renderCachedHighlights(Graphics2D graphics, Widget parent) {
        Color highlightColor = config.trawlingShoalHighlightColour();

        // Highlight starboard net if needed
        if (shouldHighlightStarboard) {
            Widget starboardDepthWidget = parent.getChild(STARBOARD_DEPTH_WIDGET_INDEX);
            if (starboardDepthWidget != null && starboardDepthWidget.getOpacity() == 0) {
                highlightNetButton(graphics, parent, cachedStarboardDepth, cachedRequiredDepth, 
                                  STARBOARD_UP, STARBOARD_DOWN, highlightColor);
            }
        }

        // Highlight port net if needed
        if (shouldHighlightPort) {
            Widget portDepthWidget = parent.getChild(PORT_DEPTH_WIDGET_INDEX);
            if (portDepthWidget != null && portDepthWidget.getOpacity() == 0) {
                highlightNetButton(graphics, parent, cachedPortDepth, cachedRequiredDepth,
                                  PORT_UP, PORT_DOWN, highlightColor);
            }
        }
    }

    /**
     * Listen for any state changes that might affect highlighting
     */
    @Subscribe
    public void onGameTick(GameTick e) {
        // Always check if we need to invalidate the cache
        if (highlightingStateValid) {
            // Check if shoal depth changed
            ShoalDepth currentRequiredDepth = determineRequiredDepth();
            if (currentRequiredDepth != cachedRequiredDepth) {
                log.debug("Shoal depth changed from {} to {}, invalidating highlighting state", 
                         cachedRequiredDepth, currentRequiredDepth);
                invalidateHighlightingState();
                return;
            }
            
            // Check if net depths changed (fallback in case varbit events are missed)
            ShoalDepth currentPortDepth = netDepthTracker.getPortNetDepth();
            ShoalDepth currentStarboardDepth = netDepthTracker.getStarboardNetDepth();
            
            if (currentPortDepth != cachedPortDepth || currentStarboardDepth != cachedStarboardDepth) {
                log.debug("Net depths changed - Port: {} -> {}, Starboard: {} -> {}, invalidating highlighting state",
                         cachedPortDepth, currentPortDepth, cachedStarboardDepth, currentStarboardDepth);
                invalidateHighlightingState();
                return;
            }
        }
    }

    /**
     * Listen for net depth changes (varbit changes)
     */
    @Subscribe
    public void onVarbitChanged(VarbitChanged e) {
        // Check if this is a net depth varbit change
        int varbitId = e.getVarbitId();
        if (varbitId == 19206 || varbitId == 19208) { // Net depth varbits
            log.debug("Net depth varbit changed ({}), invalidating highlighting state", varbitId);
            invalidateHighlightingState();
        }
    }

    /**
     * Determine which depth the nets should be set to
     */
    private ShoalDepth determineRequiredDepth() {
        if (!shoalTracker.isShoalDepthKnown()) {
            return null;
        }

        // Nets should match the current shoal depth
        return shoalTracker.getCurrentShoalDepth();
    }



    /**
     * Highlight the appropriate button for a specific net
     */
    private void highlightNetButton(Graphics2D graphics, Widget parent, ShoalDepth current, 
                                    ShoalDepth required, int upIndex, int downIndex, Color color) {
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



    /**
     * Safely access widget children
     */
    private Widget getNetWidget(Widget parent, int index) {
        Widget parentWidget = parent.getChild(index);
        if (parentWidget == null) {
            return null;
        }

        Rectangle bounds = parentWidget.getBounds();

        // Parent widgets have invalid bounds, get their children
        if (bounds.x == -1 && bounds.y == -1) {
            Widget[] children = parentWidget.getChildren();
            if (children != null) {
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

    /**
     * Check if widget is visible in viewport
     */
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
}
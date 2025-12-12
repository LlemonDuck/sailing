package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
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

    private final NetDepthTracker netDepthTracker;
    private final BoatTracker boatTracker;
    private final Client client;
    private final SailingConfig config;

    @Inject
    public NetDepthButtonHighlighter(NetDepthTracker netDepthTracker,
                                   BoatTracker boatTracker, 
                                   Client client, 
                                   SailingConfig config) {
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
    }

    @Override
    public void shutDown() {
        log.debug("NetDepthButtonHighlighter shut down");
    }

    @Override
    public Dimension render(Graphics2D graphics) {
//        if (!shouldHighlightButtons()) {
//            return null;
//        }

        // Widget widgetSailingRows = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
        return null;
    }

    /**
     * Check if button highlighting should be active
     */
    private boolean shouldHighlightButtons() {
        return false;
    }

    /**
     * Determine which depth the nets should be set to
     */
    private NetDepth determineRequiredDepth() {
        return null;
    }

    /**
     * Highlight buttons for the specified required depth
     */
    private void highlightButtonsForDepth(Graphics2D graphics, Widget parent, NetDepth requiredDepth) {
        Color highlightColor = config.trawlingShoalHighlightColour();
        log.debug("Highlighting buttons for required depth: {}", requiredDepth);

        // Check starboard net - only highlight if opacity is 0 (player can interact)
        Widget starboardDepthWidget = parent.getChild(STARBOARD_DEPTH_WIDGET_INDEX);
        if (starboardDepthWidget != null && starboardDepthWidget.getOpacity() == 0) {
            NetDepth currentDepth = getNetDepth(parent, STARBOARD_DEPTH_WIDGET_INDEX);
            log.debug("Starboard net: current={}, required={}, opacity={}", 
                     currentDepth, requiredDepth, starboardDepthWidget.getOpacity());
            if (currentDepth != null && currentDepth != requiredDepth) {
                log.debug("Highlighting starboard net button");
                highlightNetButton(graphics, parent, currentDepth, requiredDepth, 
                                  STARBOARD_UP, STARBOARD_DOWN, highlightColor);
            }
        } else {
            log.debug("Starboard widget: present={}, opacity={}", 
                     starboardDepthWidget != null, 
                     starboardDepthWidget != null ? starboardDepthWidget.getOpacity() : "N/A");
        }

        // Check port net - only highlight if opacity is 0 (player can interact)
        Widget portDepthWidget = parent.getChild(PORT_DEPTH_WIDGET_INDEX);
        if (portDepthWidget != null && portDepthWidget.getOpacity() == 0) {
            NetDepth currentDepth = getNetDepth(parent, PORT_DEPTH_WIDGET_INDEX);
            log.debug("Port net: current={}, required={}, opacity={}", 
                     currentDepth, requiredDepth, portDepthWidget.getOpacity());
            if (currentDepth != null && currentDepth != requiredDepth) {
                log.debug("Highlighting port net button");
                highlightNetButton(graphics, parent, currentDepth, requiredDepth,
                                  PORT_UP, PORT_DOWN, highlightColor);
            }
        } else {
            log.debug("Port widget: present={}, opacity={}", 
                     portDepthWidget != null, 
                     portDepthWidget != null ? portDepthWidget.getOpacity() : "N/A");
        }
    }

    /**
     * Highlight the appropriate button for a specific net
     */
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

    /**
     * Get the current net depth using NetDepthTracker
     */
    private NetDepth getNetDepth(Widget parent, int widgetIndex) {
        // Determine which net we're checking based on widget index
        if (widgetIndex == PORT_DEPTH_WIDGET_INDEX) {
            return netDepthTracker.getPortNetDepth();
        } else if (widgetIndex == STARBOARD_DEPTH_WIDGET_INDEX) {
            return netDepthTracker.getStarboardNetDepth();
        } else {
            log.warn("Unknown widget index for net depth: {}", widgetIndex);
            return null;
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
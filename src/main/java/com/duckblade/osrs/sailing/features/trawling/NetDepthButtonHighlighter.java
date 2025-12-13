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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

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
        return config.highlightNetButtons();
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
        if (!validatePrerequisites()) {
            return null;
        }

        ensureHighlightingStateValid();

        if (!hasHighlightsToRender()) {
            return null;
        }

        Widget sailingWidget = getSailingWidget();
        if (sailingWidget == null) {
            return null;
        }

        renderCachedHighlights(graphics, sailingWidget);
        return null;
    }

    private boolean validatePrerequisites() {
        if (!canHighlightButtons()) {
            if (highlightingStateValid) {
                log.debug("Prerequisites no longer met, invalidating highlighting state");
                invalidateHighlightingState();
            }
            return false;
        }
        return true;
    }

    private void ensureHighlightingStateValid() {
        if (!highlightingStateValid) {
            updateHighlightingState();
        }
    }

    private boolean hasHighlightsToRender() {
        return shouldHighlightPort || shouldHighlightStarboard;
    }

    private Widget getSailingWidget() {
        return client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
    }

    private boolean canHighlightButtons() {
        Boat boat = boatTracker.getBoat();
        if (boat == null || boat.getNetTiers().isEmpty()) {
            return false;
        }

        return shoalTracker.hasShoal() && shoalTracker.isShoalDepthKnown();
    }

    private void invalidateHighlightingState() {
        highlightingStateValid = false;
        shouldHighlightPort = false;
        shouldHighlightStarboard = false;
        cachedRequiredDepth = null;
        cachedPortDepth = null;
        cachedStarboardDepth = null;
    }

    private void updateHighlightingState() {
        log.debug("Updating highlighting state");
        
        netDepthTracker.refreshCache();
        cacheCurrentDepths();
        calculateHighlightingDecisions();
        highlightingStateValid = true;
        
        logHighlightingDecisions();
    }

    private void cacheCurrentDepths() {
        cachedRequiredDepth = determineRequiredDepth();
        cachedPortDepth = netDepthTracker.getPortNetDepth();
        cachedStarboardDepth = netDepthTracker.getStarboardNetDepth();
        
        log.debug("Current depths - Required: {}, Port: {}, Starboard: {}", 
                 cachedRequiredDepth, cachedPortDepth, cachedStarboardDepth);
    }

    private void calculateHighlightingDecisions() {
        shouldHighlightPort = shouldHighlightNet(cachedPortDepth);
        shouldHighlightStarboard = shouldHighlightNet(cachedStarboardDepth);
    }

    private boolean shouldHighlightNet(ShoalDepth netDepth) {
        return cachedRequiredDepth != null && 
               cachedRequiredDepth != ShoalDepth.UNKNOWN &&
               netDepth != null && 
               netDepth != cachedRequiredDepth;
    }

    private void logHighlightingDecisions() {
        log.debug("Highlighting decisions - Port: {} ({}), Starboard: {} ({})",
                 shouldHighlightPort, getDepthMatchStatus(cachedPortDepth),
                 shouldHighlightStarboard, getDepthMatchStatus(cachedStarboardDepth));
    }

    private String getDepthMatchStatus(ShoalDepth netDepth) {
        return netDepth != cachedRequiredDepth ? "mismatch" : "match";
    }

    private void renderCachedHighlights(Graphics2D graphics, Widget parent) {
        Color highlightColor = config.trawlingShoalHighlightColour();

        if (shouldHighlightStarboard) {
            renderStarboardHighlight(graphics, parent, highlightColor);
        }

        if (shouldHighlightPort) {
            renderPortHighlight(graphics, parent, highlightColor);
        }
    }

    private void renderStarboardHighlight(Graphics2D graphics, Widget parent, Color highlightColor) {
        Widget starboardDepthWidget = parent.getChild(STARBOARD_DEPTH_WIDGET_INDEX);
        if (isWidgetInteractable(starboardDepthWidget)) {
            highlightNetButton(graphics, parent, cachedStarboardDepth, cachedRequiredDepth, 
                              STARBOARD_UP, STARBOARD_DOWN, highlightColor);
        }
    }

    private void renderPortHighlight(Graphics2D graphics, Widget parent, Color highlightColor) {
        Widget portDepthWidget = parent.getChild(PORT_DEPTH_WIDGET_INDEX);
        if (isWidgetInteractable(portDepthWidget)) {
            highlightNetButton(graphics, parent, cachedPortDepth, cachedRequiredDepth,
                              PORT_UP, PORT_DOWN, highlightColor);
        }
    }

    private boolean isWidgetInteractable(Widget widget) {
        return widget != null && widget.getOpacity() == 0;
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        if (!highlightingStateValid) {
            return;
        }

        if (hasShoalDepthChanged()) {
            invalidateHighlightingState();
            return;
        }

        if (haveNetDepthsChanged()) {
            invalidateHighlightingState();
        }
    }

    private boolean hasShoalDepthChanged() {
        ShoalDepth currentRequiredDepth = determineRequiredDepth();
        if (currentRequiredDepth != cachedRequiredDepth) {
            log.debug("Shoal depth changed from {} to {}, invalidating highlighting state", 
                     cachedRequiredDepth, currentRequiredDepth);
            return true;
        }
        return false;
    }

    private boolean haveNetDepthsChanged() {
        ShoalDepth currentPortDepth = netDepthTracker.getPortNetDepth();
        ShoalDepth currentStarboardDepth = netDepthTracker.getStarboardNetDepth();
        
        if (currentPortDepth != cachedPortDepth || currentStarboardDepth != cachedStarboardDepth) {
            log.debug("Net depths changed - Port: {} -> {}, Starboard: {} -> {}, invalidating highlighting state",
                     cachedPortDepth, currentPortDepth, cachedStarboardDepth, currentStarboardDepth);
            return true;
        }
        return false;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged e) {
        int varbitId = e.getVarbitId();
        if (isNetDepthVarbit(varbitId)) {
            log.debug("Net depth varbit changed ({}), invalidating highlighting state", varbitId);
            invalidateHighlightingState();
        }
    }

    private boolean isNetDepthVarbit(int varbitId) {
        return varbitId == 19206 || varbitId == 19208;
    }

    private ShoalDepth determineRequiredDepth() {
        if (!shoalTracker.isShoalDepthKnown()) {
            return null;
        }
        return shoalTracker.getCurrentShoalDepth();
    }

    private void highlightNetButton(Graphics2D graphics, Widget parent, ShoalDepth current, 
                                    ShoalDepth required, int upIndex, int downIndex, Color color) {
        int buttonIndex = getButtonIndex(current, required, upIndex, downIndex);
        Widget button = getNetWidget(parent, buttonIndex);
        
        if (isButtonHighlightable(button, parent)) {
            drawButtonHighlight(graphics, button, color);
        }
    }

    private int getButtonIndex(ShoalDepth current, ShoalDepth required, int upIndex, int downIndex) {
        return required.ordinal() < current.ordinal() ? upIndex : downIndex;
    }

    private boolean isButtonHighlightable(Widget button, Widget parent) {
        return button != null && 
               !button.isHidden() && 
               hasValidBounds(button) && 
               isWidgetInViewport(button, parent);
    }

    private boolean hasValidBounds(Widget button) {
        Rectangle bounds = button.getBounds();
        return bounds.width > 0 && bounds.height > 0;
    }

    private void drawButtonHighlight(Graphics2D graphics, Widget button, Color color) {
        Rectangle bounds = button.getBounds();
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(3));
        graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }



    private Widget getNetWidget(Widget parent, int index) {
        Widget parentWidget = parent.getChild(index);
        if (parentWidget == null) {
            return null;
        }

        Rectangle bounds = parentWidget.getBounds();
        if (bounds.x == -1 && bounds.y == -1) {
            return findChildWithValidBounds(parentWidget);
        }
        
        return parentWidget;
    }

    private Widget findChildWithValidBounds(Widget parentWidget) {
        Widget[] children = parentWidget.getChildren();
        if (children != null) {
            for (Widget child : children) {
                if (child != null && hasValidBounds(child)) {
                    return child;
                }
            }
        }
        return null;
    }

    private boolean isWidgetInViewport(Widget widget, Widget scrollContainer) {
        if (widget == null || scrollContainer == null) {
            return false;
        }
        
        Rectangle widgetBounds = widget.getBounds();
        Widget scrollViewport = findScrollViewport(scrollContainer);
        
        if (scrollViewport == null) {
            Rectangle containerBounds = scrollContainer.getBounds();
            return containerBounds.contains(widgetBounds);
        }
        
        Rectangle viewportBounds = scrollViewport.getBounds();
        Rectangle visibleArea = new Rectangle(
            viewportBounds.x,
            viewportBounds.y,
            viewportBounds.width,
            viewportBounds.height
        );
        
        return visibleArea.contains(widgetBounds);
    }

    private Widget findScrollViewport(Widget scrollContainer) {
        Widget scrollViewport = scrollContainer;
        while (scrollViewport != null && scrollViewport.getScrollHeight() == 0) {
            scrollViewport = scrollViewport.getParent();
        }
        return scrollViewport;
    }
}
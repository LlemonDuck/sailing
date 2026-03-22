package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.model.ShoalDepth;
import com.duckblade.osrs.sailing.model.SizeClass;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
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
    private int starboardNetDownWidgetIndex;
    private int starboardNetUpWidgetIndex;
    private int starboardNetSpriteIndex;
    private int portNetDownWidgetIndex;
    private int portNetUpWidgetIndex;
    private int portNetSpriteIndex;
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

    /**
     * Creates a new NetDepthButtonHighlighter with the specified dependencies.
     *
     * @param shoalTracker tracker for shoal state and depth
     * @param netDepthTracker tracker for current net depths
     * @param boatTracker tracker for boat information
     * @param client the RuneLite client instance
     * @param config sailing configuration settings
     */
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

    private void SetWidgetIndexForSloop()
    {
        Widget sailingInterface = getSailingWidget();
        Widget[] children = sailingInterface.getChildren();
        int NET_TO_DOWN_BUTTON_DELTA = 1;
        int NET_TO_UP_BUTTON_DELTA = 12;
        if (children == null) return;
        int spriteID;

        boolean firstSpriteFound = false;
        for (Widget child : children) {
            spriteID = child.getSpriteId();
            if (!isANetSprite(spriteID)) continue;
            if (!firstSpriteFound)
            {
                log.debug("first found at {}", child.getIndex());
                starboardNetSpriteIndex = child.getIndex();
                starboardNetDownWidgetIndex = child.getIndex() + NET_TO_DOWN_BUTTON_DELTA;
                starboardNetUpWidgetIndex = child.getIndex() + NET_TO_UP_BUTTON_DELTA;
                firstSpriteFound = true;
                continue;
            }
            log.debug("second found at {}", child.getIndex());
            portNetSpriteIndex = child.getIndex();
            portNetDownWidgetIndex = child.getIndex()  + NET_TO_DOWN_BUTTON_DELTA;
            portNetUpWidgetIndex = child.getIndex() + NET_TO_UP_BUTTON_DELTA;
        }
    }

    private boolean isANetSprite(int spriteID)
    {
        int netMinSpriteID = 7080;
        int netMaxSpiteID = 7083;
        Range<Integer> validRange = Range.closed(netMinSpriteID, netMaxSpiteID);
        return validRange.contains(spriteID);
    }

    private void SetWidgetIndexForSkiff()
    {
        Widget sailingInterface = getSailingWidget();
        Widget[] children = sailingInterface.getChildren();
        int NET_TO_DOWN_BUTTON_DELTA = 1;
        int NET_TO_UP_BUTTON_DELTA = 12;
        int NET_SPRITE_ID = 7080;
        if (children == null) return;
        for (Widget child : children) {
            int spriteID = child.getSpriteId();
            if (spriteID != NET_SPRITE_ID) continue;
            starboardNetSpriteIndex = child.getIndex();
            starboardNetDownWidgetIndex = child.getIndex() + NET_TO_DOWN_BUTTON_DELTA;
            starboardNetUpWidgetIndex = child.getIndex() + NET_TO_UP_BUTTON_DELTA;
        }
    }

    private boolean validatePrerequisites() {
        if (!canHighlightButtons()) {
            if (highlightingStateValid) {
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
        if (boat == null || boat.getFishingNets().isEmpty()) {
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
        netDepthTracker.refreshCache();
        cacheCurrentDepths();
        calculateHighlightingDecisions();
        highlightingStateValid = true;
    }

    private void cacheCurrentDepths() {
        cachedRequiredDepth = determineRequiredDepth();
        cachedPortDepth = netDepthTracker.getPortNetDepth();
        cachedStarboardDepth = netDepthTracker.getStarboardNetDepth();
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



    private void renderCachedHighlights(Graphics2D graphics, Widget parent) {
        Color highlightColor = config.trawlingShoalHighlightColour();

        if (shouldHighlightStarboard) {
            renderNetHighlight(graphics, parent, highlightColor, 
                starboardNetSpriteIndex, starboardNetUpWidgetIndex, starboardNetDownWidgetIndex, cachedStarboardDepth);
        }

        if (shouldHighlightPort) {
            renderNetHighlight(graphics, parent, highlightColor, 
                portNetSpriteIndex, portNetUpWidgetIndex, portNetDownWidgetIndex, cachedPortDepth);
        }
    }

    private void renderNetHighlight(Graphics2D graphics, Widget parent, Color highlightColor, 
                                   int netSpriteIndex, int netUpWidgetIndex, int netDownWidgetIndex, ShoalDepth cachedDepth) {
        Widget netDepthWidget = parent.getChild(netSpriteIndex);
        if (netDepthWidget == null) return;
        if (starboardNetUpWidgetIndex == 0 || starboardNetDownWidgetIndex == 0)
        {
            initializeWidgetIndices();
        }

        if (isWidgetInteractable(netDepthWidget)) {
            highlightNetButton(graphics, parent, cachedDepth, cachedRequiredDepth,
                    netUpWidgetIndex, netDownWidgetIndex, highlightColor);
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
        return currentRequiredDepth != cachedRequiredDepth;
    }

    private boolean haveNetDepthsChanged() {
        ShoalDepth currentPortDepth = netDepthTracker.getPortNetDepth();
        ShoalDepth currentStarboardDepth = netDepthTracker.getStarboardNetDepth();
        
        return currentPortDepth != cachedPortDepth || currentStarboardDepth != cachedStarboardDepth;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged e) {
        int varbitId = e.getVarbitId();
        if (isNetDepthVarbit(varbitId)) {
            invalidateHighlightingState();
        }
    }

    private boolean isNetDepthVarbit(int varbitId) {
        return varbitId == VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_0_DEPTH || 
               varbitId == VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_1_DEPTH;
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

    private void initializeWidgetIndices()
    {
        Boat boat = boatTracker.getBoat();
        if (boat == null) return;

        SizeClass boatSize = boat.getSizeClass();
        log.debug("Boat Size: {}", boatSize);
        switch(boatSize)
        {
            case SLOOP:
                SetWidgetIndexForSloop();
                break;
            case SKIFF:
                SetWidgetIndexForSkiff();
                break;
            default:
                return;
        }
        log.debug("Starboard Down Found at {}", starboardNetDownWidgetIndex);
        log.debug("Starboard Up Found at {}", starboardNetUpWidgetIndex);
        log.debug("Port Down Found at {}", portNetDownWidgetIndex);
        log.debug("Port Up Found at {}", portNetUpWidgetIndex);
    }
}
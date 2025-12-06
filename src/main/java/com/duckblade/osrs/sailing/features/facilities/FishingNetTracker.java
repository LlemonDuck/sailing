package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Slf4j
@Singleton
public class FishingNetTracker extends Overlay
        implements PluginLifecycleComponent {

    private final Client client;
    private final SailingConfig config;

    // Chat message constants for net depth detection
    private static final String CHAT_NET_TOO_DEEP = "net is too deep";
    private static final String CHAT_NET_TOO_SHALLOW = "net is not deep enough";
    private static final String CHAT_NET_CORRECT = "the net to the correct depth";
    
    // Chat message prefixes for identifying player vs crewmate messages
    private static final String CHAT_YOUR_NET = "Your net";
    private static final String CHAT_YOUR_NET_COLORED = "<col=ff3045>Your net";
    private static final String CHAT_YOU_RAISE = "You raise";
    private static final String CHAT_YOU_RAISE_COLORED = "<col=229628>You raise";
    private static final String CHAT_YOU_LOWER = "You lower";
    private static final String CHAT_YOU_LOWER_COLORED = "<col=229628>You lower";
    private static final String CHAT_CREWMATE_NET = "'s net";
    private static final String CHAT_CREWMATE_RAISES = "raises the net";
    private static final String CHAT_CREWMATE_LOWERS = "lowers the net";
    
    // Widget indices for fishing net controls
    private static final int STARBOARD_DOWN = 97;
    private static final int STARBOARD_UP = 108;
    private static final int PORT_DOWN = 132;
    private static final int PORT_UP = 143;
    
    // Track each net independently
    private boolean shouldHighlightStarboard = false;
    private boolean shouldHighlightPort = false;
    private boolean isStarboardTooDeep = false;
    private boolean isPortTooDeep = false;


    @Inject
    public FishingNetTracker(Client client, SailingConfig config)
    {
        this.client = client;
        this.config = config;

        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(1000.0f);
    }
    
    @Override
    public void startUp()
    {
        log.debug("FishingNetTracker started");
    }
    
    @Override
    public void shutDown()
    {
        log.debug("FishingNetTracker shut down");
        shouldHighlightStarboard = false;
        shouldHighlightPort = false;
    }



    @Subscribe
    public void onChatMessage(ChatMessage e)
    {
        String message = e.getMessage();
        log.debug("Chat message received - Type: {}, Message: '{}'", e.getType(), message);
        
        // Determine if message is about player or crewmate
        boolean isPlayerMessage = message.startsWith(CHAT_YOUR_NET) || 
                                  message.startsWith(CHAT_YOUR_NET_COLORED) ||
                                  message.startsWith(CHAT_YOU_RAISE) || 
                                  message.startsWith(CHAT_YOU_RAISE_COLORED) ||
                                  message.startsWith(CHAT_YOU_LOWER) || 
                                  message.startsWith(CHAT_YOU_LOWER_COLORED);
        boolean isCrewmateMessage = !isPlayerMessage && (message.contains(CHAT_CREWMATE_NET) || 
                                                          message.contains(CHAT_CREWMATE_RAISES) || 
                                                          message.contains(CHAT_CREWMATE_LOWERS));
        
        log.debug("Message analysis - isPlayerMessage: {}, isCrewmateMessage: {}", isPlayerMessage, isCrewmateMessage);
        
        // Determine which net to update based on config
        boolean updateStarboard = false;
        boolean updatePort = false;
        
        if (isPlayerMessage) {
            if (config.trawlingStarboardNetOperator() == SailingConfig.NetOperator.PLAYER) {
                updateStarboard = true;
            }
            if (config.trawlingPortNetOperator() == SailingConfig.NetOperator.PLAYER) {
                updatePort = true;
            }
        } else if (isCrewmateMessage) {
            if (config.trawlingStarboardNetOperator() == SailingConfig.NetOperator.CREWMATE) {
                updateStarboard = true;
            }
            if (config.trawlingPortNetOperator() == SailingConfig.NetOperator.CREWMATE) {
                updatePort = true;
            }
        }
        
        log.debug("Net updates - updateStarboard: {}, updatePort: {}", updateStarboard, updatePort);
        
        // Check for net adjustment messages
        if (message.contains(CHAT_NET_CORRECT)) {
            log.debug("Net correct depth detected");
            if (updateStarboard) {
                log.debug("Clearing starboard highlight");
                shouldHighlightStarboard = false;
            }
            if (updatePort) {
                log.debug("Clearing port highlight");
                shouldHighlightPort = false;
            }
        }
        else if (message.contains(CHAT_NET_TOO_DEEP)) {
            log.debug("Net too deep detected");
            if (updateStarboard) {
                log.debug("Setting starboard highlight (too deep)");
                shouldHighlightStarboard = true;
                isStarboardTooDeep = true;
            }
            if (updatePort) {
                log.debug("Setting port highlight (too deep)");
                shouldHighlightPort = true;
                isPortTooDeep = true;
            }
        }
        else if (message.contains(CHAT_NET_TOO_SHALLOW)) {
            log.debug("Net too shallow detected");
            if (updateStarboard) {
                log.debug("Setting starboard highlight (too shallow)");
                shouldHighlightStarboard = true;
                isStarboardTooDeep = false;
            }
            if (updatePort) {
                log.debug("Setting port highlight (too shallow)");
                shouldHighlightPort = true;
                isPortTooDeep = false;
            }
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.trawlingHighlightNetButtons()) {
            return null;
        }
        
        if (!shouldHighlightStarboard && !shouldHighlightPort) {
            return null;
        }
        
        Widget widgetSailingRows = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
        if (widgetSailingRows == null) {
            return null;
        }
        
        // Get fresh widget references each render to account for scrolling
        Widget starboardDown = getNetWidget(widgetSailingRows, STARBOARD_DOWN);
        Widget starboardUp = getNetWidget(widgetSailingRows, STARBOARD_UP);
        Widget portDown = getNetWidget(widgetSailingRows, PORT_DOWN);
        Widget portUp = getNetWidget(widgetSailingRows, PORT_UP);
        
        Color highlightColor = config.trawlingHighlightColour();
        
        // Highlight starboard net buttons if needed
        if (shouldHighlightStarboard) {
            if (isStarboardTooDeep) {
                // Net is too deep, highlight UP button (raise the net)
                highlightWidget(graphics, starboardUp, highlightColor);
            } else {
                // Net is too shallow, highlight DOWN button (lower the net)
                highlightWidget(graphics, starboardDown, highlightColor);
            }
        }
        
        // Highlight port net buttons if needed
        if (shouldHighlightPort) {
            if (isPortTooDeep) {
                // Net is too deep, highlight UP button (raise the net)
                highlightWidget(graphics, portUp, highlightColor);
            } else {
                // Net is too shallow, highlight DOWN button (lower the net)
                highlightWidget(graphics, portDown, highlightColor);
            }
        }

        return null;
    }
    
    private void highlightWidget(Graphics2D graphics, Widget widget, Color color) {
        if (widget == null || widget.isHidden()) {
            return;
        }
        
        Rectangle bounds = widget.getBounds();
        if (bounds.width == 0 || bounds.height == 0) {
            return;
        }
        
        // Check if widget is actually visible within the scrollable container
        if (!isWidgetInView(widget)) {
            return;
        }
        
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(3));
        graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
    
    private boolean isWidgetInView(Widget widget) {
        if (widget == null) {
            return false;
        }
        
        Rectangle widgetBounds = widget.getBounds();
        
        // Get the scrollable container
        Widget scrollContainer = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
        if (scrollContainer == null) {
            return false;
        }
        
        // The parent of FACILITIES_ROWS is the visible viewport
        Widget viewport = scrollContainer.getParent();
        if (viewport == null) {
            // Fallback: use the scroll container itself
            viewport = scrollContainer;
        }
        
        Rectangle viewportBounds = viewport.getBounds();
        
        // Widget is visible only if it's within the viewport bounds
        // Check if the widget's Y position is within the visible area
        boolean isVisible = widgetBounds.y >= viewportBounds.y && 
                           widgetBounds.y + widgetBounds.height <= viewportBounds.y + viewportBounds.height;
        
        return isVisible;
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
            // Parent has valid bounds, use it directly
            return parentWidget;
        }
        
        return null;
    }

}

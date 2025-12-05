package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.Widget;
import com.duckblade.osrs.sailing.SailingConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;

@Slf4j
public class FishingNetTracker extends Overlay
        implements PluginLifecycleComponent {

    private final Client client;
    private final SailingConfig config;
    private final BoatTracker boatTracker;

    private static final String CHAT_NET_TOO_DEEP = "net is too deep";
    private static final String CHAT_NET_TOO_SHALLOW = "net is not deep enough";
    private static final String CHAT_NET_CORRECT = "the net to the correct depth";
    
    // Widget indices for fishing net controls
    private static final int STARBOARD_DOWN = 97;
    private static final int STARBOARD_UP = 108;
    private static final int PORT_DOWN = 132;
    private static final int PORT_UP = 143;
    
    // Widget indices for crewmate/player sprites (to identify which net is being used)
    private static final int STARBOARD_SPRITE_INDEX = 119;
    private static final int PORT_SPRITE_INDEX = 154;
    
    // Sprite IDs to identify who is using the net
    private static final int CREWMATE_SPRITE_ID = 7095;
    private static final int PLAYER_SPRITE_ID = 7103;
    
    // Track each net independently
    private boolean shouldHighlightStarboard = false;
    private boolean shouldHighlightPort = false;
    private boolean isStarboardTooDeep = false;
    private boolean isPortTooDeep = false;


    @Inject
    public FishingNetTracker(Client client, SailingConfig config, BoatTracker boatTracker)
    {
        this.client = client;
        this.config = config;
        this.boatTracker = boatTracker;

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
        boolean isPlayerMessage = message.startsWith("Your net") || message.startsWith("<col=ff3045>Your net") ||
                                  message.startsWith("You raise") || message.startsWith("<col=229628>You raise") ||
                                  message.startsWith("You lower") || message.startsWith("<col=229628>You lower");
        boolean isCrewmateMessage = !isPlayerMessage && (message.contains("'s net") || 
                                                          message.contains("raises the net") || 
                                                          message.contains("lowers the net"));
        
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
        
        // Highlight starboard net buttons if needed
        if (shouldHighlightStarboard) {
            if (isStarboardTooDeep) {
                // Net is too deep, highlight UP button (raise the net)
                highlightWidget(graphics, starboardUp, config.fishingNetRaiseHighlightColour());
            } else {
                // Net is too shallow, highlight DOWN button (lower the net)
                highlightWidget(graphics, starboardDown, config.fishingNetLowerHighlightColour());
            }
        }
        
        // Highlight port net buttons if needed
        if (shouldHighlightPort) {
            if (isPortTooDeep) {
                // Net is too deep, highlight UP button (raise the net)
                highlightWidget(graphics, portUp, config.fishingNetRaiseHighlightColour());
            } else {
                // Net is too shallow, highlight DOWN button (lower the net)
                highlightWidget(graphics, portDown, config.fishingNetLowerHighlightColour());
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

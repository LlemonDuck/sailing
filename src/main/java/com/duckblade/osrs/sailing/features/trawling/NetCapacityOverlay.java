package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Slf4j
@Singleton
public class NetCapacityOverlay extends OverlayPanel
        implements PluginLifecycleComponent {

    private static final int MAX_FISH_PER_NET = 125;

    private final Client client;
    private final SailingConfig config;
    private final BoatTracker boatTracker;

    private int totalFishCount = 0;

    @Inject
    public NetCapacityOverlay(Client client, SailingConfig config, BoatTracker boatTracker) {
        this.client = client;
        this.config = config;
        this.boatTracker = boatTracker;
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        return config.trawlingShowNetCapacity();
    }

    @Override
    public void startUp() {
        log.debug("NetCapacityOverlay started");
    }

    @Override
    public void shutDown() {
        log.debug("NetCapacityOverlay shut down");
        totalFishCount = 0;
    }

    @Subscribe
    public void onChatMessage(ChatMessage e) {
        if (!SailingUtil.isSailing(client) ||
            (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)) {
            return;
        }

        String message = e.getMessage();

        // Check for net emptying message
        if (message.contains("empty the nets into the cargo hold")) {
            log.debug("Nets emptied, resetting fish count");
            totalFishCount = 0;
            return;
        }

        // Check for fish catch messages
        if (message.contains(" catch") && (message.contains("!") || e.getType() == ChatMessageType.SPAM)) {
            int fishCount = parseFishCount(message);
            if (fishCount > 0) {
                Boat boat = boatTracker.getBoat();
                int maxCapacity = boat != null ? boat.getNetCapacity() : MAX_FISH_PER_NET;
                totalFishCount = Math.min(totalFishCount + fishCount, maxCapacity);
                log.debug("Caught {} fish, total: {}/{}", fishCount, totalFishCount, maxCapacity);
            }
        }
    }

    private int parseFishCount(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Check for "a fish" or "an fish" (singular)
        if (lowerMessage.contains(" a ") && !lowerMessage.contains(" catch a ")) {
            return 1;
        }
        
        // Check for number words
        String[] words = {"one", "two", "three", "four", "five", "six"};
        for (int i = 0; i < words.length; i++) {
            if (lowerMessage.contains(" " + words[i] + " ")) {
                return i + 1;
            }
        }
        return 0;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!SailingUtil.isSailing(client)) {
            return null;
        }

        Boat boat = boatTracker.getBoat();
        if (boat == null) {
            return null;
        }

        int maxCapacity = boat.getNetCapacity();
        if (maxCapacity == 0) {
            return null;
        }

        // Choose color based on how full the nets are
        Color textColor;
        float fillPercent = (float) totalFishCount / maxCapacity;
        if (fillPercent >= 0.9f) {
            textColor = Color.RED; // Nearly full
        } else if (fillPercent >= 0.7f) {
            textColor = Color.ORANGE; // Getting full
        } else {
            textColor = Color.WHITE; // Plenty of space
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Net Capacity:")
                .right(totalFishCount + "/" + maxCapacity)
                .rightColor(textColor)
                .build());

        return super.render(graphics);
    }
}

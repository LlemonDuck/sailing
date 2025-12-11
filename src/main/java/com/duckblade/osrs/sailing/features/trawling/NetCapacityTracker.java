package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class NetCapacityTracker implements PluginLifecycleComponent {

    private static final int MAX_FISH_PER_NET = 125;

    private final Client client;
    private final BoatTracker boatTracker;

    @Getter
    private int totalFishCount = 0;

    @Inject
    public NetCapacityTracker(Client client, BoatTracker boatTracker) {
        this.client = client;
        this.boatTracker = boatTracker;
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        return config.trawlingShowNetCapacity();
    }

    @Override
    public void startUp() {
        log.debug("NetCapacityTracker started");
        totalFishCount = 0;
    }

    @Override
    public void shutDown() {
        log.debug("NetCapacityTracker shut down");
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
        if (lowerMessage.contains(" a ") || lowerMessage.contains(" catch an ")) {
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

    public int getMaxCapacity() {
        Boat boat = boatTracker.getBoat();
        return boat != null ? boat.getNetCapacity() : 0;
    }
}

package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@Singleton
public class FishCaughtTracker implements PluginLifecycleComponent {
    public static final Pattern CATCH_FISH_REGEX =
        Pattern.compile("^(.+?) catch(?:es)? (an?|two|three|four|five|six) (.+?)!$");
    private final Client client;
    private final BoatTracker boatTracker;

    @Getter
    private final Map<String, Integer> fishCaught = new HashMap<>();

    @Getter
    private int fishInNet = 0;

    private String lastFishCaught;

    @Inject
    public FishCaughtTracker(Client client, BoatTracker boatTracker) {
        this.client = client;
        this.boatTracker = boatTracker;
    }

    @Override
    public void startUp() {
        log.debug("FishCaughtTracker started");
        reset();
    }

    @Override
    public void shutDown() {
        log.debug("FishCaughtTracker shut down");
        reset();
    }

    @Subscribe
    public void onChatMessage(ChatMessage e) {
        if (!SailingUtil.isSailing(client) ||
            (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)) {
            return;
        }

        String message = e.getMessage();
        if (message.equals("You empty the nets into the cargo hold.")) {
            // TODO: handle trying to empty net when already empty
            log.debug("Nets emptied");
            fishInNet = 0;
            return;
        }

        if (message.equals("Trawler's trust: You catch an additional fish.")) {
            addFish(message, 1, lastFishCaught, "Trawler's trust");
            return;
        }

        Matcher matcher = CATCH_FISH_REGEX.matcher(message);
        if (!matcher.find()) {
            return;
        }

        String catcher = matcher.group(1);
        String quantityWord = matcher.group(2);
        String fish = matcher.group(3);

        int quantity = wordToNumber(quantityWord);
        if (quantity == -1) {
            log.debug("Unable to find quantity for message {}", message);
            return;
        }

        addFish(message, quantity, fish, catcher);
    }

    private void addFish(String message, int quantity, String fish, String catcher) {
        log.debug(message);
        log.debug("Adding {} {} caught by {}; total: {}", quantity, fish, catcher, fishCaught.get(fish));

        fishCaught.merge(fish, quantity, Integer::sum);
        fishInNet += quantity;
        lastFishCaught = fish;
    }

    private int wordToNumber(String word) {
        if (word.equals("an")) {
            word = "a";
        }

        String[] words = {"a", "two", "three", "four", "five", "six"};
        int wordIndex = ArrayUtils.indexOf(words, word);

        if (wordIndex == ArrayUtils.INDEX_NOT_FOUND) {
            log.debug("Unable to find quantity for word {}", word);
            return -1;
        }

        return wordIndex + 1;
    }

    public int getNetCapacity() {
        Boat boat = boatTracker.getBoat();
        return boat != null ? boat.getNetCapacity() : 0;
    }

    private void reset() {
        fishCaught.clear();
        fishInNet = 0;
        lastFishCaught = null;
    }
}

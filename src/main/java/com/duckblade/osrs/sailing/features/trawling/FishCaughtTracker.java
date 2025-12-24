package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@Singleton
public class FishCaughtTracker implements PluginLifecycleComponent {
    public static final Pattern CATCH_FISH_REGEX =
        Pattern.compile("^(.+?) catch(?:es)? (an?|two|three|four|five|six) (.+?)!$");
    private final Client client;
    private final BoatTracker boatTracker;

    /**
     * All the fish that was caught into the net since it was last emptied.
     */
    private final EnumMap<Shoal, Integer> fishInNet = new EnumMap<>(Shoal.class);

    /**
     * All the fish that was collected by emptying the nets.
     */
    private final EnumMap<Shoal, Integer> fishCollected = new EnumMap<>(Shoal.class);

    /**
     * Creates a new FishCaughtTracker with the specified dependencies.
     *
     * @param client the RuneLite client instance
     * @param boatTracker tracker for boat information including net capacity
     */
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
    public void onGameStateChanged(GameStateChanged e) {
        GameState state = e.getGameState();
        if (state == GameState.HOPPING || state == GameState.LOGGING_IN) {
            log.debug("{}; nets are forcibly emptied", state);
            log.debug("lost fish: {}", fishInNet);
            fishInNet.clear();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage e) {
        if (!SailingUtil.isSailing(client) ||
            (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)) {
            return;
        }

        String message = e.getMessage();
        if (message.equals("You empty the nets into the cargo hold.")) {
            // TODO: handle trying to empty net when already empty (in case of desync)
            log.debug("Nets manually emptied; collecting fish: {}", fishInNet);

            for (var entry : fishInNet.entrySet()) {
                fishCollected.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }

            fishInNet.clear();
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

        final var shoal = Shoal.byName(fish);
        if (shoal == null) {
            return;
        }

        log.debug(message);
        log.debug("{} {} caught by {}; total: {}", quantity, fish, catcher, fishInNet.get(shoal));
        fishInNet.merge(shoal, quantity, Integer::sum);
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

    /**
     * Gets the current net capacity based on the player's boat.
     *
     * @return the net capacity, or 0 if no boat is available
     */
    public int getNetCapacity() {
        Boat boat = boatTracker.getBoat();
        return boat != null ? boat.getNetCapacity() : 0;
    }

	public int getFishInNetCount() {
		return fishInNet.values()
			.stream()
			.reduce(Integer::sum)
			.orElse(0);
	}

    /**
     * All fish caught, either currently in the net or previously collected.
     */
    public Map<Shoal, Integer> getFishCaught() {
        var fishCaught = new EnumMap<>(fishCollected);
        for (var entry : fishInNet.entrySet()) {
            fishCaught.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        return Collections.unmodifiableMap(fishCaught);
    }

    private void reset() {
        fishInNet.clear();
        fishCollected.clear();
    }
}

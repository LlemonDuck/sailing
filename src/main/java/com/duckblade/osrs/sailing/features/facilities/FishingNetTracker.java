package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.features.util.BoatTracker;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;

public class FishingNetTracker {

    private final Client client;
    private final ConfigManager configManager;
    private final BoatTracker boatTracker;

    @Inject
    public FishingNetTracker(Client client, ConfigManager configManager, BoatTracker boatTracker)
    {
        this.client = client;
        this.configManager = configManager;
        this.boatTracker = boatTracker;

    }
}

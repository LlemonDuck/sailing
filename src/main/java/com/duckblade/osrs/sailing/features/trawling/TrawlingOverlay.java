package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import org.apache.commons.text.WordUtils;

/**
 * Combined overlay for trawling features including net capacity and fish caught
 */
@Slf4j
@Singleton
public class TrawlingOverlay extends OverlayPanel
        implements PluginLifecycleComponent {

    private final Client client;
    private final FishCaughtTracker fishCaughtTracker;
    private final SailingConfig config;

    @Inject
    public TrawlingOverlay(Client client, FishCaughtTracker fishCaughtTracker, SailingConfig config) {
        this.client = client;
        this.fishCaughtTracker = fishCaughtTracker;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        // Enable if either feature is enabled
        return config.trawlingShowNetCapacity() || config.trawlingShowFishCaught();
    }

    @Override
    public void startUp() {
        log.debug("TrawlingOverlay started");
    }

    @Override
    public void shutDown() {
        log.debug("TrawlingOverlay shut down");
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!SailingUtil.isSailing(client)) {
            return null;
        }

        panelComponent.getChildren().clear();
        boolean hasContent = false;



        // Add fish caught section if enabled and available
        if (shouldShowFishCaught()) {
            var fishCaught = fishCaughtTracker.getFishCaught();
            if (!fishCaught.isEmpty()) {
                if (hasContent) {
                    panelComponent.getChildren().add(LineComponent.builder().build());
                }

                int totalFish = fishCaught.values().stream().reduce(Integer::sum).orElse(0);
                for (var entry : fishCaught.entrySet()) {
                    var shoal = entry.getKey();
                    panelComponent.getChildren().add(LineComponent.builder()
                        .leftColor(shoal.getColor())
                        .left(shoal.getName())
                        .right(String.format("%d (%.0f%%)", entry.getValue(), 100f * entry.getValue() / totalFish))
                        .build());
                }

                panelComponent.getChildren().add(LineComponent.builder()
                    .left("TOTAL")
                    .right(String.valueOf(totalFish))
                    .build());

                hasContent = true;
            }
        }

        // Add net capacity section if enabled and available
        if (shouldShowNetCapacity()) {
            int maxCapacity = fishCaughtTracker.getNetCapacity();
            if (maxCapacity > 0) {
                if (hasContent) {
                    panelComponent.getChildren().add(LineComponent.builder().build());
                }

                int fishInNetTotal = fishCaughtTracker.getFishInNetCount();

                // Choose color based on how full the nets are
                Color textColor;
                float fillPercent = (float) fishInNetTotal / maxCapacity;
                if (fillPercent >= 0.9f) {
                    textColor = Color.RED; // Nearly full
                } else if (fillPercent >= 0.7f) {
                    textColor = Color.ORANGE; // Getting full
                } else {
                    textColor = Color.WHITE; // Plenty of space
                }

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Net:")
                        .right(fishInNetTotal + "/" + maxCapacity)
                        .rightColor(textColor)
                        .build());

                hasContent = true;
            }
        }

        if (hasContent) {
            panelComponent.getChildren().add(0, TitleComponent.builder()
                .text("Trawling")
                .color(Color.CYAN)
                .build());

            return super.render(graphics);
        }

        return null;
    }

    private boolean shouldShowNetCapacity() {
        return config.trawlingShowNetCapacity();
    }

    private boolean shouldShowFishCaught() {
        return config.trawlingShowFishCaught();
    }
}
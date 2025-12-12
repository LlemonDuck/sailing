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
import java.awt.*;
import org.apache.commons.text.WordUtils;

/**
 * Combined overlay for trawling features including net capacity and depth timer
 */
@Slf4j
@Singleton
public class TrawlingOverlay extends OverlayPanel
        implements PluginLifecycleComponent {

    private final Client client;
    private final FishCaughtTracker fishCaughtTracker;
    private final NetDepthTimer netDepthTimer;
    private final SailingConfig config;

    @Inject
    public TrawlingOverlay(Client client, FishCaughtTracker fishCaughtTracker, NetDepthTimer netDepthTimer, SailingConfig config) {
        this.client = client;
        this.fishCaughtTracker = fishCaughtTracker;
        this.netDepthTimer = netDepthTimer;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        // Enable if either feature is enabled
        return config.trawlingShowNetCapacity() || config.trawlingShowNetDepthTimer();
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

        // Add net depth timer section if enabled and available
        if (shouldShowDepthTimer()) {
            NetDepthTimer.TimerInfo timerInfo = netDepthTimer.getTimerInfo();
            if (timerInfo != null) {

                if (!timerInfo.isActive()) {
                    // Show waiting or calibrating message
                    String message = timerInfo.isWaiting() ? "Waiting for shoal to stop" : "Calibrating...";
                    Color color = timerInfo.isWaiting() ? Color.ORANGE : Color.YELLOW;
                    
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Depth Timer: " + message)
                            .leftColor(color)
                            .build());
                } else {
                    // Show ticks until depth change
                    int ticksUntilChange = timerInfo.getTicksUntilDepthChange();
                    Color tickColor = ticksUntilChange <= 5 ? Color.RED : Color.WHITE;
                    
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Depth Change: " + ticksUntilChange + " ticks")
                            .leftColor(tickColor)
                            .build());
                }

                hasContent = true;
            }
        }

        // Add fish caught section if enabled and available
        if (shouldShowFishCaught()) {
            Map<String, Integer> fishCaught = fishCaughtTracker.getFishCaught();
            if (!fishCaught.isEmpty()) {
                if (hasContent) {
                    panelComponent.getChildren().add(LineComponent.builder().build());
                }

                int totalFish = fishCaught.values().stream().reduce(Integer::sum).orElse(0);
                for (Map.Entry<String, Integer> fish : fishCaught.entrySet()) {
                    panelComponent.getChildren().add(LineComponent.builder()
                        .left(WordUtils.capitalize(fish.getKey()))
                        .right(String.format("%d (%.0f%%)", fish.getValue(), 100f * fish.getValue() / totalFish))
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

                int totalFishCount = fishCaughtTracker.getFishInNet();

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
                        .left("Net:")
                        .right(totalFishCount + "/" + maxCapacity)
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

    private boolean shouldShowDepthTimer() {
        return config.trawlingShowNetDepthTimer();
    }

    private boolean shouldShowNetCapacity() {
        return config.trawlingShowNetCapacity();
    }

    private boolean shouldShowFishCaught() {
        return config.trawlingShowFishCaught();
    }
}
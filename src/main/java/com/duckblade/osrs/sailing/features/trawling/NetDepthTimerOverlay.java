package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Slf4j
@Singleton
public class NetDepthTimerOverlay extends OverlayPanel
        implements PluginLifecycleComponent {

    private final NetDepthTimer netDepthTimer;

    @Inject
    public NetDepthTimerOverlay(NetDepthTimer netDepthTimer) {
        this.netDepthTimer = netDepthTimer;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        return config.trawlingShowNetDepthTimer();
    }

    @Override
    public void startUp() {
        log.debug("NetDepthTimerOverlay started");
    }

    @Override
    public void shutDown() {
        log.debug("NetDepthTimerOverlay shut down");
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        NetDepthTimer.TimerInfo timerInfo = netDepthTimer.getTimerInfo();
        
        if (timerInfo == null) {
            return null;
        }

        panelComponent.getChildren().clear();

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Net Depth Timer")
                .color(Color.CYAN)
                .build());

        if (!timerInfo.isActive()) {
            // Show calibration message
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right("Calibrating...")
                    .rightColor(Color.YELLOW)
                    .build());
            return super.render(graphics);
        }

        // Show current depth requirement
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Required Depth:")
                .right(timerInfo.getCurrentDepth().toString())
                .rightColor(getDepthColor(timerInfo.getCurrentDepth()))
                .build());

        // Show ticks until depth change
        int ticksUntilChange = timerInfo.getTicksUntilDepthChange();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Ticks Until Change:")
                .right(String.valueOf(ticksUntilChange))
                .rightColor(ticksUntilChange <= 5 ? Color.RED : Color.WHITE)
                .build());

        // Show next depth
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Next Depth:")
                .right(timerInfo.getNextDepth().toString())
                .rightColor(getDepthColor(timerInfo.getNextDepth()))
                .build());

        // Show total ticks at waypoint
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Waypoint Tick:")
                .right(timerInfo.getCurrentTick() + " / " + timerInfo.getTotalDuration())
                .build());

        return super.render(graphics);
    }

    private Color getDepthColor(String depth) {
        switch (depth.toUpperCase()) {
            case "SHALLOW":
                return new Color(135, 206, 250); // Light blue
            case "MODERATE":
                return new Color(255, 215, 0); // Gold
            case "DEEP":
                return new Color(0, 0, 139); // Dark blue
            default:
                return Color.WHITE;
        }
    }
}

package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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

    private final Client client;
    private final NetCapacityTracker netCapacityTracker;

    @Inject
    public NetCapacityOverlay(Client client, NetCapacityTracker netCapacityTracker) {
        this.client = client;
        this.netCapacityTracker = netCapacityTracker;
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
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!SailingUtil.isSailing(client)) {
            return null;
        }

        int maxCapacity = netCapacityTracker.getMaxCapacity();
        if (maxCapacity == 0) {
            return null;
        }

        int totalFishCount = netCapacityTracker.getTotalFishCount();

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

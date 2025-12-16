package com.duckblade.osrs.sailing.features.salvaging;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Singleton
public class SalvagingOverlay extends OverlayPanel implements PluginLifecycleComponent
{
    private final SailingConfig config;
    private final SalvagingNotification salvagingNotification;

    @Inject
    public SalvagingOverlay( SailingConfig config, SalvagingNotification salvagingNotification)
    {
        this.config = config;
        this.salvagingNotification = salvagingNotification;
    }

    @Override
    public boolean isEnabled(SailingConfig config)
    {
        return config.salvagingOverlayEnabled();
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!salvagingNotification.atSalvage() || !config.salvagingOverlayEnabled())
        {
            return null;
        }
        boolean playerSalvaging = salvagingNotification.isPlayerSalvaging();

        String player = playerSalvaging ? "Yes":"No";
        Color playerColor = playerSalvaging ? Color.GREEN:Color.RED;

        int crewCount = salvagingNotification.getCrewmates().size();
        int crewSalvaging = (int) salvagingNotification.getCrewSalvaging().values().stream().filter(b -> b).count();
        String crewSalvage = crewSalvaging + "/" + crewCount;
        Color crewSalvageColor = (crewSalvaging > 0) ? Color.GREEN:Color.RED;

        panelComponent.getChildren().add(TitleComponent.builder().text("Salvaging").color(Color.WHITE).build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Player Salvaging:").leftColor(playerColor)
                .right(player).rightColor(playerColor)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Crew Salvaging:").leftColor(crewSalvageColor)
                .right(crewSalvage).rightColor(crewSalvageColor)
                .build());

        if (salvagingNotification.isCargoFull())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Cargo:").leftColor(Color.RED)
                    .right("FULL").rightColor(Color.RED)
                    .build());
        }


        panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth("Player Salvaging: Yes") + 10, 0));

        return super.render(graphics);
    }
}
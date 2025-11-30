package com.duckblade.osrs.sailing.features.facilities;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.LinkedList;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

@Slf4j
@Singleton
public class SpeedOverlay 
    extends OverlayPanel
    implements PluginLifecycleComponent
{
    private static final int SAMPLE_SIZE = 10;

    private final Client client;
    private final SailingConfig config;
    private final LinkedList<Double> recentDistances = new LinkedList<>();
    private double speed = 0.0;
    private WorldPoint previousPosition = null;

    @Inject
    public SpeedOverlay(Client client, SailingConfig config)
    {
        this.client = client;
        this.config = config;

        setPreferredPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public boolean isEnabled(SailingConfig config)
    {
        return this.config.showSpeedOverlay();
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (SailingUtil.isSailing(this.client) == false)
        {
            return null;
        }

        double metersPerSecond = this.speed / 0.6;
        double knots = metersPerSecond * 1.944;
        String formattedKnots = String.format("%.2f", knots);

        getPanelComponent().getChildren()
            .add(LineComponent.builder()
                .left("Speed: ")
                .right(formattedKnots + " knots")
                .build());

        return super.render(graphics);
    }

    @Subscribe
    public void onGameTick(GameTick e)
    {
        WorldPoint currentPosition = SailingUtil.getTopLevelWorldPoint(this.client);

        if (this.previousPosition != null)
        {
            int deltaX = currentPosition.getX() - this.previousPosition.getX();
            int deltaY = currentPosition.getY() - this.previousPosition.getY();
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (distance == 0.0) // Stopped moving
            {
                this.resetHistory();
            }

            this.recentDistances.addLast(distance);

            if (this.recentDistances.size() > SAMPLE_SIZE)
            {
                this.recentDistances.removeFirst();                
            }

            this.speed = this.calculateAverageSpeed();
            
        }

        this.previousPosition = currentPosition;
    }

    private double calculateAverageSpeed()
    {
        double sum = 0.0;
        for (Double distance : this.recentDistances)
        {
            sum += distance;
        }

        return sum / this.recentDistances.size();
    }

    private void resetHistory()
    {
        this.recentDistances.clear(); 
        this.speed = 0.0;
    }
}

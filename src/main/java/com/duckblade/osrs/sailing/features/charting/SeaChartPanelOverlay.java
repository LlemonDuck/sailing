package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.SailingPlugin;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.LineComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Dimension;
import java.awt.Graphics2D;

@Singleton
public class SeaChartPanelOverlay
		extends OverlayPanel
		implements PluginLifecycleComponent
{

	private final WeatherTaskTracker weatherTaskTracker;

	@Inject
	public SeaChartPanelOverlay(SailingPlugin plugin, WeatherTaskTracker weatherTaskTracker)
	{
		super(plugin);
		this.weatherTaskTracker = weatherTaskTracker;
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		if (weatherTaskTracker.getActiveTask() != null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
					.left("Weather pattern charting")
					.build());
			panelComponent.getChildren().add(LineComponent.builder().left("").build());

			if (weatherTaskTracker.isTaskComplete())
			{
				panelComponent.getChildren().add(LineComponent.builder()
						.left("Return to the meteorologist marked on your world map.")
						.build());
			}
			else
			{
				panelComponent.getChildren().add(LineComponent.builder()
						.left("Navigate to the marked point on your world map and use the weather device.")
						.build());
			}
		}

		return super.render(graphics);
	}
}

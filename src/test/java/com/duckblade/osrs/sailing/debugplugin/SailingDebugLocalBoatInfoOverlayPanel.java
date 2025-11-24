package com.duckblade.osrs.sailing.debugplugin;

import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Singleton
public class SailingDebugLocalBoatInfoOverlayPanel
	extends OverlayPanel
{

	private final BoatTracker boatTracker;
	private final SailingDebugConfig config;

	@Inject
	public SailingDebugLocalBoatInfoOverlayPanel(BoatTracker boatTracker, SailingDebugConfig config)
	{
		this.boatTracker = boatTracker;
		this.config = config;

		setPreferredPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Boat boat = boatTracker.getBoat();
		if (boat == null)
		{
			return null;
		}

		getPanelComponent().getChildren()
			.add(TitleComponent.builder()
				.text("Local Boat")
				.build());

		getPanelComponent().getChildren()
			.add(LineComponent.builder()
				.left("Size")
				.right(String.valueOf(boat.getSizeClass()))
				.build());

		getPanelComponent().getChildren()
			.add(LineComponent.builder()
				.left("Hull")
				.right(String.valueOf(boat.getHullTier()))
				.build());

		getPanelComponent().getChildren()
			.add(LineComponent.builder()
				.left("Sail")
				.right(String.valueOf(boat.getSailTier()))
				.build());

		getPanelComponent().getChildren()
			.add(LineComponent.builder()
				.left("Helm")
				.right(String.valueOf(boat.getHelmTier()))
				.build());

		getPanelComponent().getChildren()
			.add(LineComponent.builder()
				.left("Hook")
				.right(String.valueOf(boat.getSalvagingHookTier()))
				.build());

		getPanelComponent().getChildren()
			.add(LineComponent.builder()
				.left("Cargo")
				.right(String.valueOf(boat.getCargoHoldTier()))
				.build());

		return super.render(graphics);
	}

}

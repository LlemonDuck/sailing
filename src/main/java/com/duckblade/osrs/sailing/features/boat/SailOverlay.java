package com.duckblade.osrs.sailing.features.boat;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
@Singleton
public class SailOverlay
	extends Overlay
	implements PluginLifecycleComponent
{
	private final Client client;
	private final SailingConfig config;
	private final BoatTracker boatTracker;

	@Inject
	public SailOverlay(
		final Client client,
		final SailingConfig config,
		final BoatTracker boatTracker)
	{
		this.client = client;
		this.config = config;
		this.boatTracker = boatTracker;

		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public boolean isEnabled(final SailingConfig config)
	{
		return config.highlightSail();
	}

	@Override
	public Dimension render(final Graphics2D g)
	{
		if (!SailingUtil.isSailing(client))
		{
			return null;
		}

		final var boat = boatTracker.getBoat();
		if (boat == null)
		{
			return null;
		}

		final var sail = boat.getSail();
		if (sail != null)
		{
			final var convextHull = sail.getConvexHull();
			if (convextHull != null)
			{
				g.setColor(config.colorSailOutline());
				final var originalStroke = g.getStroke();
				g.setStroke(new BasicStroke(1));
				g.draw(convextHull);
				g.setColor(new Color(0, 0, 0, 32));
				g.fill(convextHull);
				g.setStroke(originalStroke);
			}
		}

		return null;
	}
}

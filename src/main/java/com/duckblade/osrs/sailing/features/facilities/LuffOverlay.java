package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Slf4j
@Singleton
public class LuffOverlay
	extends Overlay
	implements PluginLifecycleComponent
{
	private final Client client;
	private final SailingConfig config;
	private final BoatTracker boatTracker;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	public LuffOverlay(
			Client client,
			SailingConfig config,
			BoatTracker boatTracker,
			ModelOutlineRenderer modelOutlineRenderer
	)
	{
		this.client = client;
		this.config = config;
		this.boatTracker = boatTracker;
		this.modelOutlineRenderer = modelOutlineRenderer;

		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.highlightTrimmableSails();
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!SailingUtil.isSailing(client) || !config.highlightTrimmableSails())
		{
			return null;
		}

		Boat boat = boatTracker.getBoat();
		GameObject sail = boat != null ? boat.getSail() : null;

		if (sail == null)
		{
			return null;
		}

		// Core behaviour: only show when the luff action is actually available
		if (!sail.isOpShown(0))
		{
			return null;
		}

		SailingConfig.SailHighlightMode mode = config.sailHighlightMode();

		if (mode == SailingConfig.SailHighlightMode.AREA)
		{
			Shape hull = sail.getConvexHull();
			if (hull != null)
			{
				OverlayUtil.renderPolygon(g, hull, Color.GREEN);
			}
		}
		else if (mode == SailingConfig.SailHighlightMode.SAIL)
		{
			modelOutlineRenderer.drawOutline(sail, 2, Color.GREEN, 250);
		}

		return null;
	}
}

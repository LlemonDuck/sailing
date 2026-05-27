package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
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
	private static final int SAIL_VFX_DISTANCE = 512;

	private static final ImmutableSet<Integer> FULL_WIND_SAIL_VFX_IDS = ImmutableSet.of(
		SpotanimID.VFX_WIND_SAIL_RAFT01_FULL01,
		SpotanimID.VFX_WIND_SAIL_SMALL01_FULL01,
		SpotanimID.VFX_WIND_SAIL_LARGE01_FULL01,
		SpotanimID.VFX_WIND_SAIL_3X10_01
	);

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

		if (!isTrimWindowActive(sail))
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

	private boolean isTrimWindowActive(GameObject sail)
	{
		if (client.getVarbitValue(VarbitID.SAILING_BOAT_TIME_TRIM_WINDOW) > 0 ||
			client.getServerVarbitValue(VarbitID.SAILING_BOAT_TIME_TRIM_WINDOW) > 0)
		{
			return true;
		}

		// The Red Reef update appears to leave this varbit unset while showing the trim window via full-wind sail vfx.
		return hasFullWindSailVfx(sail);
	}

	private boolean hasFullWindSailVfx(GameObject sail)
	{
		LocalPoint sailLocation = sail.getLocalLocation();
		for (GraphicsObject graphicsObject : sail.getWorldView().getGraphicsObjects())
		{
			LocalPoint graphicsLocation = graphicsObject.getLocation();
			if (!FULL_WIND_SAIL_VFX_IDS.contains(graphicsObject.getId()) ||
				graphicsObject.finished() ||
				graphicsLocation == null ||
				sailLocation.distanceTo(graphicsLocation) > SAIL_VFX_DISTANCE)
			{
				continue;
			}

			return true;
		}

		return false;
	}
}

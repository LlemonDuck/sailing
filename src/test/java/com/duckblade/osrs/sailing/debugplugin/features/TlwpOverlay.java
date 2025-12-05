package com.duckblade.osrs.sailing.debugplugin.features;

import com.duckblade.osrs.sailing.debugplugin.SailingDebugConfig;
import com.duckblade.osrs.sailing.debugplugin.module.DebugLifecycleComponent;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class TlwpOverlay
	extends Overlay
	implements DebugLifecycleComponent
{

	private final Client client;
	private final BoatTracker boatTracker;

	private boolean active;

	@Inject
	public TlwpOverlay(Client client, BoatTracker boatTracker)
	{
		this.client = client;
		this.boatTracker = boatTracker;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	public boolean isEnabled(SailingDebugConfig config)
	{
		return config.tlwp();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!active || !SailingUtil.isSailing(client))
		{
			return null;
		}

		LocalPoint tllp = boatTracker.getBoat()
			.getWorldEntity()
			.getLocalLocation();
		Point canvasPoint = Perspective.localToCanvas(client, tllp, 0);
		if (canvasPoint != null)
		{
			graphics.setColor(Color.PINK);
			graphics.drawRect(canvasPoint.getX() - 5, canvasPoint.getY() - 5, 11, 11);
			graphics.drawRect(canvasPoint.getX(), canvasPoint.getY(), 1, 1);
		}

		WorldPoint tlwp = SailingUtil.getTopLevelWorldPoint(client);
		Polygon poly = Perspective.getCanvasTilePoly(client, Objects.requireNonNull(LocalPoint.fromWorld(client, tlwp)));
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, Color.magenta);
		}

		return null;
	}
}

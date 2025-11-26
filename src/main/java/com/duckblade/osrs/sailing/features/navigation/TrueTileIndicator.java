package com.duckblade.osrs.sailing.features.navigation;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldEntityConfig;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class TrueTileIndicator
	extends Overlay
	implements PluginLifecycleComponent
{

	private final Client client;
	private final BoatTracker boatTracker;

	private Color indicatorColor;

	LocalPoint lastPoint;
	float dx;
	float dy;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		indicatorColor = config.navigationTrueTileIndicatorColor();
		return config.navigationTrueTileIndicator();
	}

	@Inject
	public TrueTileIndicator(Client client, BoatTracker boatTracker)
	{
		this.client = client;
		this.boatTracker = boatTracker;

		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!SailingUtil.isSailing(client))
		{
			return null;
		}

		g.setColor(indicatorColor);

		Boat boat = boatTracker.getBoat();
		WorldEntity we = boat.getWorldEntity();

		LocalPoint lp = we.getTargetLocation();
		int angle = we.getTargetOrientation();

		if (!lp.equals(lastPoint))
		{
			if (lastPoint != null)
			{
				dx = (lastPoint.getX() - lp.getX()) / 128.f;
				dy = (lastPoint.getY() - lp.getY()) / 128.f;
			}
			lastPoint = lp;
		}

		renderBoatArea(client, g, we.getConfig(), lp, angle);

		var pt = Perspective.localToCanvas(client, we.getLocalLocation(), 0);
		OverlayUtil.renderTextLocation(g, pt, "(" + dx + ", " + dy + ") d=" + Math.hypot(dx, dy) + " θ=" + angle, indicatorColor);
		return null;
	}

	// public static so it can be used in SailingDebugRouteOverlay
	public static void renderBoatArea(Client client, Graphics2D g, WorldEntityConfig wec, LocalPoint lp, int angle)
	{
		int boatHalfWidth = wec.getBoundsWidth() / 2;
		int boatHalfHeight = wec.getBoundsHeight() / 2;

		float[] localCoordsX = new float[]{
			wec.getBoundsX() + boatHalfWidth,
			wec.getBoundsX() + boatHalfWidth,
			wec.getBoundsX() - boatHalfWidth,
			wec.getBoundsX() - boatHalfWidth
		};

		float[] localCoordsY = new float[]{
			wec.getBoundsY() - boatHalfHeight,
			wec.getBoundsY() + boatHalfHeight,
			wec.getBoundsY() + boatHalfHeight,
			wec.getBoundsY() - boatHalfHeight
		};

		float[] localCoordsZ = new float[]{0, 0, 0, 0};

		int[] canvasXs = new int[4];
		int[] canvasYs = new int[4];

		Perspective.modelToCanvas(
			client,
			client.getTopLevelWorldView(),
			localCoordsX.length, // end
			lp.getX(), // x3dCenter
			lp.getY(), // y3dCenter
			0, // z3dCenter
			angle, // rotate
			localCoordsX, // x3d
			localCoordsY, // y3d
			localCoordsZ, // z3d
			canvasXs, // x2d
			canvasYs // y2d
		);

		Polygon canvasPoly = new Polygon();
		canvasPoly.addPoint(canvasXs[0], canvasYs[0]);
		canvasPoly.addPoint(canvasXs[1], canvasYs[1]);
		canvasPoly.addPoint(canvasXs[2], canvasYs[2]);
		canvasPoly.addPoint(canvasXs[3], canvasYs[3]);
		g.draw(canvasPoly);
	}
}

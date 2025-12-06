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
import net.runelite.api.Constants;
import net.runelite.api.Perspective;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldEntityConfig;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class TrueTileIndicator
	extends Overlay
	implements PluginLifecycleComponent
{

	private final Client client;
	private final BoatTracker boatTracker;

	private SailingConfig.TrueTileMode mode;
	private Color indicatorColor;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		mode = config.navigationTrueTileIndicator();
		indicatorColor = config.navigationTrueTileIndicatorColor();

		return mode != SailingConfig.TrueTileMode.OFF;
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

		if (mode == SailingConfig.TrueTileMode.NAVIGATING
			&& client.getTopLevelWorldView().getYellowClickAction() != Constants.CLICK_ACTION_SET_HEADING)
		{
			return null;
		}

		g.setColor(indicatorColor);

		Boat boat = boatTracker.getBoat();
		WorldEntity we = boat.getWorldEntity();

		renderBoatArea(client, g, we.getConfig(), we.getTargetLocation(), we.getTargetOrientation());

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

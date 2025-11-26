package com.duckblade.osrs.sailing.debugplugin;

import com.duckblade.osrs.sailing.features.navigation.TrueTileIndicator;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Slf4j
@Singleton
public class SailingDebugRouteOverlay
	extends Overlay
	implements PluginLifecycleComponent
{

	private final BoatTracker boatTracker;
	private final Client client;

	private boolean active;

	@Inject
	public SailingDebugRouteOverlay(BoatTracker boatTracker, Client client, SailingDebugConfig config)
	{
		this.boatTracker = boatTracker;
		this.client = client;
		active = config.routeOverlayDefaultOn();

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!active || !SailingUtil.isSailing(client))
		{
			return null;
		}

		Boat boat = boatTracker.getBoat();
		WorldEntity we = boat.getWorldEntity();

		// boat areas
		g.setColor(Color.ORANGE);

		LocalPoint currentLocation = we.getLocalLocation();
		int currentOrientation = we.getOrientation();
		TrueTileIndicator.renderBoatArea(client, g, we.getConfig(), currentLocation, currentOrientation);

		LocalPoint targetLocation = we.getTargetLocation();
		int targetOrientation = we.getTargetOrientation();
		if (!targetLocation.equals(we.getLocalLocation()) || targetOrientation != currentOrientation)
		{
			TrueTileIndicator.renderBoatArea(client, g, we.getConfig(), targetLocation, targetOrientation);
		}

		// boat centre tiles and delta line
		g.setColor(Color.YELLOW);

		Point currentPoint = Perspective.localToCanvas(client, currentLocation, 0);
		if (currentPoint != null)
		{
			g.fillRect(currentPoint.getX() - 3, currentPoint.getY() - 3, 7, 7);
		}

		Point targetPoint = Perspective.localToCanvas(client, targetLocation, 0);
		if (targetPoint != null)
		{
			g.fillRect(targetPoint.getX() - 3, targetPoint.getY() - 3, 7, 7);
		}

		if (currentPoint != null && targetPoint != null)
		{
			g.drawLine(currentPoint.getX(), currentPoint.getY(), targetPoint.getX(), targetPoint.getY());
		}

		return null;
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted e)
	{
		if (e.getCommand().equals("route"))
		{
			active = !active;
		}
	}
}

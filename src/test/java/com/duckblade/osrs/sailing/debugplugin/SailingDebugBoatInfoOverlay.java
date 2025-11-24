package com.duckblade.osrs.sailing.debugplugin;

import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class SailingDebugBoatInfoOverlay extends Overlay
{

	private final Client client;
	private final BoatTracker boatTracker;

	private boolean active;

	@Inject
	public SailingDebugBoatInfoOverlay(Client client, BoatTracker boatTracker, SailingDebugConfig config)
	{
		this.client = client;
		this.boatTracker = boatTracker;
		active = config.boatInfoDefaultOn();

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!active)
		{
			return null;
		}

		for (WorldEntity we : client.getTopLevelWorldView().worldEntities())
		{
			LocalPoint location = we.getLocalLocation();
			if (we.isHiddenForOverlap())
			{
				continue;
			}

			Boat boat = boatTracker.getBoat(we.getWorldView().getId());
			if (boat == null)
			{
				String text = "UNTRACKED BOAT: " + we.getWorldView().getId();
				Point p = Perspective.getCanvasTextLocation(client, graphics, location, text, 0);
				if (p != null)
				{
					OverlayUtil.renderTextLocation(graphics, p, text, Color.YELLOW);
				}
				continue;
			}

			String text = boat.getDebugString();
			Point p = Perspective.getCanvasTextLocation(client, graphics, location, text, 0);
			if (p != null)
			{
				OverlayUtil.renderTextLocation(graphics, p, text, Color.BLUE);
			}
		}

		return null;
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted e)
	{
		if (e.getCommand().equals("boats"))
		{
			active = !active;
		}
	}

}

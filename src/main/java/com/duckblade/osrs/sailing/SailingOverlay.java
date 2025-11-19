package com.duckblade.osrs.sailing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class SailingOverlay extends Overlay
{

	private final Client client;
	private final SailingPlugin plugin;
	private final SailingConfig config;

	@Inject
	public SailingOverlay(Client client, SailingPlugin plugin, SailingConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WorldView wv = client.getLocalPlayer().getWorldView();
		if (wv.isTopLevel())
		{
			return null;
		}

		return null;
	}

	private void highlightObject(Graphics2D graphics, GameObject o)
	{
		if (o != null)
		{
			Shape convexHull = o.getConvexHull();
			if (convexHull != null)
			{
				OverlayUtil.renderPolygon(graphics, convexHull, Color.green);
			}
		}
	}
}

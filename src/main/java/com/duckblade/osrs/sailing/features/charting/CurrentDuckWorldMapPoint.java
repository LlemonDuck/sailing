package com.duckblade.osrs.sailing.features.charting;

import java.awt.image.BufferedImage;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

public class CurrentDuckWorldMapPoint extends WorldMapPoint
{

	public CurrentDuckWorldMapPoint(final WorldPoint worldPoint, BufferedImage icon, String name)
	{
		super(worldPoint, icon);

		setName(name);
		setSnapToEdge(true);
		setJumpOnClick(true);
	}
}

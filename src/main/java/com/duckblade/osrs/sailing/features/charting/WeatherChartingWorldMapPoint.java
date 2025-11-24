package com.duckblade.osrs.sailing.features.charting;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

import java.awt.image.BufferedImage;

public class WeatherChartingWorldMapPoint extends WorldMapPoint
{

	public WeatherChartingWorldMapPoint(final WorldPoint worldPoint, BufferedImage icon, String name)
	{
		super(worldPoint, icon);

		setName(name);
		setSnapToEdge(true);
		setJumpOnClick(true);
	}
}

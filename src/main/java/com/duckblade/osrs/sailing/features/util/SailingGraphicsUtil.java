package com.duckblade.osrs.sailing.features.util;

import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.model.SizeClass;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import static net.runelite.api.Perspective.LOCAL_HALF_TILE_SIZE;
import static net.runelite.api.Perspective.LOCAL_TILE_SIZE;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class SailingGraphicsUtil
{

	// arrow facing right
	// spans about 2 full tiles
	private static final float[] ARROW_X = new float[]{
		0,
		-3 * ((float) LOCAL_TILE_SIZE / 4),
		-3 * ((float) LOCAL_TILE_SIZE / 4),
		0,
		0,
		+3 * ((float) (5 * LOCAL_TILE_SIZE) / 16),
		0,
	};
	private static final float[] ARROW_Y = new float[]{
		+((float) LOCAL_TILE_SIZE / 8), // box top right
		+((float) LOCAL_TILE_SIZE / 8), // box bottom right
		-((float) LOCAL_TILE_SIZE / 8), // box bottom left
		-((float) LOCAL_TILE_SIZE / 8), // box top left
		-((float) (5 * LOCAL_TILE_SIZE) / 16), // head left
		0, // head top
		+((float) (5 * LOCAL_TILE_SIZE) / 16), // head right
	};

	public static int jauBetween(LocalPoint p1, LocalPoint p2)
	{
		int dx = p2.getX() - p1.getX();
		int dy = p2.getY() - p1.getY();
		double radialStep = 2048. / (Math.PI * 2.);
		return ((int) Math.round(Math.atan2(dy, -dx) * radialStep)) & 2047;
	}

	public static float[] translate(float[] src, int offset)
	{
		float[] translated = new float[src.length];
		for (int i = 0; i < src.length; i++)
		{
			translated[i] = src[i] + offset;
		}
		return translated;
	}

	public static void renderBoatArrowTowardPoint(Graphics2D g, Client client, BoatTracker boatTracker, WorldPoint destination, Color color)
	{
		Boat boat = boatTracker.getBoat();

		WorldView tlwv = client.getTopLevelWorldView();
		int baseX = tlwv.getBaseX();
		int baseY = tlwv.getBaseY();

		LocalPoint targetLp = LocalPoint.fromScene(destination.getX() - baseX, destination.getY() - baseY, tlwv); // maybe outside the scene
		LocalPoint boatLp = boat.getWorldEntity().getLocalLocation();

		SizeClass sizeClass = boat.getSizeClass();
		int radius = Math.max(sizeClass.getSizeX(), sizeClass.getSizeY()) + 5;
		float[] arrowX = SailingGraphicsUtil.translate(ARROW_X, radius * LOCAL_HALF_TILE_SIZE); // push the arrow outside the boat
		float[] arrowY = ARROW_Y;

		int[] outXs = new int[arrowX.length];
		int[] outYs = new int[arrowY.length];
		Perspective.modelToCanvas(
			client,
			client.getTopLevelWorldView(),
			arrowX.length,
			boatLp.getX(),
			boatLp.getY(),
			0,
			SailingGraphicsUtil.jauBetween(targetLp, boatLp),
			arrowX,
			arrowY,
			new float[arrowX.length],
			outXs,
			outYs
		);

		g.setColor(color);
		g.fill(new Polygon(outXs, outYs, outXs.length));
	}

}

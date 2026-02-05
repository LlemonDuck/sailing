package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
public class ChartingPathOverlay extends Overlay implements PluginLifecycleComponent
{
	private static final int RECALC_DISTANCE_THRESHOLD = 50;

	private final Client client;
	private final SailingConfig config;
	private final BoatTracker boatTracker;

	private List<SeaChartTask> cachedPath = new ArrayList<>();
	private WorldPoint lastCalcPosition = null;
	private Color pathColor;

	@Inject
	public ChartingPathOverlay(Client client, SailingConfig config, BoatTracker boatTracker)
	{
		this.client = client;
		this.config = config;
		this.boatTracker = boatTracker;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		pathColor = config.chartingPathColor();
		return config.showChartingPath();
	}

	@Override
	public void shutDown()
	{
		cachedPath.clear();
		lastCalcPosition = null;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		// Only show when on a boat
		if (boatTracker.getBoat() == null)
		{
			return null;
		}

		// Use top-level world point to get real world coordinates when on a boat
		WorldPoint playerPos = SailingUtil.getTopLevelWorldPoint(client);
		if (playerPos == null)
		{
			return null;
		}

		// Recalculate path if needed
		boolean needsRecalc = cachedPath.isEmpty()
			|| lastCalcPosition == null
			|| playerPos.distanceTo(lastCalcPosition) > RECALC_DISTANCE_THRESHOLD
			|| (!cachedPath.isEmpty() && cachedPath.get(0).isComplete(client));

		if (needsRecalc)
		{
			recalculatePath(playerPos);
		}

		if (cachedPath.isEmpty())
		{
			return null;
		}

		// Draw line to next task
		SeaChartTask nextTask = cachedPath.get(0);
		WorldPoint targetWorld = nextTask.getLocation();

		// Use top-level local point for consistent coordinate system
		LocalPoint playerLocal = SailingUtil.getTopLevelLocalPoint(client);
		if (playerLocal == null)
		{
			return null;
		}

		Point playerScreen = Perspective.localToCanvas(client, playerLocal, 0);
		if (playerScreen == null)
		{
			return null;
		}

		// Try to get target on screen, otherwise calculate a point in that direction
		LocalPoint targetLocal = LocalPoint.fromWorld(client.getTopLevelWorldView(), targetWorld);

		if (targetLocal == null)
		{
			// Target is off-screen, calculate a LocalPoint in the direction of the target
			int dx = targetWorld.getX() - playerPos.getX();
			int dy = targetWorld.getY() - playerPos.getY();

			// Normalize and extend to edge of scene (use ~50 tiles as max distance)
			double length = Math.sqrt(dx * dx + dy * dy);
			if (length == 0)
			{
				return null;
			}

			int extendDist = 50 * Perspective.LOCAL_TILE_SIZE;
			int targetLocalX = playerLocal.getX() + (int) (dx / length * extendDist);
			int targetLocalY = playerLocal.getY() + (int) (dy / length * extendDist);

			targetLocal = new LocalPoint(targetLocalX, targetLocalY, client.getTopLevelWorldView());
		}

		Point targetScreen = Perspective.localToCanvas(client, targetLocal, 0);

		if (playerScreen != null && targetScreen != null)
		{
			g.setColor(pathColor);
			g.drawLine(playerScreen.getX(), playerScreen.getY(),
				targetScreen.getX(), targetScreen.getY());
		}

		return null;
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged ev)
	{
		if (!cachedPath.isEmpty())
		{
			SeaChartTask firstTask = cachedPath.get(0);
			if (ev.getVarbitId() == firstTask.getCompletionVarb())
			{
				cachedPath.clear();
			}
		}
	}

	private void recalculatePath(WorldPoint startPos)
	{
		List<SeaChartTask> uncompleted = new ArrayList<>();
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (!task.isComplete(client))
			{
				uncompleted.add(task);
			}
		}

		if (uncompleted.isEmpty())
		{
			cachedPath.clear();
			lastCalcPosition = startPos;
			return;
		}

		List<SeaChartTask> path = nearestNeighbor(uncompleted, startPos);
		path = twoOpt(path);

		cachedPath = path;
		lastCalcPosition = startPos;
	}

	private List<SeaChartTask> nearestNeighbor(List<SeaChartTask> tasks, WorldPoint start)
	{
		List<SeaChartTask> remaining = new ArrayList<>(tasks);
		List<SeaChartTask> path = new ArrayList<>();
		WorldPoint current = start;

		while (!remaining.isEmpty())
		{
			SeaChartTask nearest = null;
			int minDist = Integer.MAX_VALUE;

			for (SeaChartTask task : remaining)
			{
				int dist = distance(current, task.getLocation());
				if (dist < minDist)
				{
					minDist = dist;
					nearest = task;
				}
			}

			path.add(nearest);
			current = nearest.getLocation();
			remaining.remove(nearest);
		}

		return path;
	}

	private List<SeaChartTask> twoOpt(List<SeaChartTask> path)
	{
		if (path.size() < 4)
		{
			return path;
		}

		List<SeaChartTask> best = new ArrayList<>(path);
		boolean improved = true;

		while (improved)
		{
			improved = false;
			for (int i = 0; i < best.size() - 2; i++)
			{
				for (int j = i + 2; j < best.size(); j++)
				{
					if (twoOptImproves(best, i, j))
					{
						reverse(best, i + 1, j);
						improved = true;
					}
				}
			}
		}

		return best;
	}

	private boolean twoOptImproves(List<SeaChartTask> path, int i, int j)
	{
		WorldPoint a = path.get(i).getLocation();
		WorldPoint b = path.get(i + 1).getLocation();
		WorldPoint c = path.get(j).getLocation();
		WorldPoint d = (j + 1 < path.size()) ? path.get(j + 1).getLocation() : null;

		int oldDist = distance(a, b);
		int newDist = distance(a, c);

		if (d != null)
		{
			oldDist += distance(c, d);
			newDist += distance(b, d);
		}

		return newDist < oldDist;
	}

	private void reverse(List<SeaChartTask> path, int start, int end)
	{
		while (start < end)
		{
			SeaChartTask temp = path.get(start);
			path.set(start, path.get(end));
			path.set(end, temp);
			start++;
			end--;
		}
	}

	private int distance(WorldPoint a, WorldPoint b)
	{
		int dx = a.getX() - b.getX();
		int dy = a.getY() - b.getY();
		return dx * dx + dy * dy;
	}
}

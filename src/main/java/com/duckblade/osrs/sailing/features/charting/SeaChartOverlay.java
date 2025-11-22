package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;

@Slf4j
@Singleton
public class SeaChartOverlay
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final Color COLOR_CHARTABLE_COMPLETED = ColorUtil.colorWithAlpha(Color.YELLOW, 127);
	private static final Color COLOR_CHARTABLE_INCOMPLETE = Color.GREEN;

	private final Client client;
	private final ItemManager itemManager;
	private final SailingConfig config;
	private final SeaChartTaskIndex taskIndex;
	private final WeatherTaskTracker weatherTaskTracker;

	private final Map<GameObject, SeaChartTask> chartObjects = new HashMap<>();
	private final Map<NPC, SeaChartTask> chartNpcs = new HashMap<>();

	@Inject
	public SeaChartOverlay(Client client, ItemManager itemManager, SailingConfig config, SeaChartTaskIndex taskIndex, WeatherTaskTracker weatherTaskTracker)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.config = config;
		this.taskIndex = taskIndex;
		this.weatherTaskTracker = weatherTaskTracker;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.showCharts() != SailingConfig.ShowChartsMode.NONE;
	}

    public void shutDown()
	{
		chartNpcs.clear();
		chartObjects.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		SailingConfig.ShowChartsMode mode = config.showCharts();
		if (!SailingUtil.isSailing(client) || mode == SailingConfig.ShowChartsMode.NONE)
		{
			return null;
		}

		for (Map.Entry<GameObject, SeaChartTask> tracked : chartObjects.entrySet())
		{
			GameObject obj = tracked.getKey();
			SeaChartTask task = tracked.getValue();

			boolean completed = isTaskCompleted(task);
			if ((completed && mode == SailingConfig.ShowChartsMode.UNCHARTED) ||
				(!completed && mode == SailingConfig.ShowChartsMode.CHARTED))
			{
				continue;
			}

			Polygon poly = obj.getCanvasTilePoly();
			if (poly != null)
			{
				Color color = completed ? COLOR_CHARTABLE_COMPLETED : COLOR_CHARTABLE_INCOMPLETE;
				OverlayUtil.renderPolygon(graphics, poly, color);
			}
			OverlayUtil.renderImageLocation(client, graphics, obj.getLocalLocation(), taskIndex.getTaskSprite(task), 0);
		}

		for (Map.Entry<NPC, SeaChartTask> tracked : chartNpcs.entrySet())
		{
			NPC npc = tracked.getKey();
			SeaChartTask task = tracked.getValue();

			boolean completed = isTaskCompleted(task);
			if (completed && mode == SailingConfig.ShowChartsMode.UNCHARTED)
			{
				continue;
			}

			Color color = isTaskCompleted(task) ? Color.YELLOW : Color.GREEN;
			OverlayUtil.renderActorOverlayImage(graphics, npc, taskIndex.getTaskSprite(task), color, npc.getLogicalHeight() / 2);
		}

		if (weatherTaskTracker.getActiveTask() != null && !weatherTaskTracker.isTaskComplete())
		{
			LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), weatherTaskTracker.getActiveTask().getDstLocation());
			if (lp == null)
			{
				return null;
			}

			Polygon poly = Perspective.getCanvasTilePoly(client, lp, 0);
			if (poly == null)
			{
				return null;
			}

			BufferedImage icon = itemManager.getImage(ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY);
			OverlayUtil.renderPolygon(graphics, poly, Color.GREEN);
			OverlayUtil.renderImageLocation(client, graphics, lp, icon, 0);
		}

		return null;
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();
		SeaChartTask task = taskIndex.findTask(o);
		if (task != null)
		{
			chartObjects.put(o, task);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		chartObjects.remove(e.getGameObject());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		NPC npc = e.getNpc();
		SeaChartTask task = taskIndex.findTask(npc);
		if (task != null)
		{
			chartNpcs.put(npc, task);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		chartNpcs.remove(e.getNpc());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			chartObjects.clear();
			chartNpcs.clear();
		}
	}

	private boolean isTaskCompleted(SeaChartTask task)
	{
		return client.getVarbitValue(task.getCompletionVarb()) != 0;
	}
}

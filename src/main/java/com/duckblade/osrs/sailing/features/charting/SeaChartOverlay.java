package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.SailingConfig;
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
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
@Singleton
public class SeaChartOverlay
	extends Overlay
	implements PluginLifecycleComponent
{

	private final Client client;
	private final ItemManager itemManager;
	private final SailingConfig config;
	private final SeaChartTaskIndex taskIndex;
	private final WeatherTaskTracker weatherTaskTracker;

	private final Map<GameObject, SeaChartTask> chartObjects = new HashMap<>();
	private final Map<NPC, SeaChartTask> chartNpcs = new HashMap<>();

	private Color colorCharted;
	private Color colorUncharted;
	private Color colorRequirementsUnmet;

	@Inject
	public SeaChartOverlay(
		Client client,
		ItemManager itemManager,
		SailingConfig config,
		SeaChartTaskIndex taskIndex,
		WeatherTaskTracker weatherTaskTracker
	)
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
		colorCharted = config.chartingChartedColor();
		colorUncharted = config.chartingUnchartedColor();
		colorRequirementsUnmet = config.chartingRequirementsUnmetColor();
		return config.showCharts() != SailingConfig.ShowChartsMode.NONE;
	}

	public void shutDown()
	{
		chartNpcs.clear();
		chartObjects.clear();
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		SailingConfig.ShowChartsMode mode = config.showCharts();
		if (mode == SailingConfig.ShowChartsMode.NONE)
		{
			return null;
		}

		for (Map.Entry<GameObject, SeaChartTask> tracked : chartObjects.entrySet())
		{
			GameObject obj = tracked.getKey();
			SeaChartTask task = tracked.getValue();

			boolean completed = task.isComplete(client);
			boolean meetsRequirements = hasTaskRequirement(task);
			if (taskIsHidden(mode, completed, meetsRequirements))
			{
				continue;
			}

			Polygon poly = obj.getCanvasTilePoly();
			if (poly != null)
			{
				Color color = getColor(completed, meetsRequirements);
				OverlayUtil.renderPolygon(g, poly, color);
			}
			OverlayUtil.renderImageLocation(client, g, obj.getLocalLocation(), taskIndex.getTaskSprite(task), 0);
		}

		for (Map.Entry<NPC, SeaChartTask> tracked : chartNpcs.entrySet())
		{
			NPC npc = tracked.getKey();
			SeaChartTask task = tracked.getValue();

			boolean completed = task.isComplete(client);
			boolean meetsRequirements = hasTaskRequirement(task);
			if (taskIsHidden(mode, completed, meetsRequirements))
			{
				continue;
			}

			Color color = getColor(completed, meetsRequirements);
			OverlayUtil.renderActorOverlayImage(g, npc, taskIndex.getTaskSprite(task), color, npc.getLogicalHeight() / 2);
		}

		renderWeatherTaskTarget(g);

		return null;
	}

	private void renderWeatherTaskTarget(Graphics2D g)
	{
		if (weatherTaskTracker.getActiveTask() != null && !weatherTaskTracker.isTaskComplete())
		{
			LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), weatherTaskTracker.getActiveTask().getDestination());
			if (lp == null)
			{
				return;
			}

			Polygon poly = Perspective.getCanvasTilePoly(client, lp, 0);
			if (poly == null)
			{
				return;
			}

			BufferedImage icon = itemManager.getImage(ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY);
			OverlayUtil.renderPolygon(g, poly, Color.GREEN);
			OverlayUtil.renderImageLocation(client, g, lp, icon, 0);
		}
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
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		if (e.getWorldView().isTopLevel())
		{
			chartObjects.clear();
		}
	}

	private boolean hasTaskRequirement(SeaChartTask task)
	{
		var questRequirement = taskIndex.getTaskQuestRequirement(task);
		if (questRequirement.getState(client) != QuestState.FINISHED)
		{
			return false;
		}

		return client.getRealSkillLevel(Skill.SAILING) >= task.getLevel();
	}

	private Color getColor(boolean isTaskCompleted, boolean hasTaskRequirement)
	{
		if (isTaskCompleted)
		{
			return colorCharted;
		}

		if (hasTaskRequirement)
		{
			return colorUncharted;
		}

		return colorRequirementsUnmet;
	}

	private boolean taskIsHidden(SailingConfig.ShowChartsMode mode, boolean completed, boolean meetsRequirements)
	{
		switch (mode)
		{
			case ALL:
				return false;

			case CHARTED:
				return !completed;

			case UNCHARTED:
				return completed;

			case REQUIREMENTS_MET:
				return completed || !meetsRequirements;

			default:
				return true;
		}
	}
}

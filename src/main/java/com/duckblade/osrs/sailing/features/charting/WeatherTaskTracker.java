package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.WeatherChartingWorldMapPoint;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;

@Singleton
public class WeatherTaskTracker implements PluginLifecycleComponent
{

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private SeaChartTaskIndex taskIndex;

	@Getter
	private SeaChartTask activeTask;

    @Getter
    private boolean taskComplete;

	private SeaChartTask potentialTask;

	// the active task state as indicated by the ID of the last weather device we had in the inventory
	private int lastState = -1;

	public void shutDown()
	{
		activeTask = null;
		taskComplete = false;
		potentialTask = null;
		lastState = -1;
		worldMapPointManager.removeIf(it -> it instanceof WeatherChartingWorldMapPoint);
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (!SailingUtil.isLocalPlayer(client, event.getSource()))
		{
			return;
		}

		Actor target = event.getTarget();
		if (!(target instanceof NPC))
		{
			return;
		}

		NPC targetNpc = (NPC) target;
		var task = taskIndex.findTask(targetNpc);
		if (task != null)
		{
			potentialTask = task;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		ItemContainer inventory = event.getItemContainer();
		int newState;
		if (inventory.contains(ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY))
		{
			newState = ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY;
		}
		else if (inventory.contains(ItemID.SAILING_CHARTING_WEATHER_STATION_FULL))
		{
			newState = ItemID.SAILING_CHARTING_WEATHER_STATION_FULL;
		}
		else
		{
			newState = -1;
		}

		if (potentialTask != null && lastState == -1 && newState == ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY)
		{
			activeTask = potentialTask;
			potentialTask = null;

			BufferedImage mapIcon = itemManager.getImage(ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY);
			worldMapPointManager.add(new WeatherChartingWorldMapPoint(activeTask.getDstLocation(), mapIcon, "Use device here"));
		}
		else if (activeTask != null && lastState == ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY && newState == ItemID.SAILING_CHARTING_WEATHER_STATION_FULL)
		{
			taskComplete = true;

			BufferedImage mapIcon = itemManager.getImage(ItemID.SAILING_CHARTING_WEATHER_STATION_FULL);
			worldMapPointManager.removeIf(it -> it instanceof WeatherChartingWorldMapPoint);
			worldMapPointManager.add(new WeatherChartingWorldMapPoint(activeTask.getLocation(), mapIcon, "Return device here"));
		}
		else if (activeTask != null && (lastState == ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY || lastState == ItemID.SAILING_CHARTING_WEATHER_STATION_FULL) && newState == -1)
		{
			activeTask = null;
			taskComplete = false;

			worldMapPointManager.removeIf(it -> it instanceof WeatherChartingWorldMapPoint);
		}

		lastState = newState;
	}
}

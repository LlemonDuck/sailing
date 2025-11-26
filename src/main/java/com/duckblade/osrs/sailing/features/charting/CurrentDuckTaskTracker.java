package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingGraphicsUtil;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CurrentDuckTaskTracker
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final String MSG_DUCK_BEGIN = "You release your current duck and he begins tracking the currents...";

	private final Client client;
	private final ChatMessageManager chatMessageManager;
	private final ItemManager itemManager;
	private final WorldMapPointManager worldMapPointManager;
	private final SeaChartTaskIndex taskIndex;
	private final BoatTracker boatTracker;

	private BufferedImage sprite;
	private SeaChartTask activeTask;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.chartingDuckSolver();
	}

	@Override
	public void startUp()
	{
		sprite = itemManager.getImage(ItemID.SAILING_CHARTING_CURRENT_DUCK);
	}

	public void shutDown()
	{
		activeTask = null;
		worldMapPointManager.removeIf(it -> it instanceof CurrentDuckWorldMapPoint);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE || !SailingUtil.isSailing(client))
		{
			return;
		}

		if (event.getMessage().contains(MSG_DUCK_BEGIN))
		{
			WorldPoint playerLoc = SailingUtil.getTopLevelWorldPoint(client);
			SeaChartTask task = taskIndex.findTask(playerLoc, 10, t -> t.getObjectId() == ObjectID.SAILING_CHARTING_HINT_MARKER_DUCK);
			if (task != null)
			{
				log.debug("beginning duck task {}", task);
				setActiveTask(task);
			}
			else
			{
				log.warn("Current duck task began, but no nearby task was found at playerLoc={}", playerLoc);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (activeTask == null)
		{
			return;
		}

		if (activeTask.isComplete(client))
		{
			setActiveTask(null);
		}
	}

	private void setActiveTask(SeaChartTask task)
	{
		activeTask = task;
		if (activeTask != null)
		{
			WorldPoint dest = activeTask.getDestination();
			if (dest == null)
			{
				chatMessageManager.queue(QueuedMessage.builder()
					.value("[Sailing] This duck's end location is not yet supported by the plugin.")
					.build());
				activeTask = null;
				return;
			}

			worldMapPointManager.add(new CurrentDuckWorldMapPoint(dest, sprite, "Current Duck Destination"));
		}
		else
		{
			worldMapPointManager.removeIf(wmp -> wmp instanceof CurrentDuckWorldMapPoint);
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (activeTask == null)
		{
			return null;
		}

		WorldPoint dest = activeTask.getDestination();
		assert dest != null;

		// todo proper height mapping of polygon
		LocalPoint destLp = LocalPoint.fromWorld(client.getTopLevelWorldView(), dest);
		if (destLp != null)
		{
			OverlayUtil.renderTileOverlay(client, graphics, destLp, sprite, Color.GREEN);
		}

		SailingGraphicsUtil.renderBoatArrowTowardPoint(
			graphics,
			client,
			boatTracker,
			dest,
			Color.ORANGE
		);

		return null;
	}
}

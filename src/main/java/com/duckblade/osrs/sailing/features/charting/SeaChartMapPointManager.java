package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableMap;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SeaChartMapPointManager implements PluginLifecycleComponent
{
	private static final BufferedImage MAP_GENERIC = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "generic.png");
	private static final BufferedImage MAP_GENERIC_COMPLETE = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "generic_complete.png");
	private static final BufferedImage MAP_SPYGLASS = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "spyglass.png");
	private static final BufferedImage MAP_SPYGLASS_COMPLETE = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "spyglass_complete.png");
	private static final BufferedImage MAP_CRATE = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "crate.png");
	private static final BufferedImage MAP_CRATE_COMPLETE = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "crate_complete.png");
	private static final BufferedImage MAP_DUCK = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "duck.png");
	private static final BufferedImage MAP_DUCK_COMPLETE = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "duck_complete.png");
	private static final BufferedImage MAP_MERMAID = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "mermaid.png");
	private static final BufferedImage MAP_MERMAID_COMPLETE = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "mermaid_complete.png");
	private static final BufferedImage MAP_WEATHER = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "weather.png");
	private static final BufferedImage MAP_WEATHER_COMPLETE = ImageUtil.loadImageResource(SeaChartMapPointManager.class, "weather_complete.png");

	private final Map<Integer, ChartMapPoint> taskByVarb = Stream.of(SeaChartTask.values())
		.collect(ImmutableMap.toImmutableMap(SeaChartTask::getCompletionVarb, ChartMapPoint::new));

	private class ChartMapPoint extends WorldMapPoint
	{
		SeaChartTask task;
		boolean added = false;

		ChartMapPoint(SeaChartTask task)
		{
			super(WorldMapPoint.builder()
				.image(getIcon(task))
				.worldPoint(task.getLocation())
				.tooltip("Charting spot (" + task.getType().getName() + ")"));
			this.task = task;
		}

		void reconcile()
		{
			boolean show = !mode.isHidden(task.isComplete(client), taskIndex.hasTaskRequirement(task));
			setImage(getIcon(task));
			if (added != show)
			{
				if (show)
				{
					worldMapPointManager.add(this);
				}
				else
				{
					worldMapPointManager.remove(this);
				}

				added = show;
			}
		}
	}

	private final Client client;
	private final ClientThread clientThread;
	private final WorldMapPointManager worldMapPointManager;
	private final SeaChartTaskIndex taskIndex;

	private SailingConfig.ShowChartsMode mode;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		mode = config.showChartsOnMap();
		return mode != SailingConfig.ShowChartsMode.NONE;
	}

	@Override
	public void startUp()
	{
		reconcileAll();
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged ev)
	{
		if (SailingConfig.CONFIG_GROUP.equals(ev.getGroup())
			&& SailingConfig.SHOW_CHARTS_ON_MAP.equals(ev.getKey()))
		{
			this.reconcileAll();
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged ev)
	{
		if (ev.getGameState() == GameState.LOGGED_IN)
		{
			reconcileAll();
		}
	}

	@Subscribe
	private void onVarbitChanged(VarbitChanged ev)
	{
		if (ev.getVarpId() == VarPlayerID.QP)
		{
			// a quest state changed
			reconcileAll();
		}

		var t = taskByVarb.get(ev.getVarbitId());
		if (t != null)
		{
			t.reconcile();
		}
	}

	@Override
	public void shutDown()
	{
		clientThread.invokeLater(() ->
		{
			worldMapPointManager.removeIf(p ->
			{
				if (p instanceof ChartMapPoint)
				{
					((ChartMapPoint) p).added = false;
					return true;
				}

				return false;
			});
		});
	}

	private void reconcileAll()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		clientThread.invoke(() ->
		{
			for (var e : taskByVarb.values())
			{
				e.reconcile();
			}
		});
	}

	private BufferedImage getIcon(SeaChartTask task)
	{
		boolean completed = client != null && task.isComplete(client);
		switch (task.getType())
		{
			case SPYGLASS:
				return completed ? MAP_SPYGLASS_COMPLETE : MAP_SPYGLASS;

			case DRINK_CRATE:
				return completed ? MAP_CRATE_COMPLETE : MAP_CRATE;

			case CURRENT_DUCK:
				return completed ? MAP_DUCK_COMPLETE : MAP_DUCK;

			case MERMAID_GUIDE:
				return completed ? MAP_MERMAID_COMPLETE : MAP_MERMAID;

			case WEATHER:
				return completed ? MAP_WEATHER_COMPLETE : MAP_WEATHER;

			case GENERIC:
			default:
				return completed ? MAP_GENERIC_COMPLETE : MAP_GENERIC;
		}
	}
}

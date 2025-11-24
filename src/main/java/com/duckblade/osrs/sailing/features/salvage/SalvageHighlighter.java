package com.duckblade.osrs.sailing.features.salvage;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import static net.runelite.api.Skill.SAILING;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class SalvageHighlighter
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final Map<Integer, Integer> SALVAGE_LEVEL_REQ = Map.of(
		ObjectID.SAILING_SMALL_SHIPWRECK, 15,
		ObjectID.SAILING_FISHERMAN_SHIPWRECK, 26,
		ObjectID.SAILING_BARRACUDA_SHIPWRECK, 35,
		ObjectID.SAILING_LARGE_SHIPWRECK, 53,
		ObjectID.SAILING_PIRATE_SHIPWRECK, 64,
		ObjectID.SAILING_MERCENARY_SHIPWRECK, 73,
		ObjectID.SAILING_FREMENNIK_SHIPWRECK_STUMP, 80,
		ObjectID.SAILING_MERCHANT_SHIPWRECK, 87
	);

	private static final List<Integer> SALVAGE_STUMP = ImmutableList.of(
		ObjectID.SAILING_SMALL_SHIPWRECK_STUMP,
		ObjectID.SAILING_FISHERMAN_SHIPWRECK_STUMP,
		ObjectID.SAILING_BARRACUDA_SHIPWRECK_STUMP,
		ObjectID.SAILING_LARGE_SHIPWRECK_STUMP,
		ObjectID.SAILING_PIRATE_SHIPWRECK_STUMP,
		ObjectID.SAILING_MERCENARY_SHIPWRECK_STUMP,
		ObjectID.SAILING_FREMENNIK_SHIPWRECK_STUMP,
		ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP
	);

	private final Client client;
	private final SailingConfig config;

	private final Set<GameObject> salvage = new HashSet<>();
	private final Set<GameObject> salvageStump = new HashSet<>();
	private int sailingLevel = 0;
	private Color salvageHighlightColor;
	private Color salvageHighlightSunkColor;
	private Color salvageHighlightNotLevelColor;

	@Inject
	public SalvageHighlighter(Client client, SailingConfig config)
	{
		this.client = client;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}


	@Override
	public void startUp()
	{

		sailingLevel = client.getRealSkillLevel(SAILING);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		salvageHighlightColor = config.salvagingHighlightColor();
		salvageHighlightSunkColor = config.salvagingHighlightSunkColor();
		salvageHighlightNotLevelColor = config.salvagingHighlightNotLevelColor();
		return config.salvagingHighlight();
	}

	@Override
	public void shutDown()
	{
		salvage.clear();
		salvageStump.clear();
		sailingLevel = client.getRealSkillLevel(SAILING);
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		if (statChanged.getSkill() == SAILING)
		{
			int mewSailingLevel = statChanged.getBoostedLevel();
			if (mewSailingLevel != sailingLevel)
			{
				sailingLevel = mewSailingLevel;
			}
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();
		if (SALVAGE_LEVEL_REQ.containsKey(o.getId()))
		{
			salvage.add(o);
		}
		else if (SALVAGE_STUMP.contains(o.getId()))
		{
			salvageStump.add(o);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		salvage.remove(e.getGameObject());
		salvageStump.remove(e.getGameObject());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOADING)
		{
			salvage.clear();
			salvageStump.clear();
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!SailingUtil.isSailing(client) || !config.salvagingHighlight())
		{
			return null;
		}

		for (GameObject salvage : salvage)
		{
			OverlayUtil.renderTileOverlay(graphics, salvage, "", getHighlightColor(salvage));
		}

		for (GameObject salvage : salvageStump)
		{
			OverlayUtil.renderTileOverlay(graphics, salvage, "", salvageHighlightSunkColor);
		}

		return null;
	}

	private Color getHighlightColor(GameObject salvage)
	{
		Integer levelReq = SALVAGE_LEVEL_REQ.get(salvage.getId());
		if (levelReq <= sailingLevel)
		{
			return salvageHighlightColor;
		}

		return salvageHighlightNotLevelColor;
	}
}

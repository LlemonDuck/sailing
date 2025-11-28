package com.duckblade.osrs.sailing.features.salvaging;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableMap;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Skill;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
@Singleton
public class SalvagingHighlight
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final int SIZE_SALVAGEABLE_AREA = 15;

	private static final Map<Integer, Integer> SALVAGE_LEVEL_REQ = ImmutableMap.<Integer, Integer>builder()
		.put(ObjectID.SAILING_SMALL_SHIPWRECK, 15)
		.put(ObjectID.SAILING_FISHERMAN_SHIPWRECK, 26)
		.put(ObjectID.SAILING_BARRACUDA_SHIPWRECK, 35)
		.put(ObjectID.SAILING_LARGE_SHIPWRECK, 53)
		.put(ObjectID.SAILING_PIRATE_SHIPWRECK, 64)
		.put(ObjectID.SAILING_MERCENARY_SHIPWRECK, 73)
		.put(ObjectID.SAILING_FREMENNIK_SHIPWRECK, 80)
		.put(ObjectID.SAILING_MERCHANT_SHIPWRECK, 87)
		.build();

	private static final Map<Integer, Integer> STUMP_LEVEL_REQ = ImmutableMap.<Integer, Integer>builder()
		.put(ObjectID.SAILING_SMALL_SHIPWRECK_STUMP, 15)
		.put(ObjectID.SAILING_FISHERMAN_SHIPWRECK_STUMP, 26)
		.put(ObjectID.SAILING_BARRACUDA_SHIPWRECK_STUMP, 35)
		.put(ObjectID.SAILING_LARGE_SHIPWRECK_STUMP, 53)
		.put(ObjectID.SAILING_PIRATE_SHIPWRECK_STUMP, 64)
		.put(ObjectID.SAILING_MERCENARY_SHIPWRECK_STUMP, 73)
		.put(ObjectID.SAILING_FREMENNIK_SHIPWRECK_STUMP, 80)
		.put(ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, 87)
		.build();

	private final Client client;

	private final Set<GameObject> wrecks = new HashSet<>();
	private final Set<GameObject> stumps = new HashSet<>();

	private boolean activeWrecks;
	private Color activeColour;
	private boolean inactiveWrecks;
	private Color inactiveColour;
	private boolean highLevelWrecks;
	private Color highLevelColour;

	@Inject
	public SalvagingHighlight(Client client)
	{
		this.client = client;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		activeWrecks = config.salvagingHighlightActiveWrecks();
		activeColour = config.salvagingHighlightActiveWrecksColour();
		inactiveWrecks = config.salvagingHighlightInactiveWrecks();
		inactiveColour = config.salvagingHighlightInactiveWrecksColour();
		highLevelWrecks = config.salvagingHighlightHighLevelWrecks();
		highLevelColour = config.salvagingHighLevelWrecksColour();

		return activeWrecks || inactiveWrecks || highLevelWrecks;
	}

	@Override
	public void shutDown()
	{
		wrecks.clear();
		stumps.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int sailingLevel = client.getBoostedSkillLevel(Skill.SAILING);

		for (GameObject wreck : wrecks)
		{
			boolean hasReq = sailingLevel >= SALVAGE_LEVEL_REQ.get(wreck.getId());
			if ((hasReq && activeWrecks) || (!hasReq && highLevelWrecks))
			{
				renderWreck(graphics, wreck, hasReq ? activeColour : highLevelColour);
			}
		}
		for (GameObject wreck : stumps)
		{
			boolean hasReq = sailingLevel >= STUMP_LEVEL_REQ.get(wreck.getId());
			if ((hasReq && inactiveWrecks) || (!hasReq && highLevelWrecks))
			{
				renderWreck(graphics, wreck, hasReq ? inactiveColour : highLevelColour);
			}
		}

		return null;
	}

	private void renderWreck(Graphics2D graphics, GameObject wreck, Color colour)
	{
		Polygon poly = Perspective.getCanvasTileAreaPoly(client, wreck.getLocalLocation(), SIZE_SALVAGEABLE_AREA);
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, colour);
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		if (SALVAGE_LEVEL_REQ.containsKey(e.getGameObject().getId()))
		{
			wrecks.add(e.getGameObject());
		}
		else if (STUMP_LEVEL_REQ.containsKey(e.getGameObject().getId()))
		{
			stumps.add(e.getGameObject());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		wrecks.remove(e.getGameObject());
		stumps.remove(e.getGameObject());
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		if (e.getWorldView().isTopLevel())
		{
			wrecks.clear();
			stumps.clear();
		}
	}
}

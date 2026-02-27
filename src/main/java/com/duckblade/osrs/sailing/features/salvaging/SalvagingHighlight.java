package com.duckblade.osrs.sailing.features.salvaging;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.duckblade.osrs.sailing.features.salvaging.Wreck.WreckType;
import com.google.common.collect.ImmutableMap;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.BasicStroke;
import java.util.HashSet;
import java.util.HashMap;
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

	private static final Map<Integer, Wreck> WRECK_DEF_BY_ID =
			ImmutableMap.<Integer, Wreck>builder()
					.put(ObjectID.SAILING_SMALL_SHIPWRECK, new Wreck(WreckType.SALVAGE, 15))
					.put(ObjectID.SAILING_SMALL_SHIPWRECK_STUMP, new Wreck(WreckType.STUMP, 15))
					.put(ObjectID.SAILING_FISHERMAN_SHIPWRECK, new Wreck(WreckType.SALVAGE, 26))
					.put(ObjectID.SAILING_FISHERMAN_SHIPWRECK_STUMP, new Wreck(WreckType.STUMP, 26))
					.put(ObjectID.SAILING_BARRACUDA_SHIPWRECK, new Wreck(WreckType.SALVAGE, 35))
					.put(ObjectID.SAILING_BARRACUDA_SHIPWRECK_STUMP, new Wreck(WreckType.STUMP, 35))
					.put(ObjectID.SAILING_LARGE_SHIPWRECK, new Wreck(WreckType.SALVAGE, 53))
					.put(ObjectID.SAILING_LARGE_SHIPWRECK_STUMP, new Wreck(WreckType.STUMP, 53))
					.put(ObjectID.SAILING_PIRATE_SHIPWRECK, new Wreck(WreckType.SALVAGE, 64))
					.put(ObjectID.SAILING_PIRATE_SHIPWRECK_STUMP, new Wreck(WreckType.STUMP, 64))
					.put(ObjectID.SAILING_MERCENARY_SHIPWRECK, new Wreck(WreckType.SALVAGE, 73))
					.put(ObjectID.SAILING_MERCENARY_SHIPWRECK_STUMP, new Wreck(WreckType.STUMP, 73))
					.put(ObjectID.SAILING_FREMENNIK_SHIPWRECK, new Wreck(WreckType.SALVAGE, 80))
					.put(ObjectID.SAILING_FREMENNIK_SHIPWRECK_STUMP, new Wreck(WreckType.STUMP, 80))
					.put(ObjectID.SAILING_MERCHANT_SHIPWRECK, new Wreck(WreckType.SALVAGE, 87))
					.put(ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP, new Wreck(WreckType.STUMP, 87))
					.build();

	private final Client client;

	private final Map<GameObject, WreckType> wreckObjects = new HashMap<>();
	private boolean showActiveWrecks;
	private Color activeColour;
	private int activeOpacity;
	private boolean showInctiveWrecks;
	private Color inactiveColour;
	private int inactiveOpacity;
	private boolean showHighLevelWrecks;
	private Color highLevelColour;
	private int highLevelOpacity;

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
		showActiveWrecks = config.salvagingHighlightActiveWrecks();
		activeColour = config.salvagingHighlightActiveWrecksColour();
		activeOpacity = config.salvagingHighlightActiveWrecksOpacity();
		showInctiveWrecks = config.salvagingHighlightInactiveWrecks();
		inactiveColour = config.salvagingHighlightInactiveWrecksColour();
		inactiveOpacity = config.salvagingHighlightInactiveWrecksOpacity();
		showHighLevelWrecks = config.salvagingHighlightHighLevelWrecks();
		highLevelColour = config.salvagingHighLevelWrecksColour();
		highLevelOpacity = config.salvagingHighlightHighLevelWrecksOpacity();

		return showActiveWrecks || showInctiveWrecks || showHighLevelWrecks;
	}

	@Override
	public void shutDown()
	{
		wreckObjects.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!SailingUtil.isSailing(client))
		{
			return null;
		}

		int sailingLevel = client.getBoostedSkillLevel(Skill.SAILING);

		for (Map.Entry<GameObject, WreckType> wreck : wreckObjects.entrySet())
		{
			GameObject wreckObj = wreck.getKey();
			WreckType type = wreck.getValue();
			int levelReq = WRECK_DEF_BY_ID.get(wreckObj.getId()).getLevelReq();
			boolean hasReq = sailingLevel >= levelReq;
			if (hasReq && showActiveWrecks && type == WreckType.SALVAGE)
			{
				renderWreck(graphics, wreckObj, activeColour, activeOpacity);
			}
			else if(hasReq && showInctiveWrecks && type == WreckType.STUMP)
			{
				renderWreck(graphics, wreckObj, inactiveColour, inactiveOpacity);
			}
			else if(!hasReq && showHighLevelWrecks)
			{
				renderWreck(graphics, wreckObj, highLevelColour, highLevelOpacity);
			}
		}

		return null;
	}

	private void renderWreck(Graphics2D graphics, GameObject wreck, Color colour, int opacity)
	{
		Polygon poly = Perspective.getCanvasTileAreaPoly(client, wreck.getLocalLocation(), SIZE_SALVAGEABLE_AREA);
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, colour, new Color(0, 0, 0, opacity), new BasicStroke((float) 1));
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		if (WRECK_DEF_BY_ID.containsKey(e.getGameObject().getId()))
		{
			WreckType type = WRECK_DEF_BY_ID.get(e.getGameObject().getId()).getType();
			wreckObjects.put(e.getGameObject(), type);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		wreckObjects.remove(e.getGameObject(), WreckType.SALVAGE);
		wreckObjects.remove(e.getGameObject(), WreckType.STUMP);
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		if (e.getWorldView().isTopLevel())
		{
			wreckObjects.clear();
		}
	}
}

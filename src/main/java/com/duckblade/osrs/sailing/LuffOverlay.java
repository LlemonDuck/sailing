package com.duckblade.osrs.sailing;

import com.duckblade.osrs.sailing.util.SailingUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class LuffOverlay extends Overlay
{

	private static final String CHAT_LUFF_AVAILABILE = "You feel a gust of wind.";
	private static final String CHAT_LUFF_PERFORMED = "You trim the sails, catching the wind for a burst of speed!";
	private static final String CHAT_LUFF_ENDED = "The wind dies down and your sails with it.";

	private final Client client;
	private final SailingConfig config;

	private final Map<Integer, GameObject> sails = new HashMap<>();

	private boolean needLuff = false;

	@Inject
	public LuffOverlay(Client client, SailingConfig config)
	{
		this.client = client;
		this.config = config;

		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	void shutDown()
	{
		sails.clear();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();
		if (SailingUtil.SAIL_IDS.contains(o.getId()))
		{
			sails.put(o.getWorldView().getId(), o);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		sails.remove(e.getGameObject().getWorldView().getId());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOADING)
		{
			sails.clear();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (!SailingUtil.isSailing(client))
		{
			return;
		}

		String msg = e.getMessage();
		if (CHAT_LUFF_AVAILABILE.equals(msg))
		{
			needLuff = true;
		}
		else if (CHAT_LUFF_PERFORMED.equals(msg) || CHAT_LUFF_ENDED.equals(msg))
		{
			needLuff = false;
		}
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!needLuff || !SailingUtil.isSailing(client) || !config.highlightTrimmableSails())
		{
			return null;
		}

		GameObject o = sails.get(client.getLocalPlayer().getWorldView().getId());
		Shape hull = o != null ? o.getConvexHull() : null;
		if (hull != null)
		{
			OverlayUtil.renderPolygon(g, hull, Color.green);
		}

		return null;
	}
}

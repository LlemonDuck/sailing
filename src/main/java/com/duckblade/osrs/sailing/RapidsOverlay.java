package com.duckblade.osrs.sailing;

import com.duckblade.osrs.sailing.util.SailingUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class RapidsOverlay extends Overlay
{

	private final Client client;
	private final SailingConfig config;

	private final Set<GameObject> rapids = new HashSet<>();

	@Inject
	public RapidsOverlay(Client client, SailingConfig config)
	{
		this.client = client;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	void shutDown()
	{
		rapids.clear();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();
		if (SailingUtil.RAPIDS_IDS.contains(o.getId()))
		{
			rapids.add(o);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		rapids.remove(e.getGameObject());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOADING)
		{
			rapids.clear();
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!SailingUtil.isSailing(client) || !config.highlightRapids())
		{
			return null;
		}

		for (GameObject rapid : rapids)
		{
			OverlayUtil.renderTileOverlay(graphics, rapid, "", Color.cyan);
		}

		return null;
	}
}

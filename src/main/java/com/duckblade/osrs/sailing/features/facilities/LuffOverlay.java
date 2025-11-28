package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Arrays;
import java.util.HashSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import java.util.Set;

@Slf4j
@Singleton
public class LuffOverlay
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final String CHAT_LUFF_AVAILABILE = "You feel a gust of wind.";
	private static final String CHAT_LUFF_PERFORMED = "You trim the sails, catching the wind for a burst of speed!";
	private static final String CHAT_LUFF_STORED = "You trim the sails, catching the wind and storing a wind mote in your helm.";
	private static final String CHAT_LUFF_ENDED = "The wind dies down and your sails with it.";
	private static final String CHAT_MOTE_PERFORMED = "You release the wind mote for a burst of speed!";

	private static final Set<String> CHAT_LUFF_CLEAR_MESSAGES = new HashSet<>(Arrays.asList(
			CHAT_LUFF_PERFORMED,
			CHAT_LUFF_STORED,
			CHAT_LUFF_ENDED,
			CHAT_MOTE_PERFORMED
	));

	private final Client client;
	private final SailingConfig config;
	private final BoatTracker boatTracker;

	private boolean needLuff = false;

	@Inject
	public LuffOverlay(Client client, SailingConfig config, BoatTracker boatTracker)
	{
		this.client = client;
		this.config = config;
		this.boatTracker = boatTracker;

		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.highlightTrimmableSails();
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		if (!e.getWorldView().isTopLevel() &&
			e.getWorldView() == client.getLocalPlayer().getWorldView())
		{
			needLuff = false;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (!SailingUtil.isSailing(client) ||
			(e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM))
		{
			return;
		}

		String msg = e.getMessage();
		if (CHAT_LUFF_AVAILABILE.equals(msg))
		{
			needLuff = true;
		}
		else if (CHAT_LUFF_CLEAR_MESSAGES.contains(msg))
		{
			needLuff = false;
		}
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (needLuff && !SailingUtil.isSailing(client))
		{
			needLuff = false;
		}

		if (!needLuff || !SailingUtil.isSailing(client) || !config.highlightTrimmableSails())
		{
			return null;
		}

		Boat boat = boatTracker.getBoat();
		GameObject sail = boat != null ? boat.getSail() : null;
		Shape convextHull = sail != null ? sail.getConvexHull() : null;
		if (convextHull != null)
		{
			OverlayUtil.renderPolygon(g, convextHull, Color.green);
		}

		return null;
	}
}

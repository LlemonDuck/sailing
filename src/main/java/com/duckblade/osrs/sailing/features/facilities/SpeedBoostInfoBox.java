package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.SailingPlugin;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;

@Singleton
public class SpeedBoostInfoBox
	extends InfoBox
	implements PluginLifecycleComponent
{

	private static final int ICON_ID_LUFF = 7075;

	private static final String CHAT_LUFF_SAIL = "You trim the sails, catching the wind for a burst of speed!";
	private static final String CHAT_LUFF_STORED = "You release the wind mote for a burst of speed!";

	private final Client client;
	private final BoatTracker boatTracker;

	private int speedBoostDuration;

	@Inject
	public SpeedBoostInfoBox(SailingPlugin plugin, Client client, SpriteManager spriteManager, BoatTracker boatTracker)
	{
		super(null, plugin);
		spriteManager.getSpriteAsync(ICON_ID_LUFF, 0, this);

		this.client = client;
		this.boatTracker = boatTracker;
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.showSpeedBoostInfoBox();
	}

	@Override
	public void shutDown()
	{
		speedBoostDuration = 0;
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE || !SailingUtil.isSailing(client))
		{
			return;
		}

		String msg = e.getMessage();
		if (CHAT_LUFF_SAIL.equals(msg) || CHAT_LUFF_STORED.equals(msg))
		{
			// offset by 1, onGameTick fires _after_ onChatMessage
			speedBoostDuration = boatTracker.getBoat().getSpeedBoostDuration() + 1;
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (speedBoostDuration > 0)
		{
			--speedBoostDuration;
		}
	}

	@Override
	public boolean render()
	{
		return speedBoostDuration > 0;
	}

	@Override
	public String getText()
	{
		return Integer.toString(speedBoostDuration);
	}

	@Override
	public Color getTextColor()
	{
		return Color.GREEN;
	}
}

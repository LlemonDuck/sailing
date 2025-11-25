package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.SailingPlugin;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.ImageUtil;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.image.BufferedImage;

@Singleton
public class SpeedBoostInfoBox
	extends InfoBox
	implements PluginLifecycleComponent
{

	private static final BufferedImage ICON_LUFF = ImageUtil.loadImageResource(SpeedBoostInfoBox.class, "speed-boost-info-box.png");

	private static final String CHAT_LUFF_SAIL = "You trim the sails, catching the wind for a burst of speed!";
	private static final String CHAT_LUFF_STORED = "You release the wind mote for a burst of speed!";

	private final SailingConfig config;
	private final Client client;
	private final BoatTracker boatTracker;

	private int speedBoostDuration;

	@Inject
	public SpeedBoostInfoBox(SailingPlugin plugin, SailingConfig config, Client client, BoatTracker boatTracker)
	{
		super(ICON_LUFF, plugin);
		this.config = config;
		this.client = client;
		this.boatTracker = boatTracker;
	}

	@Override
	public void shutDown()
	{
		speedBoostDuration = 0;
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (!SailingUtil.isSailing(client))
		{
			return;
		}

		String msg = e.getMessage();
		if (CHAT_LUFF_SAIL.equals(msg) || CHAT_LUFF_STORED.equals(msg))
		{
			// offset by 1, onGameTick runs _after_ onChatMessage
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
		return config.showSpeedBoostInfoBox() && speedBoostDuration > 0;
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

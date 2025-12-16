package com.duckblade.osrs.sailing.features.navigation;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.config.Notification;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LowHPNotification implements PluginLifecycleComponent
{

	private final Client client;
	private final Notifier notifier;

	private Notification notification;
	private int threshold;

	private boolean hasNotified = false;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		notification = config.lowBoatHPNotification();
		threshold = config.lowBoatHPThreshold();
		return notification.isEnabled();
	}

	@Override
	public void shutDown()
	{
		hasNotified = false;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (!SailingUtil.isSailing(client))
		{
			hasNotified = false;
			return;
		}

		int currentHP = getBoatHP();

		if (currentHP < 0)
		{
			hasNotified = false;
			return;
		}

		if (currentHP < threshold)
		{
			if (!hasNotified)
			{
				notifier.notify(notification, "Your boat's hitpoints are low!");
				hasNotified = true;
			}
		}
		else
		{
			hasNotified = false;
		}
	}

	private int getBoatHP()
	{
		int hp = client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_BOAT_HP);
		if (hp >= 0)
		{
			return hp;
		}

		return -1;
	}
}

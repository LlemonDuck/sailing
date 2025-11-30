package com.duckblade.osrs.sailing.features.shipcombat;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LowHPNotification implements PluginLifecycleComponent
{
	private final SailingConfig config;
	private final Client client;
	private final BoatTracker boatTracker;
	private final Notifier notifier;

	private boolean hasNotified = false;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.notifyLowBoatHP().isEnabled();
	}

	@Override
	public void shutDown()
	{
		hasNotified = false;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (!SailingUtil.isSailing(client) || boatTracker.getBoat() == null)
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

		int threshold = config.lowBoatHPThreshold();

		if (currentHP < threshold)
		{
			if (!hasNotified)
			{
				notifier.notify(config.notifyLowBoatHP(), "Your boat's hitpoints are low!");
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

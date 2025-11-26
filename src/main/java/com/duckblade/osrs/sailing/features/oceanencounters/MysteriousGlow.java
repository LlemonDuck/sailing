package com.duckblade.osrs.sailing.features.oceanencounters;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.Notifier;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MysteriousGlow implements PluginLifecycleComponent
{
	private final SailingConfig config;

	private final Notifier notifier;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.notifyMysteriousGlowSpawn().isEnabled();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		if (e.getNpc().getId() == NpcID.SAILING_CHANCE_ENCOUNTERS_GLOW)
		{
			notifier.notify(config.notifyMysteriousGlowSpawn(), "A mysterious glow has spawned.");
		}
	}
}

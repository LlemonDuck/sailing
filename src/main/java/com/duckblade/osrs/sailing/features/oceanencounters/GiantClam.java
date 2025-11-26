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
public class GiantClam implements PluginLifecycleComponent
{
	private final SailingConfig config;

	private final Notifier notifier;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.notifyGiantClamSpawn().isEnabled();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		switch (e.getNpc().getId())
		{
			case NpcID.SAILING_CHANCE_ENCOUNTERS_CLAM_OPEN:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_CLAM_PEARL:
				notifier.notify(config.notifyGiantClamSpawn(), "A giant clam has spawned.");
				break;
		}
	}
}

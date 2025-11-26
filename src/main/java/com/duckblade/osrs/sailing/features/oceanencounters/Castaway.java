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
public class Castaway implements PluginLifecycleComponent
{
	private final SailingConfig config;

	private final Notifier notifier;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.notifyCastawaySpawn().isEnabled();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		switch (e.getNpc().getId())
		{
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_MAN1_ADRIFT:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_MAN1_PASSENGER:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_MAN2_ADRIFT:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_MAN2_PASSENGER:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_WOMAN1_ADRIFT:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_WOMAN1_PASSENGER:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_WOMAN2_ADRIFT:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_WOMAN2_PASSENGER:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_WOMAN3_ADRIFT:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_WOMAN3_PASSENGER:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_WILSON_ADRIFT:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_WILSON_PASSENGER:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_DOG1_ADRIFT:
			case NpcID.SAILING_CHANCE_ENCOUNTERS_RESCUE_DOG1_PASSENGER:
				notifier.notify(config.notifyCastawaySpawn(), "A castaway has spawned.");
				break;
		}
	}
}

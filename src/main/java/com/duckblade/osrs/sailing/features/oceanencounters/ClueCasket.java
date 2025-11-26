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
public class ClueCasket implements PluginLifecycleComponent
{
	private final SailingConfig config;

	private final Notifier notifier;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.notifyClueCasketSpawn().isEnabled();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		switch (e.getNpc().getId())
		{
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_CASKET_BEGINNER:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_CASKET_EASY:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_CASKET_MEDIUM:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_CASKET_HARD:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_CASKET_ELITE:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_CASKET_MASTER:
				notifier.notify(config.notifyClueCasketSpawn(), "A clue casket has spawned.");
				break;
		}
	}
}

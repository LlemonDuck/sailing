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
public class LostShipment implements PluginLifecycleComponent
{
	private final SailingConfig config;

	private final Notifier notifier;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.notifyLostShipmentSpawn().isEnabled();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		switch (e.getNpc().getId())
		{
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_WOOD:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_OAK:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_TEAK:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_MAHOGANY:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_CAMPHOR:
			case NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_IRONWOOD:
				notifier.notify(config.notifyLostShipmentSpawn(), "A lost shipment has spawned.");
				break;
		}
	}
}

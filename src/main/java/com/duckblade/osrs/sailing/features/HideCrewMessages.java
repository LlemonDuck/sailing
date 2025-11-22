package com.duckblade.osrs.sailing.features;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Singleton;
import java.util.List;

@Slf4j
@Singleton
public class HideCrewMessages
	implements PluginLifecycleComponent {

	private static final List<Integer> NPC_IDS = List.of(
			// Jobless Jim
			NpcID.SAILING_CREW_GENERIC_1_SHIP_OP,
			// Ex-Captain Siad
			NpcID.SAILING_CREW_CAPTAIN_SIAD_SHIP_OP,
			// Adventurer Ada
			NpcID.SAILING_CREW_GENERIC_2_SHIP_OP,
			// Cabin Boy Jenkins
			NpcID.SAILING_CREW_GHOST_JENKINS_SHIP_OP,
			// Oarswoman Olga
			NpcID.SAILING_CREW_WEREWOLF_SHIP_OP,
			// Jittery Jim
			NpcID.SAILING_CREW_GENERIC_3_SHIP_OP,
			// Bosun Zarah
			NpcID.SAILING_CREW_FREMENNIK_SHIP_OP,
			// Jolly Jim
			NpcID.SAILING_CREW_GENERIC_4_SHIP_OP,
			// Spotter Virginia
			NpcID.SAILING_CREW_SPIRIT_ANGLER_SHIP_OP,
			//Sailor Jakob
			NpcID.SAILING_CREW_GENERIC_5_SHIP_OP
	);
	@Override
	public boolean isEnabled(SailingConfig config) {
		return config.hideCrewOverheadMessages();
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event) {
		Actor actor = event.getActor();
		if (actor instanceof NPC) {
			NPC npc = (NPC) actor;
			if (NPC_IDS.contains(npc.getId())) {
				npc.setOverheadText(null);
			}
		}
	}
}
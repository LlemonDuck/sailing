package com.duckblade.osrs.sailing.features.util;

import com.duckblade.osrs.sailing.model.Crewmate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;
import net.runelite.api.gameval.DBTableID;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CrewmateIndex
{

	private final Client client;

	private final Map<Integer, Crewmate> crewmates = new HashMap<>();

	public Crewmate getCrewmate(int uniqueId)
	{
		if (uniqueId == 0)
		{
			return null;
		}

		return crewmates.computeIfAbsent(uniqueId, this::load);
	}

	private Crewmate load(int uniqueId)
	{
		List<Integer> rows = client.getDBRowsByValue(DBTableID.SailingCrew.ID, DBTableID.SailingCrew.COL_UNIQUE_ID, 0, uniqueId);
		if (rows.isEmpty())
		{
			log.warn("no crewmate found for uniqueId {}", uniqueId);
			return null;
		}

		int rowID = rows.get(0);
		if (rows.size() > 1)
		{
			log.warn("multiple crewmates found for uniqueId {}, only taking first", uniqueId);
		}

		// scripts use sailing_crew:cargo_npc for all of these lookups
		int npcId = (int) client.getDBTableField(rowID, DBTableID.SailingCrew.COL_CARGO_NPC, 0)[0];
		NPCComposition npcDef = client.getNpcDefinition(npcId);

		return new Crewmate(
			uniqueId,
			npcDef.getName(),
			(int) client.getDBTableField(rowID, DBTableID.SailingCrew.COL_STAT_HELMSMANSHIP, 0)[0],
			(int) client.getDBTableField(rowID, DBTableID.SailingCrew.COL_STAT_PRIVATEERING, 0)[0],
			(int) client.getDBTableField(rowID, DBTableID.SailingCrew.COL_STAT_DECKHANDINESS, 0)[0]
		);
	}

}

package com.duckblade.osrs.sailing.features.util;

import com.duckblade.osrs.sailing.model.Crewmate;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CrewmateTracker
	implements PluginLifecycleComponent
{

	private static final Map<Integer, Integer> CREWMATE_VARBS = ImmutableMap.<Integer, Integer>builder()
		.put(VarbitID.SAILING_CREW_SLOT_1, VarbitID.SAILING_CREW_SLOT_1_POSITION)
		.put(VarbitID.SAILING_CREW_SLOT_2, VarbitID.SAILING_CREW_SLOT_2_POSITION)
		.put(VarbitID.SAILING_CREW_SLOT_3, VarbitID.SAILING_CREW_SLOT_3_POSITION)
		.put(VarbitID.SAILING_CREW_SLOT_4, VarbitID.SAILING_CREW_SLOT_4_POSITION)
		.put(VarbitID.SAILING_CREW_SLOT_5, VarbitID.SAILING_CREW_SLOT_5_POSITION)
		.build();

	private final Client client;
	private final ClientThread clientThread;
	private final CrewmateIndex crewmateIndex;

	@Getter
	private final Map<Integer, Crewmate> crewmates = new HashMap<>();

	@Getter
	private final Map<Integer, CrewmateAssignment> assignments = new HashMap<>();

	@Override
	public void startUp()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::loadAll);
		}
	}

	@Override
	public void shutDown()
	{
		crewmates.clear();
		assignments.clear();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		for (Map.Entry<Integer, Integer> entry : CREWMATE_VARBS.entrySet())
		{
			if (entry.getValue() == e.getVarbitId() || entry.getKey() == e.getVarbitId())
			{
				loadSingle(entry.getKey(), e.getVarbitId());
				return;
			}
		}
	}

	private void loadAll()
	{
		CREWMATE_VARBS.forEach(this::loadSingle);
	}

	private void loadSingle(int slotVarb, int posVarb)
	{
		int crewmateVarbValue = client.getVarbitValue(slotVarb);
		if (crewmateVarbValue == 0)
		{
			crewmates.remove(slotVarb);
			assignments.remove(slotVarb);
			return;
		}

		Crewmate crewmate = crewmateIndex.getCrewmate(crewmateVarbValue);
		crewmates.put(slotVarb, crewmate);

		CrewmateAssignment assignment = CrewmateAssignment.fromCrewAssignmentVarb(client.getVarbitValue(posVarb));
		if (assignment == null)
		{
			assignments.remove(slotVarb);
		}
		else
		{
			assignments.put(slotVarb, assignment);
		}
	}
}

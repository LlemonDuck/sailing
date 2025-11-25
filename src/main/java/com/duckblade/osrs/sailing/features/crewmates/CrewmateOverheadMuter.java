package com.duckblade.osrs.sailing.features.crewmates;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CrewmateOverheadMuter
	implements PluginLifecycleComponent
{

	private final Client client;

	private SailingConfig.CrewmateMuteMode mode;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		mode = config.crewmatesMuteOverheads();
		return mode != SailingConfig.CrewmateMuteMode.NONE;
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged e)
	{
		Actor npc = e.getActor();
		if (!(npc instanceof NPC))
		{
			return;
		}

		if (npc.getWorldView().isTopLevel())
		{
			return;
		}

		if (mode == SailingConfig.CrewmateMuteMode.OTHER_BOATS &&
			npc.getWorldView() == client.getLocalPlayer().getWorldView())
		{
			return;
		}

		log.trace("muting npc {}={} in wv {}", ((NPC) npc).getId(), npc.getName(), npc.getWorldView().getId());
		npc.setOverheadCycle(-1);
		npc.setOverheadText("");
	}
}

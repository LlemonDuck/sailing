package com.duckblade.osrs.sailing.features.barracudatrials;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class HidePortalTransitions
	implements PluginLifecycleComponent
{
	private static final int FADE_OUT_TRANSITION_SCRIPT_ID = 948;

	private final Client client;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.barracudaHidePortalTransitions();
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (!SailingUtil.isSailing(client))
		{
			return;
		}

		if (event.getScriptId() == FADE_OUT_TRANSITION_SCRIPT_ID)
		{
			event.getScriptEvent().getArguments()[4] = 255;
			event.getScriptEvent().getArguments()[5] = 0;
		}
	}

}

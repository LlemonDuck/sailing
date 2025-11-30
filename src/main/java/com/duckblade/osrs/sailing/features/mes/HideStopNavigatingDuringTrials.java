package com.duckblade.osrs.sailing.features.mes;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class HideStopNavigatingDuringTrials implements PluginLifecycleComponent
{

	private static final String OPTION_STOP_NAVIGATING = "Stop-navigating";
	private static final String OPTION_ESCAPE = "Escape";

	private final Client client;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.hideStopNavigatingDuringTrials();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{if (client.getVarbitValue(VarbitID.SAILING_BT_IN_TRIAL) == 0)
		{
			return;
		}

		if (OPTION_STOP_NAVIGATING.equals(e.getOption()) ||
			OPTION_ESCAPE.equals(e.getOption()))
		{
			// Push the Stop-navigating option down instead of removing it
			e.getMenuEntry().setDeprioritized(true);
		}
	}
}

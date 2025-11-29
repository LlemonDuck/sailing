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
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class HidePortalTransitions
	implements PluginLifecycleComponent
{
	private static final int FADE_TRANSITION_SCRIPT_ID = 948;
	private boolean isFadingOutDuringBarracudaTrial = false;

	private final Client client;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.barracudaHidePortalTransitions();
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != FADE_TRANSITION_SCRIPT_ID)
		{
			return;
		}

		if (!SailingUtil.isSailing(client))
		{
			return;
		}

		var gwenithGlideMasterState = client.getVarbitValue(VarbitID.SAILING_BT_GWENITH_GLIDE_MASTER_STATE);

		var isInGwenithGlideTrial = gwenithGlideMasterState >= 2;
		if (!isInGwenithGlideTrial)
		{
			return;
		}

		var args = event.getScriptEvent().getArguments();
		if (args.length < 6)
		{
			return;
		}

		var isFadingOut = (int)args[2] == 255;
		if (isFadingOut)
		{
			isFadingOutDuringBarracudaTrial = true;
			args[4] = 255;
			args[5] = 0;
			return;
		}

		var isFadingIn = (int)args[4] == 255;
		if (isFadingOutDuringBarracudaTrial && isFadingIn)
		{
			isFadingOutDuringBarracudaTrial = false;
			args[2] = 255;
			args[5] = 0;
		}
	}

}

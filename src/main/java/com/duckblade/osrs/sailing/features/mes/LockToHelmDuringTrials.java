package com.duckblade.osrs.sailing.features.mes;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.BarracudaTrial;
import com.duckblade.osrs.sailing.model.SailTier;
import com.duckblade.osrs.sailing.model.WindFacilityTier;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.duckblade.osrs.sailing.features.util.SailingUtil.*;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LockToHelmDuringTrials
	implements PluginLifecycleComponent
{
	private static final Set<Integer> BT_RELATED_IDS =
		Stream.of(
				Arrays.stream(SailTier.values())
					.map(SailTier::getGameObjectIds),
				Arrays.stream(WindFacilityTier.values())
					.map(WindFacilityTier::getGameObjectIds),
				Arrays.stream(BarracudaTrial.values())
					.map(BarracudaTrial::getGameObjectIds)
			)
		.flatMap(s -> s)
		.flatMapToInt(Arrays::stream)
		.boxed()
		.collect(Collectors.toUnmodifiableSet());

	private final Client client;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.lockToHelmDuringTrials();
	}

	@Subscribe(priority = -99)
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (!SailingUtil.isInBarracudaTrialAndAtHelm(client)) return;

		if (e.getMenuEntry().getType() != MenuAction.SET_HEADING)
		{
			if (!BT_RELATED_IDS.contains(e.getIdentifier()))
			{
				e.getMenuEntry().setDeprioritized(true);
			}
		}
	}
}

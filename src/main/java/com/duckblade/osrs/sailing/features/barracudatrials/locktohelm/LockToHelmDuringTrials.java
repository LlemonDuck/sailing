package com.duckblade.osrs.sailing.features.barracudatrials.locktohelm;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.BarracudaTrialTier;
import com.duckblade.osrs.sailing.model.SailTier;
import com.duckblade.osrs.sailing.model.WindFacilityTier;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class LockToHelmDuringTrials
	implements PluginLifecycleComponent
{

	private final Client client;
	private final SailingConfig config;
	private Set<Integer> clickableObjects;
	private Set<LockToHelmFilter> lockToHelmFilters;

	@Inject
	public LockToHelmDuringTrials(Client client, SailingConfig config)
	{
		this.client = client;
		this.config = config;
		this.lockToHelmFilters = config.lockToHelmFilter();
		this.clickableObjects = reloadClickableObjects();
	}

	private Set<Integer> reloadClickableObjects()
	{
		return Stream.of(
				Arrays.stream(BarracudaTrialTier.values())
					.map(BarracudaTrialTier::getGameObjectIds)
					.flatMapToInt(Arrays::stream),
				Arrays.stream(SailTier.values())
					.map(SailTier::getGameObjectIds)
					.flatMapToInt(Arrays::stream),
				Arrays.stream(WindFacilityTier.getWindFacilityObjectIDs(WindFacilityTier.WIND_CATCHER, WindFacilityTier.GALE_CATCHER))
					.filter(s -> lockToHelmFilters.contains(LockToHelmFilter.WIND_GALE_CATCHER)),
				Arrays.stream(WindFacilityTier.CRYSTAL_EXTRACTOR.getGameObjectIds())
					.filter(s -> lockToHelmFilters.contains(LockToHelmFilter.CRYSTAL_EXTRACTOR))
			)
			.flatMapToInt(s -> s)
			.boxed()
			.collect(Collectors.toUnmodifiableSet());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (isEnabled(config) && SailingConfig.LOCK_PLAYER_TO_HELM.equals(e.getKey()))
		{
			log.info("{}: {} -> {}", e.getKey(), e.getOldValue(), e.getNewValue());
			lockToHelmFilters = config.lockToHelmFilter();
			clickableObjects = reloadClickableObjects();
		}
	}

	@Subscribe(priority = -99)
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (!SailingUtil.isInBarracudaTrialAndAtHelm(client))
		{
			return;
		}

		if (e.getMenuEntry().getType() != MenuAction.SET_HEADING)
		{
			if (!clickableObjects.contains(e.getIdentifier()))
			{
				e.getMenuEntry().setDeprioritized(true);
			}
		}
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return !config.lockToHelmFilter().contains(LockToHelmFilter.DISABLED);
	}

	@Override
	public void startUp()
	{
		lockToHelmFilters = config.lockToHelmFilter();
		clickableObjects = reloadClickableObjects();
		log.info("{}: {}", this.getClass().getSimpleName(), lockToHelmFilters);
	}
}

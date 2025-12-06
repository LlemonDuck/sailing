package com.duckblade.osrs.sailing.features.mes;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.FacilityAggregate;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LockToHelmDuringTrials
	implements PluginLifecycleComponent
{

	private static final int FACILITY_HELM = 3;
    private static final String OPTION_STOP_NAVIGATING = "Stop-navigating";
    private static final String OPTION_ESCAPE = "Escape";

    private static final Set<Integer> NON_WIND_FACILITY_IDS = Arrays.stream(FacilityAggregate.values())
        .filter(facility -> facility != FacilityAggregate.WIND)
        .map(FacilityAggregate::getGameObjectIds)
        .flatMapToInt(Arrays::stream)
        .boxed()
        .collect(Collectors.toUnmodifiableSet());

    private static final Comparator<MenuEntry> NON_WIND_FACILITY_COMPARATOR =
        Comparator.comparing((MenuEntry me) -> NON_WIND_FACILITY_IDS.contains(me.getIdentifier())
        && me.getType() != MenuAction.EXAMINE_OBJECT)
        .reversed();

	private final Client client;

    private boolean isNotSailingOrNotInTrials() {
        // Not sailing, not in BT or not at helm
        return !SailingUtil.isSailing(client)
            || client.getVarbitValue(VarbitID.SAILING_BT_IN_TRIAL) == 0
            || client.getVarbitValue(VarbitID.SAILING_BOAT_FACILITY_LOCKEDIN) != FACILITY_HELM;
    }

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.lockToHelmDuringTrials();
	}

	@Subscribe
    public void onMenuEntryAdded(MenuEntryAdded e)
	{
        if (isNotSailingOrNotInTrials()) return;

        if (OPTION_STOP_NAVIGATING.equals(e.getOption()) || OPTION_ESCAPE.equals(e.getOption()))
        {
            // Push the Stop-navigating option down instead of removing it
            e.getMenuEntry().setDeprioritized(true);
        }
	}

    @Subscribe(priority = -99)
    public void onPostMenuSort(PostMenuSort e)
    {
        if (isNotSailingOrNotInTrials()) return;

        Menu menu = client.getMenu();
        menu.setMenuEntries(
            Arrays.stream(menu.getMenuEntries())
                .sorted(NON_WIND_FACILITY_COMPARATOR)
                .toArray(MenuEntry[]::new)
        );
    }

}

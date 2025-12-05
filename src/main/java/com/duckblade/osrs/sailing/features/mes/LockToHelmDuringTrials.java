package com.duckblade.osrs.sailing.features.mes;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.CargoHoldTier;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
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

    private static final Set<Integer> CARGO_HOLD_IDS = Arrays.stream(CargoHoldTier.values())
        .map(CargoHoldTier::getGameObjectIds)
        .flatMapToInt(Arrays::stream)
        .boxed()
        .collect(Collectors.toUnmodifiableSet());

    private static final Comparator<MenuEntry> MENU_ENTRY_COMPARATOR =
        Comparator.comparing((MenuEntry me) -> CARGO_HOLD_IDS.contains(me.getIdentifier())
        && me.getType() != MenuAction.EXAMINE_OBJECT)
        .reversed();

	private final Client client;

    private boolean isNotSailingOrInTrials() {
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
        if (isNotSailingOrInTrials()) return;

        if (OPTION_STOP_NAVIGATING.equals(e.getOption()) || OPTION_ESCAPE.equals(e.getOption()))
        {
            // Push the Stop-navigating option down instead of removing it
            e.getMenuEntry().setDeprioritized(true);
        }
	}

    @Subscribe(priority = -99)
    public void onPostMenuSort(PostMenuSort e)
    {
        if (isNotSailingOrInTrials()) return;

        Menu menu = client.getMenu();
        menu.setMenuEntries(
            Arrays.stream(menu.getMenuEntries())
                .sorted(MENU_ENTRY_COMPARATOR)
                .toArray(MenuEntry[]::new)
        );
    }

}

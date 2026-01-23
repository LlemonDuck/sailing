package com.duckblade.osrs.sailing.features.mes;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.util.Arrays;
import java.util.Comparator;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LockToHelmDuringTrials implements PluginLifecycleComponent
{

	private static final int LOCKED_TO_HELM = 3;

	private static final String[] FACILITY_SUBSTRINGS = new String[] {"Anchor", "Helm", "Keg", "Range", "cargo hold", "cannon",
		"focus", "net", "pearl", "salvaging hook", "station", "spreader", "stone"};

	private static final Comparator<MenuEntry> MENU_ENTRY_COMPARATOR =
		Comparator.comparing((MenuEntry me) ->
				me.getTarget().startsWith("<col=ffff>") &&
					Arrays.stream(FACILITY_SUBSTRINGS).anyMatch(facilitySubstring -> me.getTarget().contains(facilitySubstring)))
			.reversed();

	private final Client client;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.lockToHelmDuringTrials();
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort e)
	{
		if (client.getVarbitValue(VarbitID.SAILING_BOAT_FACILITY_LOCKEDIN) != LOCKED_TO_HELM ||
			client.getVarbitValue(VarbitID.SAILING_BT_IN_TRIAL) == 0)
		{
			return;
		}

		Menu menu = client.getMenu();
		menu.setMenuEntries(
			Arrays.stream(menu.getMenuEntries())
				.sorted(MENU_ENTRY_COMPARATOR)
				.toArray(MenuEntry[]::new)
		);
	}
}

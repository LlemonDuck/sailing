package com.duckblade.osrs.sailing.features.mes;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Comparator;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DeprioSailsOffHelm
	implements PluginLifecycleComponent
{

	private static final int FACILITY_HELM = 3;
	private static final String MENU_TARGET_SAILS = "<col=ffff>Sails";

	private static final Comparator<MenuEntry> IS_SAILS =
		Comparator.comparing((MenuEntry me) -> MENU_TARGET_SAILS.equals(me.getTarget()))
			.reversed();

	private final Client client;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.disableSailsWhenNotAtHelm();
	}

	@Subscribe(priority = -1)
	public void onPostMenuSort(PostMenuSort e)
	{
		if (!SailingUtil.isSailing(client))
		{
			return;
		}

		// todo getSailingFacility
		// todo crewmate support?
		if (client.getVarbitValue(VarbitID.SAILING_BOAT_FACILITY_LOCKEDIN) == FACILITY_HELM)
		{
			// at helm
			return;
		}

		Menu menu = client.getMenu();
		menu.setMenuEntries(
			Arrays.stream(menu.getMenuEntries())
				.sorted(IS_SAILS)
				.toArray(MenuEntry[]::new)
		);
	}

}

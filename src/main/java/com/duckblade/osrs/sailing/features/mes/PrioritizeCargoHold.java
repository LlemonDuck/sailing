package com.duckblade.osrs.sailing.features.mes;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.CargoHoldTier;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PrioritizeCargoHold
	implements PluginLifecycleComponent
{

	private static final Set<Integer> CARGO_HOLD_IDS = Arrays.stream(CargoHoldTier.values())
		.map(CargoHoldTier::getGameObjectIds)
		.flatMapToInt(Arrays::stream)
		.boxed()
		.collect(Collectors.toUnmodifiableSet());

	private static final Comparator<MenuEntry> MENU_ENTRY_COMPARATOR =
		Comparator.comparing((me) -> CARGO_HOLD_IDS.contains(me.getIdentifier()) && me.getType() != MenuAction.EXAMINE_OBJECT);

	private final Client client;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.prioritizeCargoHold();
	}

	@Subscribe(priority = -100)
	public void onPostMenuSort(PostMenuSort e)
	{
		if (!SailingUtil.isSailing(client))
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

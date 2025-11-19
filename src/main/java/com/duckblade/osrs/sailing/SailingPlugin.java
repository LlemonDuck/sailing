package com.duckblade.osrs.sailing;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Sailing"
)
public class SailingPlugin extends Plugin
{

	private static final Set<Integer> CARGO_HOLDS = ImmutableSet.of(
		ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT,
		ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_RAFT_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_RAFT,
		ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_RAFT_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_RAFT,
		ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_RAFT_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_RAFT,
		ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_RAFT_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_RAFT,
		ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_RAFT_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT,
		ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_RAFT_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT,
		ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_RAFT_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_2X5,
		ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_2X5_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5,
		ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_2X5_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_2X5,
		ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_2X5_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_2X5,
		ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_2X5_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_2X5,
		ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_2X5_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_2X5,
		ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_2X5_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_2X5,
		ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_2X5_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE,
		ObjectID.SAILING_BOAT_CARGO_HOLD_REGULAR_LARGE_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_LARGE,
		ObjectID.SAILING_BOAT_CARGO_HOLD_OAK_LARGE_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_LARGE,
		ObjectID.SAILING_BOAT_CARGO_HOLD_TEAK_LARGE_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_LARGE,
		ObjectID.SAILING_BOAT_CARGO_HOLD_MAHOGANY_LARGE_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE,
		ObjectID.SAILING_BOAT_CARGO_HOLD_CAMPHOR_LARGE_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_LARGE,
		ObjectID.SAILING_BOAT_CARGO_HOLD_IRONWOOD_LARGE_OPEN,
		ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE,
		ObjectID.SAILING_BOAT_CARGO_HOLD_ROSEWOOD_LARGE_OPEN
	);

	@Inject
	private Client client;

	@Inject
	private EventBus eventBus;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SailingConfig config;

	@Inject
	private SailingOverlay sailingOverlay;

	@Inject
	private LuffOverlay luffOverlay;

	@Inject
	private RapidsOverlay rapidsOverlay;

	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, GameObject> cargoHolds = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(sailingOverlay);

		eventBus.register(luffOverlay);
		overlayManager.add(luffOverlay);

		eventBus.register(rapidsOverlay);
		overlayManager.add(rapidsOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(luffOverlay);
		eventBus.unregister(luffOverlay);
		luffOverlay.shutDown();

		overlayManager.remove(rapidsOverlay);
		eventBus.unregister(rapidsOverlay);
		rapidsOverlay.shutDown();

		overlayManager.remove(sailingOverlay);

		cargoHolds.clear();
	}

	@Provides
	SailingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SailingConfig.class);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (client.getLocalPlayer().getWorldView().isTopLevel())
		{
			// not in a boat
			return;
		}

		if (!config.disableSailsWhenNotAtHelm())
		{
			return;
		}

		// todo magic constant
		// todo getSailingFacility
		if (client.getVarbitValue(VarbitID.SAILING_BOAT_FACILITY_LOCKEDIN) == 3)
		{
			// at sails
			return;
		}

		// todo magic constant
		if (e.getTarget().equals("<col=ffff>Sails"))
		{
			e.getMenuEntry().setDeprioritized(true);
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();
		if (o.getWorldView().isTopLevel())
		{
			return;
		}

		if (CARGO_HOLDS.contains(o.getId()))
		{
			cargoHolds.put(o.getWorldView().getId(), o);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		GameObject o = e.getGameObject();
		cargoHolds.remove(o.getWorldView().getId());
	}
}

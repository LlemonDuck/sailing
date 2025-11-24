package com.duckblade.osrs.sailing.features.cargohold;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;

@Slf4j
@Singleton
public class CargoHoldTracker
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final String CONFIG_PREFIX = "cargoHoldInventory_";

	private static final int UNKNOWN_ITEM = -1;

	private static final String MSG_CREWMATE_SALVAGES = "Managed to hook some salvage! I'll put it in the cargo hold.";
	private static final String MSG_CREWMATE_SALVAGE_FULL = ""; // todo

	private static final Set<Integer> CARGO_INVENTORY_IDS = ImmutableSet.of(
		InventoryID.SAILING_BOAT_1_CARGOHOLD,
		InventoryID.SAILING_BOAT_2_CARGOHOLD,
		InventoryID.SAILING_BOAT_3_CARGOHOLD,
		InventoryID.SAILING_BOAT_4_CARGOHOLD,
		InventoryID.SAILING_BOAT_5_CARGOHOLD
	);

	private static final char CONFIG_DELIMITER_PAIRS = ';';
	private static final char CONFIG_DELIMITER_KV = ':';
	private static final Splitter.MapSplitter CONFIG_SPLITTER = Splitter.on(CONFIG_DELIMITER_PAIRS)
		.withKeyValueSeparator(CONFIG_DELIMITER_KV);
	private static final Joiner.MapJoiner CONFIG_JOINER = Joiner.on(CONFIG_DELIMITER_PAIRS)
		.withKeyValueSeparator(CONFIG_DELIMITER_KV);

	private final Client client;
	private final ConfigManager configManager;
	private final BoatTracker boatTracker;

	// boat slot -> item id+count
	private final Map<Integer, Multiset<Integer>> cargoHoldItems = new HashMap<>();
	private Multiset<Integer> memoizedInventory;

	private boolean overlayEnabled;
	private boolean pendingInventoryAction;
	private boolean sawItemContainerUpdate;

	@Inject
	public CargoHoldTracker(Client client, ConfigManager configManager, BoatTracker boatTracker)
	{
		this.client = client;
		this.configManager = configManager;
		this.boatTracker = boatTracker;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public void startUp()
	{
		for (int boatSlot = 0; boatSlot < 5; boatSlot++)
		{
			loadFromConfig(boatSlot);
		}
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		// always on for tracking events, conditionally display
		overlayEnabled = config.cargoHoldShowCounts();
		return true;
	}

	@Override
	public void shutDown()
	{
		cargoHoldItems.clear();
		memoizedInventory = null;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!overlayEnabled || !SailingUtil.isSailing(client))
		{
			return null;
		}

		Boat boat = boatTracker.getBoat();
		GameObject cargoHold = boat != null ? boat.getCargoHold() : null;
		if (cargoHold == null)
		{
			return null;
		}

		int usedCapacity = usedCapacity();
		int maxCapacity = maxCapacity();
		String text = (usedCapacity != -1 ? String.valueOf(usedCapacity) : "???") + "/" + (maxCapacity != -1 ? String.valueOf(maxCapacity) : "???");
		Color textColor = ColorUtil.colorLerp(Color.GREEN, Color.RED, (double) usedCapacity / maxCapacity);
		Point textLocation = cargoHold.getCanvasTextLocation(g, text, 0);
		if (textLocation != null)
		{
			OverlayUtil.renderTextLocation(g, textLocation, text, textColor);
		}

		return null;
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged e)
	{
		Actor actor = e.getActor();
		if (!(actor instanceof NPC) ||
			!SailingUtil.isSailing(client) ||
			actor.getWorldView() != client.getLocalPlayer().getWorldView())
		{
			return;
		}

		if (MSG_CREWMATE_SALVAGES.equals(e.getOverheadText()))
		{
			// todo different ones? doesn't matter now since it's count only but will matter later
			cargoHold().add(ItemID.SAILING_SMALL_SHIPWRECK_SALVAGE);
			writeToConfig();
		}

		if (MSG_CREWMATE_SALVAGE_FULL.equals(e.getOverheadText()))
		{
			cargoHold().add(UNKNOWN_ITEM, maxCapacity() - usedCapacity());
			writeToConfig();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (!CARGO_INVENTORY_IDS.contains(e.getContainerId() & 0x4FFF))
		{
			return;
		}

		sawItemContainerUpdate = true;

		ItemContainer containerInv = e.getItemContainer();
		Multiset<Integer> trackedInv = cargoHold();
		trackedInv.clear();
		for (Item item : containerInv.getItems())
		{
			if (item != null)
			{
				trackedInv.add(item.getId(), item.getQuantity());
			}
		}
		writeToConfig();
	}

	@Subscribe
	public void onPostClientTick(PostClientTick e)
	{
		Multiset<Integer> oldInventory = memoizedInventory;
		boolean shouldInfer = !sawItemContainerUpdate && pendingInventoryAction && memoizedInventory != null;
		pendingInventoryAction = false;
		sawItemContainerUpdate = false;
		memoizedInventory = null;

		if (!shouldInfer)
		{
			return;
		}

		Multiset<Integer> newInventory = getInventoryMap();
		Multiset<Integer> withdrawn = Multisets.difference(newInventory, oldInventory);
		Multiset<Integer> deposited = Multisets.difference(oldInventory, newInventory);

		Multiset<Integer> cargoHold = cargoHold();
		Multisets.removeOccurrences(cargoHold, withdrawn);
		deposited.entrySet().forEach(entry -> cargoHold.add(entry.getElement(), entry.getCount()));
		writeToConfig();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		if (!e.getMenuTarget().contains("Withdraw") && !e.getMenuTarget().contains("Deposit"))
		{
			return;
		}

		Widget cargoHoldWidget = client.getWidget(InterfaceID.SAILING_BOAT_CARGOHOLD); // todo confirm
		if (cargoHoldWidget != null && !cargoHoldWidget.isHidden())
		{
			pendingInventoryAction = true;
			memoizedInventory = getInventoryMap();
		}
	}

	private Multiset<Integer> cargoHold()
	{
		return cargoHold(currentBoatSlot());
	}

	private Multiset<Integer> cargoHold(int boatSlot)
	{
		return cargoHoldItems.computeIfAbsent(boatSlot, k -> HashMultiset.create());
	}

	private int currentBoatSlot()
	{
		return client.getVarbitValue(VarbitID.SAILING_LAST_PERSONAL_BOAT_BOARDED) - 1;
	}

	private int usedCapacity()
	{
		return cargoHold().size();
	}

	private int maxCapacity()
	{
		Boat boat = boatTracker.getBoat();
		if (boat == null)
		{
			return -1;
		}

		return boat.getCargoCapacity(client);
	}

	private Multiset<Integer> getInventoryMap()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv == null)
		{
			return ImmutableMultiset.of();
		}

		Multiset<Integer> ret = HashMultiset.create();
		for (Item item : inv.getItems())
		{
			if (item == null)
			{
				continue;
			}

			int quantity = item.getQuantity();
			ret.add(item.getId(), quantity);
		}

		return ret;
	}

	private String configKey(int boatSlot)
	{
		return CONFIG_PREFIX + boatSlot;
	}

	private void loadFromConfig(int boatSlot)
	{
		String key = configKey(boatSlot);
		String savedInventory = configManager.getRSProfileConfiguration(SailingConfig.CONFIG_GROUP, key);
		if (savedInventory != null)
		{
			Multiset<Integer> hold = cargoHold(boatSlot);
			CONFIG_SPLITTER.split(savedInventory).forEach((k, v) ->
				hold.add(Integer.parseInt(k), Integer.parseInt(v)));
		}
	}

	private void writeToConfig()
	{
		writeToConfig(currentBoatSlot());
	}

	private void writeToConfig(int boatSlot)
	{
		String key = configKey(boatSlot);
		Multiset<Integer> hold = cargoHold(boatSlot);
		String configValue = CONFIG_JOINER.join(hold.entrySet()
			.stream()
			.map(entry -> Map.entry(entry.getElement(), entry.getCount()))
			.iterator());

		configManager.setRSProfileConfiguration(SailingConfig.CONFIG_GROUP, key, configValue);
	}

}

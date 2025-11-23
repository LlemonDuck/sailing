package com.duckblade.osrs.sailing.features.cargohold;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
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
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
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

	private static final String MSG_CREWMATE_SALVAGES = "Managed to hook some salvage! I'll put it in the cargo hold.";
	private static final Set<Integer> CARGO_INVENTORY_IDS = ImmutableSet.of(
		InventoryID.SAILING_BOAT_1_CARGOHOLD,
		InventoryID.SAILING_BOAT_2_CARGOHOLD,
		InventoryID.SAILING_BOAT_3_CARGOHOLD,
		InventoryID.SAILING_BOAT_4_CARGOHOLD,
		InventoryID.SAILING_BOAT_5_CARGOHOLD
	);

	private final Client client;

	// boat slot -> item id -> item count
	private final Map<Integer, Map<Integer, Integer>> cargoHoldItems = new HashMap<>();
	private final BoatTracker boatTracker;

	private boolean overlayEnabled;

	@Inject
	public CargoHoldTracker(Client client, BoatTracker boatTracker)
	{
		this.client = client;
		this.boatTracker = boatTracker;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
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

		int maxCapacity = boat.getCargoCapacity(client);
		int usedCapacity = currentBoatInventory()
			.values()
			.stream()
			.mapToInt(Integer::intValue)
			.sum();

		String text = usedCapacity + "/" + (maxCapacity != -1 ? String.valueOf(maxCapacity) : "???");
		Color textColour = ColorUtil.colorLerp(Color.GREEN, Color.RED, (double) usedCapacity / maxCapacity);
		Point textLocation = cargoHold.getCanvasTextLocation(g, text, 0);
		if (textLocation != null)
		{
			OverlayUtil.renderTextLocation(g, textLocation, text, textColour);
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
			currentBoatInventory().compute(ItemID.SAILING_SMALL_SHIPWRECK_SALVAGE, incrementBy(1));
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (!CARGO_INVENTORY_IDS.contains(e.getContainerId() & 0x4FFF))
		{
			return;
		}

		ItemContainer containerInv = e.getItemContainer();
		Map<Integer, Integer> trackedInv = currentBoatInventory();
		trackedInv.clear();
		for (Item item : containerInv.getItems())
		{
			if (item != null)
			{
				trackedInv.compute(item.getId(), incrementBy(item.getQuantity()));
			}
		}
	}

	private Map<Integer, Integer> currentBoatInventory()
	{
		return cargoHoldItems.computeIfAbsent(currentBoatSlot() + 1, k -> new HashMap<>());
	}

	private int currentBoatSlot()
	{
		return client.getVarbitValue(VarbitID.SAILING_LAST_PERSONAL_BOAT_BOARDED);
	}

	private static BiFunction<Integer, Integer, Integer> incrementBy(int addend)
	{
		return (k, v) ->
			v != null ? v + addend : addend;
	}
}

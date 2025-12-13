package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.courier.CourierTaskTracker;
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
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
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

	// todo cargo pickup and dropoff
	// these aren't necessary as of dec 4 2025 but jagex has said that is temporary (we'll see)
	private static final String MES_CARGO_PACK_PERSONAL = "You deposit some cargo into the cargo hold."; // always 1
	private static final String MES_CARGO_PACK_CREW = "Your crew pack the cargo they were holding into the cargo hold."; // todo determine amount?

	private static final String MSG_CREWMATE_SALVAGES = "Managed to hook some salvage! I'll put it in the cargo hold.";
	private static final String MSG_CREWMATE_SALVAGE_FULL = "The cargo hold is full. I can't salvage anything.";
	private static final String WIDGET_TEXT_CARGO_HOLD_EMPTY = "This cargo hold has no items to show here.";

	private static final Set<String> JENKINS_MESSAGES = ImmutableSet.of(
		"Wooo.",
		"Woooo wooo.",
		"Woooo wooo wooo.",
		"Woooo wooooo woooo.",
		"Woooo wooo wooooo woooo."
	);

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

	private static final int INVENTORY_DELTA_MAX_DELAY = 2;

	private final Client client;
	private final ConfigManager configManager;
	private final BoatTracker boatTracker;
	private final CourierTaskTracker courierTaskTracker;

	// boat slot -> item id+count
	private final Map<Integer, Multiset<Integer>> cargoHoldItems = new HashMap<>();
	private final Map<Integer, Boolean> stackabilityCache = new HashMap<>();

	private Multiset<Integer> memoizedInventory;

	private boolean overlayEnabled;
	private int pendingInventoryAction;
	private boolean sawItemContainerUpdate;
	private boolean sawInventoryContainerUpdate;
    private boolean sawQuickDeposit;
    private int tickCount;
	private int lastXp;
	private boolean pendingJenkinsAction;

	@Inject
	public CargoHoldTracker(Client client, ConfigManager configManager, BoatTracker boatTracker, CourierTaskTracker courierTaskTracker)
	{
		this.client = client;
		this.configManager = configManager;
		this.boatTracker = boatTracker;
		this.courierTaskTracker = courierTaskTracker;

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
	public void startUp()
	{
		loadAllFromConfig();

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			lastXp = client.getSkillExperience(Skill.SAILING);
		}
	}

	@Override
	public void shutDown()
	{
		cargoHoldItems.clear();
		memoizedInventory = null;
		pendingJenkinsAction = false;
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

		int maxCapacity = maxCapacity();
		int usedCapacity = usedCapacity();
		String text = (usedCapacity != -1 ? String.valueOf(usedCapacity) : "???") + "/" + (maxCapacity != -1 ? String.valueOf(maxCapacity) : "???");
		Color textColour = ColorUtil.colorLerp(Color.GREEN, Color.RED, (double) usedCapacity / maxCapacity);
		Point textLocation = cargoHold.getCanvasTextLocation(g, text, 0);
		if (textLocation != null)
		{
			OverlayUtil.renderTextLocation(g, textLocation, text, textColour);
		}

		return null;
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		loadAllFromConfig();
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
			log.trace("crewmate salvage");
			add(ItemID.SAILING_SMALL_SHIPWRECK_SALVAGE, 1);
			writeToConfig();
			return;
		}

		if (MSG_CREWMATE_SALVAGE_FULL.equals(e.getOverheadText()))
		{
			set(UNKNOWN_ITEM, maxCapacity() - usedCapacity());
			writeToConfig();
			return;
		}

		if (JENKINS_MESSAGES.contains(e.getOverheadText()))
		{
			pendingJenkinsAction = true;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		if (e.getSkill() == Skill.SAILING &&
			lastXp != (lastXp = e.getXp()) &&
			pendingJenkinsAction)
		{
			log.trace("jenkins salvage");
			add(ItemID.SAILING_SMALL_SHIPWRECK_SALVAGE, 1);
			writeToConfig();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
        if (sawQuickDeposit && tickCount <= 8){
            Multiset<Integer> cargoHoldToUpdate = cargoHold();
            ItemContainer containerInv = e.getItemContainer();
            Multiset<Integer> trackedInv = cargoHold();
            log.debug("read cargo hold inventory from event {}", trackedInv);
            Multiset<Integer> newInventory = HashMultiset.create();
            for (Item item : containerInv.getItems())
            {
                if (item == null || item.getId() == UNKNOWN_ITEM)
                {
                    continue;
                }

                // todo fix stackable items
                newInventory.add(item.getId());
            }
            Multiset<Integer> deposited = Multisets.difference(memoizedInventory, newInventory);
            log.debug(Long.toString(deposited.size()));
            deposited.entrySet().forEach(entry -> cargoHoldToUpdate.add(entry.getElement(), entry.getCount()));
            log.trace("deposited: {}", deposited);
            sawItemContainerUpdate = true;
            writeToConfig();
            sawQuickDeposit = false;
            return;
        }

		if (e.getContainerId() == InventoryID.INV)
		{
			sawInventoryContainerUpdate = true;
			return;
		}

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
			if (item == null)
			{
				continue;
			}

			if (courierTaskTracker.getTaskForItemID(item.getId()) != null)
			{
				continue;
			}

			add(item.getId(), item.getQuantity());
		}

		log.debug("read cargo hold inventory from event {}", trackedInv);
		writeToConfig();
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		pendingJenkinsAction = false;

        //8 is the max ticks it takes between clicking Quick-deposit, walking to cargo hold, and depositing items in a sloop
        //if sawQuickDeposit is still enabled after 8 ticks, it is most likely due to inventory not changing
        if (sawQuickDeposit && tickCount <= 8) {
            tickCount++;
            log.debug(Integer.toString(tickCount));
        } else if (sawQuickDeposit) {
            sawQuickDeposit = false;
        }

		if (--pendingInventoryAction < 0)
		{
			sawItemContainerUpdate = false;
			return;
		}

		if (sawItemContainerUpdate)
		{
			// trust the item container event over any guesswork
			resetInventoryDeltaState();
			return;
		}

		if (!sawInventoryContainerUpdate || memoizedInventory == null)
		{
			// inventory change might be delayed an extra tick if action was clicked near the tick boundary
			return;
		}

		Multiset<Integer> oldInventory = memoizedInventory;
		resetInventoryDeltaState();

		Multiset<Integer> cargoHoldToUpdate = cargoHold();

		Multiset<Integer> newInventory = getInventoryMap();
		log.trace("new inventory {}", newInventory);

		Multiset<Integer> withdrawn = Multisets.difference(newInventory, oldInventory); // items found in inv that weren't in prior snapshot
		log.trace("withdrawn: {}", withdrawn);

		Multiset<Integer> deposited = Multisets.difference(oldInventory, newInventory); // items missing from inv that were in prior snapshot
		log.trace("deposited: {}", deposited);

		Multisets.removeOccurrences(cargoHoldToUpdate, withdrawn);
		deposited.entrySet().forEach(entry -> cargoHoldToUpdate.add(entry.getElement(), entry.getCount()));

		log.debug("updated cargo hold from inventory delta {}", cargoHoldToUpdate);
		writeToConfig();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
        if (e.getMenuOption().contains("Quick-deposit") && (maxCapacity() - usedCapacity() > 0)){
            log.debug("quick deposit triggered");
            tickCount = 0;
            memoizedInventory = getInventoryMap();
            sawQuickDeposit = true;
            return;
        }

		if (!e.getMenuOption().contains("Withdraw") && !e.getMenuOption().contains("deposit"))
		{
			return;
		}

        Widget cargoHoldWidget = client.getWidget(InterfaceID.SailingBoatCargohold.UNIVERSE); // todo confirm
		if (cargoHoldWidget != null && !cargoHoldWidget.isHidden())
		{
			pendingInventoryAction = INVENTORY_DELTA_MAX_DELAY;
			memoizedInventory = getInventoryMap();
			log.debug("queued pendingInventoryAction with inventory {}", memoizedInventory);
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded e)
	{
		if (e.getGroupId() != InterfaceID.SAILING_BOAT_CARGOHOLD)
		{
			return;
		}

		Widget itemsParent = client.getWidget(InterfaceID.SailingBoatCargohold.ITEMS);
		Widget itemsChild = itemsParent != null ? itemsParent.getChild(0) : null;
		if (itemsChild != null && Objects.equals(itemsChild.getText(), WIDGET_TEXT_CARGO_HOLD_EMPTY))
		{
			cargoHold().clear();
		}
	}

	private void resetInventoryDeltaState()
	{
		pendingInventoryAction = 0;
		sawItemContainerUpdate = false;
		sawInventoryContainerUpdate = false;
		memoizedInventory = null;
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
		return Math.min(maxCapacity(), Math.max(0, cargoHold().size()));
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
			if (item == null || item.getId() == UNKNOWN_ITEM)
			{
				continue;
			}

			// todo fix stackable items
			ret.add(item.getId());
		}

		return ret;
	}

	private void add(int item, int count)
	{
		if (isStackable(item))
		{
			cargoHold().setCount(item, 1);
		}
		else
		{
			cargoHold().add(item, count);
		}
	}

	private void set(int item, int count)
	{
		if (isStackable(item))
		{
			cargoHold().setCount(item, 1);
		}
		else
		{
			cargoHold().setCount(item, count);
		}
	}

	private boolean isStackable(int itemId)
	{
		assert client.isClientThread();
		return stackabilityCache.computeIfAbsent(itemId, id ->
			client.getItemDefinition(id).isStackable()
		);
	}

	private String configKey(int boatSlot)
	{
		return CONFIG_PREFIX + boatSlot;
	}

	private void loadAllFromConfig()
	{
		if (configManager.getRSProfileKey() != null)
		{
			for (int boatSlot = 0; boatSlot < 5; boatSlot++)
			{
				loadFromConfig(boatSlot);
			}
		}
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
			log.debug("read cargoHold {} from config {} = {}", boatSlot, key, hold);
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
		log.trace("wrote cargoHold {} to config {} = {}", boatSlot, key, configValue);
	}

}

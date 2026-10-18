package com.duckblade.osrs.sailing.features.facilities;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Follows a deposit option clicked on the cargo hold object itself (Deposit-all, Quick-deposit, Deposit-held).
 * Those never open the hold, so its contents are not resent; the only sign of the deposit is the inventory
 * shrinking once the player gets to the hold, and that is what is counted.
 * <p>
 * The rules were measured in game against the hold's own contents and then replayed over recordings of every
 * deposit made:
 * <ul>
 * <li>Beside the hold the game carries the click out in the same tick, so nothing clicked afterwards can stop
 * it. From afar the player walks there first, and a click in the game world on the way stops the walk. But the
 * deposit may already have gone through when that click was made (Walk here, Drop or Open in the tick the
 * player arrived), so the deposit gets one more tick before it is called off. A click that only works the
 * interface (a tab, a side panel, choosing an item or spell to use) does not stop the walk.</li>
 * <li>A Drop, Eat or alchemy cast clicked before the deposit always goes through: on its own a tick earlier,
 * or inside the deposit's own inventory change, which is then a slot larger than the deposit. Clicked after
 * the deposit it is never both: the deposit lands and the drop finds nothing, or the drop lands and the
 * deposit never happens. So a fall of no more than the outstanding drop clicks is the drops, with the deposit
 * still to come, and a larger fall is the deposit less the drops clicked ahead of it.</li>
 * <li>A pickup while the deposit is pending goes into the hold with everything else.</li>
 * <li>A second deposit click in the tick the first landed is a new order; the first still counts.</li>
 * <li>The game plays a sound when a deposit lands and another when an item is dropped, but only while sound
 * effects are on, so a sound confirms and never decides.</li>
 * </ul>
 */
@Slf4j
class CargoHoldDepositWatch
{

	/** How long a deposit click is followed: the walk across the deck to the hold takes a few ticks. */
	static final int MAX_TICKS = 10;

	private static final int NONE = Integer.MIN_VALUE;

	/** A Drop, Eat or alchemy click whose item has not left the inventory yet. */
	private static final class RemovalClick
	{
		private final int tick;
		private final int slot;
		private final int itemId;

		private RemovalClick(int tick, int slot, int itemId)
		{
			this.tick = tick;
			this.slot = slot;
			this.itemId = itemId;
		}
	}

	private int ticksLeft;
	/** Clicked while standing beside the hold, so nothing can cancel it. */
	private boolean besideHold;
	/** The inventory the deposit is measured against; moves with drops and pickups that are not the deposit. */
	private Multiset<Integer> inventoryBefore = ImmutableMultiset.of();
	private Multiset<Integer> inventoryNow = ImmutableMultiset.of();
	/** The inventory fell by more than the outstanding drops: the deposit has landed and waits for the tick. */
	private boolean landed;
	/** The tick of a click that stopped the walk to the hold, or NONE. */
	private int cancelTick = NONE;
	/** Oldest first. A click older than a tick whose item never left found nothing and is forgotten. */
	private final List<RemovalClick> removalClicks = new ArrayList<>();
	/** How many of those were clicked before the deposit: they go through whatever the deposit does. */
	private int removalsBeforeDeposit;
	private int dropSoundTick = NONE;
	/** The inventory's size when it last changed, to settle drop clicks while no deposit is pending. */
	private int lastInventorySize = -1;

	boolean isPending()
	{
		return ticksLeft > 0;
	}

	/**
	 * A deposit option was clicked on the hold.
	 *
	 * @param inventory the inventory as it is now
	 * @param beside    whether the player stands beside the hold, where the click is carried out at once
	 * @param tick      the current tick
	 * @return the deposit of an earlier click that landed in this tick and has not been counted yet, else empty
	 */
	Multiset<Integer> depositClicked(Multiset<Integer> inventory, boolean beside, int tick)
	{
		Multiset<Integer> earlier = ImmutableMultiset.of();
		if (ticksLeft > 0 && landed)
		{
			// The last click's change has landed but the tick has not come round to count it. The second click
			// must not wipe it (Deposit-all, then Deposit-all again in the tick the first landed).
			earlier = finish(tick);
		}

		ticksLeft = MAX_TICKS;
		besideHold = beside;
		inventoryBefore = inventory;
		inventoryNow = inventory;
		lastInventorySize = inventory.size();
		landed = false;
		cancelTick = NONE;
		forgetStaleRemovals(tick);
		removalsBeforeDeposit = removalClicks.size();
		return earlier;
	}

	/**
	 * Drop, Eat or an alchemy cast was clicked on an inventory item: a slot about to empty that is not a deposit.
	 * A second click on a slot whose first has not shown yet finds nothing (ten Drops spam-clicked in four ticks
	 * emptied nine slots, one slot having been clicked twice).
	 */
	void itemRemovalClicked(int slot, int itemId, int tick)
	{
		for (RemovalClick click : removalClicks)
		{
			if (click.slot == slot)
			{
				return;
			}
		}
		removalClicks.add(new RemovalClick(tick, slot, itemId));
	}

	/**
	 * Something was clicked that stops the walk to the hold. The deposit is called off once a tick has passed
	 * with no inventory change, since the game may already have carried it out when the click was made.
	 */
	void walkInterrupted(int tick)
	{
		if (ticksLeft <= 0 || besideHold || cancelTick != NONE)
		{
			return;
		}
		cancelTick = tick;
	}

	/** The game played the sound of an item being dropped. */
	void dropSoundPlayed(int tick)
	{
		dropSoundTick = tick;
	}

	/** The game played the deposit sound: the click is going through, so a click on the way no longer calls it off. */
	void depositSoundPlayed(int tick)
	{
		if (ticksLeft > 0)
		{
			cancelTick = NONE;
		}
	}

	/** The inventory changed. */
	void inventoryChanged(Multiset<Integer> inventory, int tick)
	{
		forgetStaleRemovals(tick);
		if (ticksLeft > 0)
		{
			int fall = inventoryBefore.size() - inventory.size();
			if (fall > 0 && fall <= removalClicks.size())
			{
				// No more slots gone than the player just dropped: those items, not the deposit, which may still
				// land (Drop then Deposit-all in one tick, the drop landing first and the rest a tick later; two
				// Drops around a Deposit-all from afar, both landing and no deposit).
				removalsLanded(fall);
				inventoryBefore = inventory;
			}
			else if (fall > 0)
			{
				landed = true;
			}
			else if (fall < 0)
			{
				// A pickup ahead of the deposit, which the deposit then takes too (Take, then Deposit-all, the
				// pickup landing after the click: 3 were carried at the click and 4 went in).
				inventoryBefore = inventory;
			}
			inventoryNow = inventory;
		}
		else if (lastInventorySize > inventory.size())
		{
			// The dropped items left on their own before any deposit click, so a deposit clicked straight after
			// gets all of its slots.
			removalsLanded(lastInventorySize - inventory.size());
		}
		lastInventorySize = inventory.size();
	}

	/**
	 * A game tick passed.
	 *
	 * @return what the pending deposit put into the hold if it has landed, else empty
	 */
	Multiset<Integer> tick(int tick)
	{
		if (ticksLeft <= 0)
		{
			return ImmutableMultiset.of();
		}

		if (landed)
		{
			return finish(tick);
		}

		if (cancelTick != NONE && tick > cancelTick + 1)
		{
			log.debug("quick deposit called off by a click at tick {}", cancelTick);
			ticksLeft = 0;
			cancelTick = NONE;
			removalsBeforeDeposit = 0;
			return ImmutableMultiset.of();
		}

		if (--ticksLeft == 0)
		{
			log.debug("quick deposit never showed in the inventory");
			removalsBeforeDeposit = 0;
		}
		return ImmutableMultiset.of();
	}

	/** The hold's own contents arrived: whatever was pending is in them, or never happened. */
	void reset()
	{
		ticksLeft = 0;
		landed = false;
		cancelTick = NONE;
		removalsBeforeDeposit = 0;
		inventoryBefore = ImmutableMultiset.of();
		inventoryNow = ImmutableMultiset.of();
	}

	/** Forget everything, drop clicks included. */
	void clear()
	{
		reset();
		removalClicks.clear();
		dropSoundTick = NONE;
		lastInventorySize = -1;
	}

	/**
	 * Whether a tile touches a side of an object's footprint: next to it straight on, not on a corner diagonal
	 * (an object is used from a side) and not under it.
	 */
	static boolean isBesideFootprint(int x, int y, int minX, int minY, int sizeX, int sizeY)
	{
		int maxX = minX + sizeX - 1;
		int maxY = minY + sizeY - 1;
		boolean nearX = x >= minX - 1 && x <= maxX + 1;
		boolean nearY = y >= minY - 1 && y <= maxY + 1;
		boolean overX = x >= minX && x <= maxX;
		boolean overY = y >= minY && y <= maxY;
		return nearX && nearY && (overX != overY);
	}

	/** The pending deposit's change has arrived: work out what went in and end the click. */
	private Multiset<Integer> finish(int tick)
	{
		Multiset<Integer> deposited = HashMultiset.create(Multisets.difference(inventoryBefore, inventoryNow));
		forgetStaleRemovals(tick);

		// Drops clicked ahead of the deposit went through inside this change (eight times in the recordings, the
		// hold one short of the fall each time); the drop sound, when sound is on, says the same of a drop the
		// clicks did not place first. At least one slot is the deposit's own.
		int inside = Math.min(removalsBeforeDeposit, deposited.size() - 1);
		if (inside == 0 && deposited.size() > 1 && !removalClicks.isEmpty()
			&& dropSoundTick != NONE && tick - dropSoundTick <= 1)
		{
			inside = 1;
		}
		for (int i = 0; i < inside && i < removalClicks.size(); i++)
		{
			if (!deposited.remove(removalClicks.get(i).itemId) && !deposited.isEmpty())
			{
				deposited.remove(deposited.iterator().next());
			}
		}

		// Any drop clicked after the deposit found nothing once the deposit landed (never both), so none of them
		// can be charged to the next deposit.
		removalClicks.clear();
		removalsBeforeDeposit = 0;
		ticksLeft = 0;
		landed = false;
		cancelTick = NONE;
		log.debug("quick deposit landed with {}", deposited);
		return deposited;
	}

	/** Removal clicks older than a tick whose items never left found nothing to remove. */
	private void forgetStaleRemovals(int tick)
	{
		while (!removalClicks.isEmpty() && tick - removalClicks.get(0).tick > 1)
		{
			removalsLanded(1);
		}
	}

	/** The oldest removal clicks are accounted for: their items have left the inventory, or never will. */
	private void removalsLanded(int count)
	{
		int n = Math.min(count, removalClicks.size());
		removalClicks.subList(0, n).clear();
		removalsBeforeDeposit = Math.max(0, removalsBeforeDeposit - n);
	}

}

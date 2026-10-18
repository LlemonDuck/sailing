package com.duckblade.osrs.sailing.features.facilities;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.junit.Assert;
import org.junit.Test;

/**
 * Each sequence below is a tick sequence recorded in game while Deposit-all, Drop and the hold's own contents
 * were watched together; the tick numbers in the comments are the recordings'.
 */
public class CargoHoldDepositWatchTest
{

	private static final int SALVAGE = 1;
	private static final int TOOL = 2;
	private static final int CRATE = 3;

	private final CargoHoldDepositWatch watch = new CargoHoldDepositWatch();
	private final Multiset<Integer> hold = HashMultiset.create();
	private int tick = 100;

	@Test
	public void theDepositIsCountedWhenItLandsAfterTheWalk()
	{
		deposit(5, false);
		ticks(CargoHoldDepositWatch.MAX_TICKS - 2);
		inventory(0);
		ticks(1);

		Assert.assertEquals(5, hold.count(SALVAGE));
		Assert.assertEquals(0, hold.count(TOOL));
		Assert.assertFalse(watch.isPending());
	}

	@Test
	public void onlyWhatLeftTheInventoryIsCounted()
	{
		Multiset<Integer> carried = inventoryOf(3);
		carried.add(CRATE);
		hold.addAll(watch.depositClicked(carried, false, tick));
		ticks(2);
		inventory(0);
		ticks(1);

		Assert.assertEquals(3, hold.count(SALVAGE));
		Assert.assertEquals(1, hold.count(CRATE));
		Assert.assertEquals(0, hold.count(TOOL));
	}

	@Test
	public void aDepositThatNeverShowsIsForgotten()
	{
		deposit(5, false);
		ticks(CargoHoldDepositWatch.MAX_TICKS);
		Assert.assertFalse(watch.isPending());

		// whatever empties the inventory later is not the deposit
		inventory(0);
		ticks(1);
		Assert.assertEquals(0, hold.size());
	}

	@Test
	public void aClickAwayFromTheHoldCallsTheDepositOffAfterATick()
	{
		deposit(5, false);
		watch.walkInterrupted(tick);
		ticks(2);
		inventory(0);
		ticks(2);

		Assert.assertEquals(0, hold.size());
	}

	@Test
	public void aClickJustAsTheDepositLandsDoesNotCallItOff()
	{
		// Deposit-all from afar, then Walk here in the tick the player reached the hold: the 22 go in (960-961).
		deposit(22, false);
		ticks(1);
		watch.walkInterrupted(tick);
		inventory(0);
		ticks(1);
		Assert.assertEquals(22, hold.size());

		// Deposit-all, then Drop a tick later, and the drop goes through: one slot on the floor, no deposit (380-381).
		deposit(22, false);
		ticks(1);
		drop(0);
		watch.walkInterrupted(tick);
		inventory(21);
		ticks(CargoHoldDepositWatch.MAX_TICKS);
		Assert.assertEquals(22, hold.size());

		// Deposit-all, then Drop as the player reaches the hold: the deposit wins and the drop finds nothing (344-345).
		deposit(21, false);
		ticks(1);
		drop(0);
		watch.walkInterrupted(tick);
		inventory(0);
		ticks(1);
		Assert.assertEquals(43, hold.size());
	}

	@Test
	public void besideTheHoldNothingCallsTheDepositOff()
	{
		// Deposit-all beside the hold, then Command, Walk here or Open in the same tick: the game already has the
		// deposit (four times in 25 minutes, 22 salvage each lost to a rule that let any click cancel).
		deposit(22, true);
		watch.walkInterrupted(tick);
		ticks(3);
		inventory(0);
		ticks(1);

		Assert.assertEquals(22, hold.size());
	}

	@Test
	public void aDropJustBeforeTheDepositIsNotTheDeposit()
	{
		// Drop, then Deposit-all in the same tick: the drop lands first, the 19 a tick later (872-873).
		inventory(20);
		ticks(1);
		drop(0);
		deposit(20, false);
		inventory(19);
		ticks(1);
		Assert.assertEquals(0, hold.size());
		inventory(0);
		ticks(1);
		Assert.assertEquals(19, hold.size());

		// Both in one change, with the drop sound: one of the 22 went to the floor (571, 580, 607).
		inventory(22);
		ticks(1);
		drop(0);
		deposit(22, false);
		watch.dropSoundPlayed(tick);
		inventory(0);
		ticks(1);
		Assert.assertEquals(40, hold.size());

		// The same with sound off: the drop was clicked ahead of the deposit, so it went through (547, 635, 664, 890).
		inventory(22);
		ticks(1);
		drop(0);
		deposit(22, false);
		inventory(0);
		ticks(1);
		Assert.assertEquals(61, hold.size());

		// A drop a tick earlier that landed on its own, then Deposit-all: nothing to take off (617-618).
		inventory(22);
		ticks(1);
		drop(0);
		inventory(21);
		ticks(1);
		deposit(21, false);
		inventory(0);
		ticks(1);
		Assert.assertEquals(82, hold.size());

		// Drop clicked after Deposit-all beside the hold, finding nothing: the full 22 count, and the stale drop
		// click is not charged to a deposit made later (344-345, then 370).
		inventory(22);
		ticks(1);
		deposit(22, true);
		drop(0);
		inventory(0);
		ticks(1);
		Assert.assertEquals(104, hold.size());
		inventory(22);
		ticks(20);
		deposit(22, true);
		inventory(0);
		ticks(1);
		Assert.assertEquals(126, hold.size());
	}

	@Test
	public void aSecondDepositClickInTheTickTheFirstLandsKeepsTheFirst()
	{
		// Deposit-all from afar; next tick a Drop, the 21 landing, and Deposit-all clicked again before the tick
		// counted the first (148-149).
		inventory(21);
		ticks(1);
		deposit(21, false);
		ticks(1);
		drop(0);
		watch.walkInterrupted(tick);
		inventory(0);
		deposit(0, false);
		ticks(1);
		Assert.assertEquals(21, hold.size());

		// That Drop found nothing, so a deposit a tick later gets its full count.
		inventory(22);
		deposit(22, true);
		inventory(0);
		ticks(1);
		Assert.assertEquals(43, hold.size());
	}

	@Test
	public void twoDropsAroundADepositFromAfarAreNotADeposit()
	{
		// Drop, Deposit-all from afar, Drop again; both drops landed in one change (21 -> 19) and the deposit never
		// happened (103-104). Then Drop and Deposit-all beside the hold: the drop inside the change, 18 in (114).
		inventory(21);
		ticks(1);
		drop(0);
		deposit(21, false);
		ticks(1);
		drop(1);
		watch.walkInterrupted(tick);
		inventory(19);
		ticks(CargoHoldDepositWatch.MAX_TICKS);
		Assert.assertEquals(0, hold.size());

		drop(2);
		deposit(19, true);
		inventory(0);
		ticks(1);
		Assert.assertEquals(18, hold.size());
	}

	@Test
	public void aRepeatDropClickOnTheSameSlotIsNotAnotherDrop()
	{
		// Drop spam-clicked, slot 23 twice; nine items left, then Deposit-all took the 11 that were left
		// (2351-2355). The repeat click found nothing and must not cut the deposit.
		inventory(14);
		ticks(1);
		drop(23);
		drop(23);
		drop(22);
		inventory(13);
		ticks(1);
		drop(15);
		deposit(13, false);
		inventory(11);
		ticks(1);
		inventory(0);
		ticks(1);

		Assert.assertEquals(11, hold.size());
	}

	@Test
	public void aPickupInTheTickOfTheDepositClickGoesInToo()
	{
		// Take, then Deposit-all, the pickup landing after the click; the deposit took all 4 (2383-2384).
		inventory(3);
		ticks(1);
		deposit(3, false);
		inventory(4);
		ticks(1);
		inventory(0);
		ticks(1);

		Assert.assertEquals(4, hold.size());
	}

	@Test
	public void aSecondDropAfterTheFirstLandedDoesNotCutTheDeposit()
	{
		// Drop (lands alone), Deposit-all, then another Drop a moment after the deposit: the first drop is spent, so
		// the whole deposit counts; the second finds nothing once the deposit has landed.
		inventory(22);
		ticks(1);
		drop(0);
		deposit(22, false);
		inventory(21);
		drop(1);
		inventory(0);
		ticks(1);

		Assert.assertEquals(21, hold.size());
	}

	@Test
	public void theDepositSoundKeepsAnInterruptedDepositAlive()
	{
		deposit(5, false);
		ticks(1);
		watch.walkInterrupted(tick);
		ticks(1);
		watch.depositSoundPlayed(tick);
		ticks(2);
		inventory(0);
		ticks(1);

		Assert.assertEquals(5, hold.size());
	}

	@Test
	public void theHoldBeingReadEndsThePendingDeposit()
	{
		deposit(5, false);
		ticks(1);
		watch.reset();
		inventory(0);
		ticks(1);

		Assert.assertEquals(0, hold.size());
		Assert.assertFalse(watch.isPending());
	}

	@Test
	public void besideTheHoldMeansNextToASideOfIt()
	{
		// a 2x1 hold at scene (10, 20)
		Assert.assertTrue(CargoHoldDepositWatch.isBesideFootprint(9, 20, 10, 20, 2, 1));
		Assert.assertTrue(CargoHoldDepositWatch.isBesideFootprint(12, 20, 10, 20, 2, 1));
		Assert.assertTrue(CargoHoldDepositWatch.isBesideFootprint(11, 21, 10, 20, 2, 1));
		Assert.assertTrue(CargoHoldDepositWatch.isBesideFootprint(10, 19, 10, 20, 2, 1));

		// corners, under it and further off are not
		Assert.assertFalse(CargoHoldDepositWatch.isBesideFootprint(9, 21, 10, 20, 2, 1));
		Assert.assertFalse(CargoHoldDepositWatch.isBesideFootprint(12, 19, 10, 20, 2, 1));
		Assert.assertFalse(CargoHoldDepositWatch.isBesideFootprint(10, 20, 10, 20, 2, 1));
		Assert.assertFalse(CargoHoldDepositWatch.isBesideFootprint(8, 20, 10, 20, 2, 1));
		Assert.assertFalse(CargoHoldDepositWatch.isBesideFootprint(11, 22, 10, 20, 2, 1));
	}

	private static Multiset<Integer> inventoryOf(int salvage)
	{
		Multiset<Integer> inventory = HashMultiset.create();
		inventory.add(TOOL);
		inventory.add(SALVAGE, salvage);
		return inventory;
	}

	/** The inventory now holds the tool and this much salvage. */
	private void inventory(int salvage)
	{
		watch.inventoryChanged(inventoryOf(salvage), tick);
	}

	/** A deposit option clicked on the hold while carrying the tool and this much salvage. */
	private void deposit(int salvageCarried, boolean beside)
	{
		hold.addAll(watch.depositClicked(inventoryOf(salvageCarried), beside, tick));
	}

	private void drop(int slot)
	{
		watch.itemRemovalClicked(slot, SALVAGE, tick);
	}

	private void ticks(int count)
	{
		for (int i = 0; i < count; i++)
		{
			tick++;
			hold.addAll(watch.tick(tick));
		}
	}

}

package com.duckblade.osrs.sailing.features.facilities;

import net.runelite.api.MenuAction;
import org.junit.Assert;
import org.junit.Test;

public class CargoHoldTrackerTest
{

	@Test
	public void gameWorldAndItemClicksInterruptTheWalkToTheHold()
	{
		Assert.assertTrue(CargoHoldTracker.interruptsWalk(MenuAction.WALK, -1, "Walk here"));
		Assert.assertTrue(CargoHoldTracker.interruptsWalk(MenuAction.GAME_OBJECT_FIRST_OPTION, -1, "Open"));
		Assert.assertTrue(CargoHoldTracker.interruptsWalk(MenuAction.NPC_FIRST_OPTION, -1, "Command"));
		Assert.assertTrue(CargoHoldTracker.interruptsWalk(MenuAction.GROUND_ITEM_THIRD_OPTION, 1234, "Take"));
		Assert.assertTrue(CargoHoldTracker.interruptsWalk(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, 1234, "Use"));
		Assert.assertTrue(CargoHoldTracker.interruptsWalk(MenuAction.CC_OP, 1234, "Drop"));
		Assert.assertTrue(CargoHoldTracker.interruptsWalk(MenuAction.CC_OP_LOW_PRIORITY, 1234, "Drop"));
	}

	@Test
	public void interfaceClicksDoNotInterruptTheWalkToTheHold()
	{
		Assert.assertFalse(CargoHoldTracker.interruptsWalk(MenuAction.CC_OP, -1, "Inventory"));
		Assert.assertFalse(CargoHoldTracker.interruptsWalk(MenuAction.CC_OP_LOW_PRIORITY, -1, "Toggle"));
		Assert.assertFalse(CargoHoldTracker.interruptsWalk(MenuAction.WIDGET_TARGET, 1234, "Use"));
		Assert.assertFalse(CargoHoldTracker.interruptsWalk(MenuAction.CANCEL, -1, "Cancel"));
		Assert.assertFalse(CargoHoldTracker.interruptsWalk(MenuAction.GAME_OBJECT_FIRST_OPTION, -1, "Examine"));
		Assert.assertFalse(CargoHoldTracker.interruptsWalk(MenuAction.EXAMINE_OBJECT, -1, "Examine"));
		Assert.assertFalse(CargoHoldTracker.interruptsWalk(MenuAction.RUNELITE, -1, "Mark tile"));
		Assert.assertFalse(CargoHoldTracker.interruptsWalk(MenuAction.RUNELITE_OVERLAY, -1, "Configure"));
	}

	@Test
	public void dropEatAndAlchemyEmptyAnInventorySlot()
	{
		Assert.assertTrue(CargoHoldTracker.isItemRemoval("Drop", ""));
		Assert.assertTrue(CargoHoldTracker.isItemRemoval("Eat", "<col=ff9040>Shark</col>"));
		Assert.assertTrue(CargoHoldTracker.isItemRemoval(
			"Cast",
			"<col=00ff00>High Level Alchemy</col><col=ffffff> -> </col><col=ff9040>Salvage</col>"
		));

		Assert.assertFalse(CargoHoldTracker.isItemRemoval("Cast", "<col=00ff00>Bones to Bananas</col>"));
		Assert.assertFalse(CargoHoldTracker.isItemRemoval("Use", "<col=ff9040>Salvage</col>"));
		Assert.assertFalse(CargoHoldTracker.isItemRemoval("Deposit-all", "<col=ffff>Cargo hold</col>"));
		Assert.assertFalse(CargoHoldTracker.isItemRemoval("Wield", "<col=ff9040>Spyglass</col>"));
	}

}

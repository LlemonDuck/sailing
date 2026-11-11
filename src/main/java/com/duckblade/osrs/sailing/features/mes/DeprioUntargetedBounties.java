package com.duckblade.osrs.sailing.features.mes;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.CourierTaskUtil;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.DBTableID;
import net.runelite.client.eventbus.Subscribe;

/**
 * Deprioritizes the Attack option on bounty task monsters that none of the
 * five task slots currently asks for.
 * <p>
 * The PortTask table names a bounty target for every bounty task, so the union
 * of those targets is the set of NPCs this can apply to. Everything outside
 * that set, including every ordinary monster, is left alone.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DeprioUntargetedBounties
	implements PluginLifecycleComponent
{

	private static final String OPTION_ATTACK = "Attack";

	private static final Set<MenuAction> NPC_OPTIONS = Set.of(
		MenuAction.NPC_FIRST_OPTION,
		MenuAction.NPC_SECOND_OPTION,
		MenuAction.NPC_THIRD_OPTION,
		MenuAction.NPC_FOURTH_OPTION,
		MenuAction.NPC_FIFTH_OPTION
	);

	private final Client client;

	// task id (PortTask COL_TASK_ID) -> the NPC ids that task wants killed
	private final Map<Integer, Set<Integer>> taskTargets = new HashMap<>();
	// every bounty target in the table, which scopes what this can deprioritize
	private final Set<Integer> allTargets = new HashSet<>();
	private final Set<Integer> activeTargets = new HashSet<>();

	private boolean tableLoaded;
	private boolean slotsDirty = true;

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.deprioUntargetedBounties();
	}

	@Override
	public void shutDown()
	{
		taskTargets.clear();
		allTargets.clear();
		activeTargets.clear();
		tableLoaded = false;
		slotsDirty = true;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		loadTable();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (CourierTaskUtil.TASK_SLOT_IDS.contains(e.getVarbitId()))
		{
			slotsDirty = true;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (!SailingUtil.isSailing(client) ||
			!OPTION_ATTACK.equals(e.getOption()) ||
			!NPC_OPTIONS.contains(e.getMenuEntry().getType()))
		{
			return;
		}

		NPC npc = e.getMenuEntry().getNpc();
		if (npc == null)
		{
			return;
		}

		if (!allTargets.contains(npc.getId()))
		{
			return;
		}

		updateActiveTargets();
		if (activeTargets.isEmpty() || activeTargets.contains(npc.getId()))
		{
			return;
		}

		// Push the option down instead of removing it
		e.getMenuEntry().setDeprioritized(true);
	}

	private void loadTable()
	{
		if (tableLoaded)
		{
			return;
		}

		List<Integer> rows = client.getDBTableRows(DBTableID.PortTask.ID);
		if (rows == null || rows.isEmpty())
		{
			return;
		}

		for (int row : rows)
		{
			Set<Integer> targets = new HashSet<>();
			collectColumn(targets, row, DBTableID.PortTask.COL_BOUNTY_TARGET_ALIVE);
			collectColumn(targets, row, DBTableID.PortTask.COL_BOUNTY_TARGET_DEAD);
			if (targets.isEmpty())
			{
				continue;
			}

			Object[] taskId = client.getDBTableField(row, DBTableID.PortTask.COL_TASK_ID, 0);
			if (taskId == null || taskId.length == 0 || !(taskId[0] instanceof Integer))
			{
				continue;
			}

			taskTargets.put((Integer) taskId[0], targets);
			allTargets.addAll(targets);
		}

		log.debug("Loaded {} bounty tasks covering {} npcs", taskTargets.size(), allTargets.size());
		tableLoaded = true;
	}

	private void collectColumn(Set<Integer> targets, int row, int column)
	{
		Object[] values = client.getDBTableField(row, column, 0);
		if (values == null)
		{
			return;
		}

		for (Object value : values)
		{
			if (value instanceof Integer)
			{
				targets.add((Integer) value);
			}
		}
	}

	private void updateActiveTargets()
	{
		if (!slotsDirty)
		{
			return;
		}

		activeTargets.clear();
		for (int slot : CourierTaskUtil.TASK_SLOT_IDS)
		{
			Set<Integer> targets = taskTargets.get(client.getVarbitValue(slot));
			if (targets != null)
			{
				activeTargets.addAll(targets);
			}
		}

		slotsDirty = false;
	}
}

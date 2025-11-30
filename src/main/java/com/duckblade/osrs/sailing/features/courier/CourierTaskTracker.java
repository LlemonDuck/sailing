package com.duckblade.osrs.sailing.features.courier;

import com.duckblade.osrs.sailing.features.util.CourierTaskUtil;
import com.duckblade.osrs.sailing.model.CourierTask;
import com.duckblade.osrs.sailing.model.Port;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.DBTableID;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CourierTaskTracker
	implements PluginLifecycleComponent
{

	private static final Set<Integer> LEDGER_TABLE_IDS = Arrays.stream(Port.values()).map(Port::getLedgerTableID).collect(Collectors.toSet());

	private final Client client;

	@Getter
	private final Set<CourierTask> tasks = new HashSet<>();

	@Getter
	private GameObject activeLedger;

	@Getter
	private Port activePort;

	public void shutDown()
	{
		activePort = null;
		activeLedger = null;
		tasks.clear();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		int id = e.getGameObject().getId();
		if (LEDGER_TABLE_IDS.contains(id))
		{
			log.debug("Found ledger table at {}", e.getGameObject().getLocalLocation());
			activeLedger = e.getGameObject();
			activePort = Port.findByLedgerTableID(id);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		int id = e.getGameObject().getId();
		if (LEDGER_TABLE_IDS.contains(id))
		{
			activeLedger = null;
			activePort = null;
		}
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		if (e.getWorldView().isTopLevel())
		{
			activeLedger = null;
			activePort = null;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (CourierTaskUtil.ALL_COURIER_TASK_VARBITS.contains(e.getVarbitId()))
		{
			updateTasks();
		}
	}

	private void updateTasks()
	{
		tasks.clear();
		for (int i = 0; i < CourierTaskUtil.TASK_SLOT_IDS.size(); i++)
		{
			CourierTask task = this.getTaskInfo(i);
			if (task != null)
			{
				log.debug("Tracking task {}: {}", i, task);
				tasks.add(task);
			}
		}
	}

	public CourierTask getTaskInfo(int taskIndex)
	{
		int taskID = client.getVarbitValue(CourierTaskUtil.TASK_SLOT_IDS.get(taskIndex));
		List<Integer> taskRow = client.getDBRowsByValue(DBTableID.PortTask.ID, DBTableID.PortTask.COL_TASK_ID, 0, taskID);

		if (taskRow.isEmpty())
		{
			return null;
		}

		Integer rowID = taskRow.get(0);
		int taskType = (int) client.getDBTableField(rowID, DBTableID.PortTask.COL_TASK_TYPE, 0)[0];
		if (taskType != CourierTaskUtil.TaskType.DELIVERY.ordinal())
		{
			return null;
		}

		int fromPortID = (int) client.getDBTableField(rowID, DBTableID.PortTask.COL_CARGO_PORT, 0)[0];
		int toPortID = (int) client.getDBTableField(rowID, DBTableID.PortTask.COL_ENDING_PORT, 0)[0];
		int cargoAmount = (int) client.getDBTableField(rowID, DBTableID.PortTask.COL_CARGO, 1)[0];
		int cargoCrateItemID = (int) client.getDBTableField(rowID, DBTableID.PortTask.COL_CARGO, 0)[0];

		Port fromPort = Port.findByID(fromPortID);
		Port toPort = Port.findByID(toPortID);

		int numRetrieved = client.getVarbitValue(CourierTaskUtil.CARGO_RETRIEVED_VARBITS.get(taskIndex));
		int numDelivered = client.getVarbitValue(CourierTaskUtil.CARGO_DELIVERED_VARBITS.get(taskIndex));

		return new CourierTask(taskIndex, fromPort, toPort, cargoCrateItemID, cargoAmount, numRetrieved, numDelivered);
	}

	public List<CourierTask> getDropOffTasksForPort(Port port)
	{
		return tasks.stream()
			.filter(task -> task.getToPort() == port)
			.collect(Collectors.toList());
	}

	public List<CourierTask> getPickupTasksForPort(Port port)
	{
		return tasks.stream()
			.filter(task -> task.getFromPort() == port)
			.collect(Collectors.toList());
	}

	public CourierTask getTaskForItemID(int itemID)
	{
		return tasks.stream()
			.filter(task -> task.getCargoCrateItemID() == itemID)
			.findFirst()
			.orElse(null);
	}
}

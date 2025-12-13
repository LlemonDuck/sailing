package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;

@Slf4j
@Singleton
public class SeaChartTaskIndex implements PluginLifecycleComponent
{

	private static final int SEARCH_DIST_GAME_OBJECT = 5;
	private static final int SEARCH_DIST_NPC = 5;

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	private final Map<WorldPoint, SeaChartTask> tasksByLocation = new HashMap<>();
	private final Map<Integer, List<SeaChartTask>> tasksByGameObject = new HashMap<>();
	private final Map<Integer, List<SeaChartTask>> tasksByNpc = new HashMap<>();

	public void startUp()
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.getLocation() != null)
			{
				tasksByLocation.put(task.getLocation(), task);
			}
			if (task.getObjectId() != -1)
			{
				tasksByGameObject.computeIfAbsent(task.getObjectId(), (k) -> new ArrayList<>(1)).add(task);
			}
			else if (task.getNpcId() != -1)
			{
				tasksByNpc.computeIfAbsent(task.getNpcId(), (k) -> new ArrayList<>(1)).add(task);
			}
		}
	}

	public void shutDown()
	{
		tasksByLocation.clear();
		tasksByGameObject.clear();
		tasksByNpc.clear();
	}

	public SeaChartTask findTask(GameObject obj)
	{
		List<SeaChartTask> tasks = tasksByGameObject.get(obj.getId());
		if (tasks == null || tasks.isEmpty())
		{
			return null;
		}
		if (tasks.size() == 1)
		{
			return tasks.get(0);
		}

		WorldPoint wp = obj.getWorldLocation();
		SeaChartTask task = tasksByLocation.get(wp);
		if (task != null)
		{
			return task;
		}

		task = findTask(wp);
		if (task != null)
		{
			return task;
		}

		task = findTask(wp, SEARCH_DIST_GAME_OBJECT, t -> t.getObjectId() == obj.getId());
		if (task != null)
		{
			log.debug("scan found task for game object {} @ {} = task {}", obj.getId(), obj.getWorldLocation(), task.getTaskId());
			return task;
		}

		log.warn("No task found for game object {} @ {}", obj.getId(), obj.getWorldLocation());
		return null;
	}

	public SeaChartTask findTask(NPC npc)
	{
		List<SeaChartTask> tasks = tasksByNpc.get(npc.getId());
		if (tasks == null || tasks.isEmpty())
		{
			return null;
		}
		if (tasks.size() == 1)
		{
			return tasks.get(0);
		}

		WorldPoint wp = npc.getWorldLocation();
		SeaChartTask task = tasksByLocation.get(wp);
		if (task != null)
		{
			return task;
		}

		task = findTask(wp);
		if (task != null)
		{
			return task;
		}

		task = findTask(wp, SEARCH_DIST_NPC, t -> t.getNpcId() == npc.getId());
		if (task != null)
		{
			log.debug("scan found task for npc {} @ {} = task {}", npc.getId(), npc.getWorldLocation(), task.getTaskId());
			return task;
		}

		log.warn("No task found for npc {} @ {}", npc.getId(), npc.getWorldLocation());
		return null;
	}

	public SeaChartTask findTask(WorldPoint wp)
	{
		return findTask(wp, 1);
	}

	public SeaChartTask findTask(WorldPoint wp, int distance)
	{
		return findTask(wp, distance, t -> true);
	}

	public SeaChartTask findTask(WorldPoint wp, int distance, Predicate<SeaChartTask> filter)
	{
		for (int x = -distance; x <= distance; x++)
		{
			for (int y = -distance; y <= distance; y++)
			{
				SeaChartTask nearby = tasksByLocation.get(new WorldPoint(wp.getX() + x, wp.getY() + y, 0));
				if (nearby != null && filter.test(nearby))
				{
					return nearby;
				}
			}
		}

		return null;
	}

	public BufferedImage getTaskSprite(SeaChartTask task)
	{
		return itemManager.getImage(task.getType().getIconItem());
	}

	public Quest getTaskQuestRequirement(SeaChartTask task)
	{
		switch (task.getType())
		{
			case CURRENT_DUCK:
				return Quest.CURRENT_AFFAIRS;
			case DRINK_CRATE:
				return Quest.PRYING_TIMES;
			case MERMAID_GUIDE:
				return Quest.RECIPE_FOR_DISASTER__PIRATE_PETE;
			default:
				return Quest.PANDEMONIUM;
		}
	}

	public boolean hasTaskRequirement(SeaChartTask task)
	{
		var questRequirement = getTaskQuestRequirement(task);
		if (questRequirement.getState(client) != QuestState.FINISHED)
		{
			return false;
		}

		return client.getRealSkillLevel(Skill.SAILING) >= task.getLevel();
	}
}

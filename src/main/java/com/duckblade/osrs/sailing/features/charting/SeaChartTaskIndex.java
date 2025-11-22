package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.game.ItemManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class SeaChartTaskIndex implements PluginLifecycleComponent
{

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
                tasksByGameObject.computeIfAbsent(task.getObjectId(), ArrayList::new).add(task);
            }
            else if (task.getNpcId() != -1)
            {
                tasksByNpc.computeIfAbsent(task.getNpcId(), ArrayList::new).add(task);
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

        for (int x = -5; x <= 5; x++)
        {
            for (int y = -5; y <= 5; y++)
            {
                SeaChartTask nearby = tasksByLocation.get(new WorldPoint(wp.getX() + x, wp.getY() + y, 0));
                if (nearby != null && nearby.getObjectId() == obj.getId())
                {
                    log.debug("scan happened for game object {} @ {} = task {}", obj.getId(), obj.getWorldLocation(), nearby.getTaskId());
                    return nearby;
                }
            }
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

        for (int x = -5; x <= 5; x++)
        {
            for (int y = -5; y <= 5; y++)
            {
                SeaChartTask nearby = tasksByLocation.get(new WorldPoint(wp.getX() + x, wp.getY() + y, 0));
                if (nearby != null && nearby.getNpcId() == npc.getId())
                {
                    log.debug("scan required for game object {} @ {} = task {}", npc.getId(), npc.getWorldLocation(), nearby.getTaskId());
                    return nearby;
                }
            }
        }

        log.warn("No task found for game object {} @ {}", npc.getId(), npc.getWorldLocation());
        return null;
    }

    public BufferedImage getTaskSprite(SeaChartTask task)
    {
        switch (task.getObjectId())
        {
            case ObjectID.SAILING_CHARTING_HINT_MARKER_SPYGLASS:
                return itemManager.getImage(ItemID.SAILING_CHARTING_SPYGLASS);

            case ObjectID.SAILING_CHARTING_HINT_MARKER_DUCK:
                return itemManager.getImage(ItemID.SAILING_CHARTING_CURRENT_DUCK);

            case ObjectID.SAILING_CHARTING_DRINK_CRATE:
                return itemManager.getImage(ItemID.SAILING_CHARTING_CROWBAR);

            case -1:
                break;

            default:
                return itemManager.getImage(ItemID.SAILING_LOG_INITIAL);
        }

        switch (task.getNpcId())
        {
            case NpcID.SAILING_CHARTING_MERMAID_GUIDE_1:
            case NpcID.SAILING_CHARTING_MERMAID_GUIDE_2:
            case NpcID.SAILING_CHARTING_MERMAID_GUIDE_3:
            case NpcID.SAILING_CHARTING_MERMAID_GUIDE_4:
            case NpcID.SAILING_CHARTING_MERMAID_GUIDE_5:
                return itemManager.getImage(ItemID.HUNDRED_PIRATE_DIVING_HELMET);

            case NpcID.SAILING_CHARTING_WEATHER_TROLL:
                return itemManager.getImage(ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY);

            default:
                return itemManager.getImage(ItemID.SAILING_LOG_INITIAL);
        }
    }
}

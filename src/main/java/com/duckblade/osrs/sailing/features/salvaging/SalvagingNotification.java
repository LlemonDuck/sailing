package com.duckblade.osrs.sailing.features.salvaging;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.Notifier;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SalvagingNotification implements PluginLifecycleComponent
{

    private static final ImmutableSet<Integer> SALVAGE_WRECKS = ImmutableSet.of(
            ObjectID.SAILING_SMALL_SHIPWRECK,
            ObjectID.SAILING_SMALL_SHIPWRECK_STUMP,
            ObjectID.SAILING_FISHERMAN_SHIPWRECK,
            ObjectID.SAILING_FISHERMAN_SHIPWRECK_STUMP,
            ObjectID.SAILING_BARRACUDA_SHIPWRECK,
            ObjectID.SAILING_BARRACUDA_SHIPWRECK_STUMP,
            ObjectID.SAILING_LARGE_SHIPWRECK,
            ObjectID.SAILING_LARGE_SHIPWRECK_STUMP,
            ObjectID.SAILING_PIRATE_SHIPWRECK,
            ObjectID.SAILING_PIRATE_SHIPWRECK_STUMP,
            ObjectID.SAILING_MERCENARY_SHIPWRECK,
            ObjectID.SAILING_MERCENARY_SHIPWRECK_STUMP,
            ObjectID.SAILING_FREMENNIK_SHIPWRECK,
            ObjectID.SAILING_FREMENNIK_SHIPWRECK_STUMP,
            ObjectID.SAILING_MERCHANT_SHIPWRECK,
            ObjectID.SAILING_MERCHANT_SHIPWRECK_STUMP
    );

    private final Client client;
    private final SailingConfig config;
    private final Notifier notifier;

    @Getter
    private boolean playerSalvaging = false;
    @Getter
    private boolean cargoFull = false;

    private boolean playerSorting = false;
    private boolean playerSalvageTracker = false;
    private boolean playerSortTracker = false;
    private boolean crewSalvageTracker = false;
    private boolean fullCargoTracker = false;

    @Getter
    private final Set<Actor> crewmates = new HashSet<>();

    @Getter
    private final HashMap<Actor, Boolean> crewSalvaging = new HashMap<>();
    private final Set<String> crewNames = new HashSet<>();
    private final Set<GameObject> wrecks = new HashSet<>();
    private final HashMap<Actor, Integer> crewIdleTicks = new HashMap<>();

    @Override
    public boolean isEnabled(SailingConfig config)
    {
        boolean activeNotifCrewStop = config.salvagingNotifCrewStop().isEnabled();
        boolean activeNotifCrewStart = config.salvagingNotifCrewStart().isEnabled();
        boolean activeNotifPlayerStop = config.salvagingNotifPlayerStop().isEnabled();
        boolean activeNotifSortStop = config.salvagingNotifSortStop().isEnabled();
        boolean activeNotifCargoFull = config.salvagingNotifCargoFull().isEnabled();

        return
                activeNotifCrewStop || activeNotifCrewStart ||
                activeNotifPlayerStop || activeNotifSortStop ||
                activeNotifCargoFull;
    }

    public boolean atSalvage()
    {
        return SailingUtil.isSailing(client) && !wrecks.isEmpty();
    }

    @Subscribe
    // Sometimes it doesn't call NpcDespawn on a world hop, so we manually clear them
    public void onGameStateChanged(GameStateChanged state)
    {
        if (state.getGameState() == GameState.LOGGING_IN || state.getGameState() == GameState.HOPPING)
        {
            crewSalvaging.clear();
            crewIdleTicks.clear();
            crewmates.clear();
            playerSalvageTracker = false;
            playerSortTracker = false;
            crewSalvageTracker = false;
            fullCargoTracker = false;
        }
    }

    @Subscribe
    // Unloaded world views don't always call the ObjectDespawn event, so make sure we clear that
    public void onWorldViewUnloaded(WorldViewUnloaded e)
    {
        if (e.getWorldView().isTopLevel())
        {
            wrecks.clear();
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        NPC npc = event.getNpc();
        if (!isCrewName(npc.getName()) || npc.getWorldView() != client.getLocalPlayer().getWorldView())
        {
            return;
        }

        if (!crewmates.contains(npc)) {
            log.debug("Found Crewmate: {}, ID: {}", npc.getName(), npc.getId());
            crewmates.add(npc);
            crewSalvaging.put(npc, false);
            crewIdleTicks.put(npc, 0);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        crewmates.remove(npc);
        crewSalvaging.remove(npc);
        crewIdleTicks.remove(npc);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned obj)
    {
        int objId = obj.getGameObject().getId();
        if (SALVAGE_WRECKS.contains(objId) && !wrecks.contains(obj.getGameObject()))
        {
            log.debug("Adding Shipwreck with ID: {}", objId);
            wrecks.add(obj.getGameObject());
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned obj)
    {
        wrecks.remove(obj.getGameObject());
    }

    @Subscribe
    //Tracking crew idle ticks so we don't spam notifs when wrecks swap, cap at 10 so it doesn't overflow
    public void onGameTick(GameTick tick)
    {
        for (Actor crew : crewmates)
        {
            int currentIdle = crewIdleTicks.get(crew);
            if (!crewSalvaging.get(crew) && currentIdle < 10)
            {
                crewIdleTicks.replace(crew, currentIdle + 1);
                handleSalvageUpdate();
            }
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged animationChanged)
    {
        final Actor actor = animationChanged.getActor();
        final int anim = actor.getAnimation();

        if (actor.getWorldView().isTopLevel())
        {
            return;
        }

        if (SailingUtil.isLocalPlayer(client, actor))
        {
            playerSalvaging = isAnimationSalvaging(anim);
            playerSorting = (anim == 13599);
        }
        else if (crewmates.contains(actor))
        {
            if (isAnimationSalvaging(anim))
            {
                if (cargoFull)
                {
                    cargoFull = false;
                }
                crewSalvaging.replace(actor, true);
            }
            else
            {
                crewSalvaging.replace(actor, false);
                crewIdleTicks.replace(actor, 0);
            }
        }
        handleSalvageUpdate();
    }

    @Subscribe
    public void onChatMessage(ChatMessage msg)
    {
        if (msg.getType() != ChatMessageType.SPAM)
        {
            return;
        }
        if (msg.getMessage().equals("Your crewmate on the salvaging hook cannot salvage as the cargo hold is full."))
        {
            cargoFull = true;
            handleSalvageUpdate();
        }
    }

    private void handleSalvageUpdate()
    {
        //Player state has updated
        if (playerSalvaging != playerSalvageTracker)
        {
            if (!playerSalvaging && atSalvage())
            {
                notifier.notify(config.salvagingNotifPlayerStop(), "Salvaging: Player stopped salvaging");
            }
            playerSalvageTracker = playerSalvaging;
        }

        boolean crewSalvage = crewSalvaging.values().stream().anyMatch(b -> b);
        //Crew salvage state has updated
        if (crewSalvage != crewSalvageTracker)
        {
            if (crewSalvage)
            {
                if(atSalvage())
                {
                    notifier.notify(config.salvagingNotifCrewStart(), "Salvaging: Crew started salvaging");
                }
                crewSalvageTracker = true;
            }
            else
            {
                if(maxCrewIdleTicks() > 3)
                {
                    if(atSalvage())
                    {
                        notifier.notify(config.salvagingNotifCrewStop(), "Salvaging: Crew stopped salvaging");
                    }
                    crewSalvageTracker = false;
                }
            }
        }

        //Sorting state has updated
        if (playerSorting != playerSortTracker)
        {
            if (!playerSorting && atSalvage())
            {
                notifier.notify(config.salvagingNotifSortStop(), "Salvaging: Player stopped sorting salvage");
            }
            playerSortTracker = playerSorting;
        }

        if(cargoFull != fullCargoTracker)
        {
            if (cargoFull && atSalvage())
            {
                notifier.notify(config.salvagingNotifCargoFull(), "Salvaging: Full cargo");
            }
            fullCargoTracker = cargoFull;
        }
    }

    private boolean isAnimationSalvaging(int anim)
    {
        return anim == 13576 || anim == 13577 || anim == 13584 || anim == 13583;
    }

    private boolean isCrewName(String name)
    {
        if (crewNames.isEmpty())
        {
            populateCrewNameList();
        }
        return crewNames.contains(name);
    }

    private void populateCrewNameList()
    {
        List<Integer> rows = client.getDBTableRows(DBTableID.SailingCrew.ID);
        for (int row : rows)
        {
            Object[] npcs = client.getDBTableField(row, DBTableID.SailingCrew.COL_CARGO_NPC, 0);
            for (Object npc : npcs)
            {
                NPCComposition npcDef = client.getNpcDefinition((int) npc);
                crewNames.add(npcDef.getName());
            }
        }
    }

    //Get the maximum idle tick of a crewmate that isn't at the max
    private int maxCrewIdleTicks()
    {
        int max = 0;
        for (int i : crewIdleTicks.values())
        {
            if (i > max && i != 10)
            {
                max = i;
            }
        }
        return max;
    }


}
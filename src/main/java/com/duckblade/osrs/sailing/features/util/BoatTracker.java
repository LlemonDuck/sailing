package com.duckblade.osrs.sailing.features.util;

import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.model.CargoHoldTier;
import com.duckblade.osrs.sailing.model.HelmTier;
import com.duckblade.osrs.sailing.model.HullTier;
import com.duckblade.osrs.sailing.model.SailTier;
import com.duckblade.osrs.sailing.model.SalvagingHookTier;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.WorldEntityDespawned;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BoatTracker
	implements PluginLifecycleComponent
{

	private final Map<Integer, Boat> trackedBoats = new HashMap<>();
	private final Client client;

	public void shutDown()
	{
		trackedBoats.clear();
	}

	@Subscribe
	public void onWorldEntitySpawned(WorldEntitySpawned e)
	{
		WorldEntity we = e.getWorldEntity();
		if (SailingUtil.WORLD_ENTITY_TYPE_BOAT.contains(we.getConfig().getId()))
		{
			int wvId = we.getWorldView().getId();
			log.trace("tracking boat in wv {}", wvId);
			trackedBoats.put(wvId, new Boat(wvId, we));
		}
	}

	@Subscribe
	public void onWorldEntityDespawned(WorldEntityDespawned e)
	{
		if (trackedBoats.remove(e.getWorldEntity().getWorldView().getId()) != null)
		{
			log.trace("removed tracking boat from wv {}", e.getWorldEntity().getWorldView().getId());
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();
		Boat boat = getBoat(o.getWorldView().getId());
		if (boat == null)
		{
			return;
		}

		if (HullTier.fromGameObjectId(o.getId()) != null)
		{
			boat.setHull(o);
			log.trace("found hull {}={}+{} for boat in wv {}", o.getId(), boat.getHullTier(), boat.getSizeClass(), boat.getWorldViewId());
		}
		if (SailTier.fromGameObjectId(o.getId()) != null)
		{
			boat.setSail(o);
			log.trace("found sail {}={} for boat in wv {}", o.getId(), boat.getSailTier(), boat.getWorldViewId());
		}
		if (HelmTier.fromGameObjectId(o.getId()) != null)
		{
			boat.setHelm(o);
			log.trace("found helm {}={} for boat in wv {}", o.getId(), boat.getHelmTier(), boat.getWorldViewId());
		}
		if (SalvagingHookTier.fromGameObjectId(o.getId()) != null && boat.getSalvagingHooks().add(o))
		{
			log.trace("found salvaging hook {}={} for boat in wv {}", o.getId(), SalvagingHookTier.fromGameObjectId(o.getId()), boat.getWorldViewId());
		}
		if (CargoHoldTier.fromGameObjectId(o.getId()) != null)
		{
			boat.setCargoHold(o);
			log.trace("found cargo hold {}={} for boat in wv {}", o.getId(), boat.getCargoHoldTier(), boat.getWorldViewId());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		GameObject o = e.getGameObject();
		Boat boat = getBoat(o.getWorldView().getId());
		if (boat == null)
		{
			return;
		}

		if (boat.getHull() == o)
		{
			boat.setHull(null);
			log.trace("unsetting hull for boat in wv {}", boat.getWorldViewId());
		}
		if (boat.getSail() == o)
		{
			boat.setSail(null);
			log.trace("unsetting sail for boat in wv {}", boat.getWorldViewId());
		}
		if (boat.getHelm() == o)
		{
			boat.setHelm(null);
			log.trace("unsetting helm for boat in wv {}", boat.getWorldViewId());
		}
		if (boat.getSalvagingHooks().remove(o))
		{
			log.trace("unsetting salvaging hook for boat in wv {}", boat.getWorldViewId());
		}
		if (boat.getCargoHold() == o)
		{
			boat.setCargoHold(null);
			log.trace("unsetting cargo hold for boat in wv {}", boat.getWorldViewId());
		}
	}

	public Boat getBoat()
	{
		return getBoat(client.getLocalPlayer().getWorldView().getId());
	}

	public Boat getBoat(int wvId)
	{
		if (wvId == -1)
		{
			return null;
		}

		return trackedBoats.get(wvId);
	}
}

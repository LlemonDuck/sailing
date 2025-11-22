package com.duckblade.osrs.sailing;

import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.model.CargoHoldTier;
import com.duckblade.osrs.sailing.model.HelmTier;
import com.duckblade.osrs.sailing.model.HullTier;
import com.duckblade.osrs.sailing.model.SailTier;
import com.duckblade.osrs.sailing.model.SalvagingHookTier;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.WorldEntity;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WorldEntityDespawned;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BoatTracker
{

	private static final int WORLD_ENTITY_TYPE_BOAT = 2;

	private final Map<Integer, Boat> trackedBoats = new HashMap<>();

	public void shutDown()
	{
		trackedBoats.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOADING)
		{
			trackedBoats.clear();
		}
	}

	@Subscribe
	public void onWorldEntitySpawned(WorldEntitySpawned e)
	{
		WorldEntity we = e.getWorldEntity();
		if (we.getConfig().getId() == WORLD_ENTITY_TYPE_BOAT)
		{
			int wvId = we.getWorldView().getId();
			log.debug("tracking boat in wv {}", wvId);
			trackedBoats.put(wvId, new Boat(wvId));
		}
	}

	@Subscribe
	public void onWorldEntityDespawned(WorldEntityDespawned e)
	{
		trackedBoats.remove(e.getWorldEntity().getWorldView().getId());
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
		}
		if (SailTier.fromGameObjectId(o.getId()) != null)
		{
			boat.setSail(o);
		}
		if (HelmTier.fromGameObjectId(o.getId()) != null)
		{
			boat.setHelm(o);
		}
		if (SalvagingHookTier.fromGameObjectId(o.getId()) != null)
		{
			boat.setSalvagingHook(o);
		}
		if (CargoHoldTier.fromGameObjectId(o.getId()) != null)
		{
			boat.setCargoHold(o);
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
		}
		if (boat.getSail() == o)
		{
			boat.setSail(null);
		}
		if (boat.getHelm() == o)
		{
			boat.setHelm(null);
		}
		if (boat.getSalvagingHook() == o)
		{
			boat.setSalvagingHook(null);
		}
		if (boat.getCargoHold() == o)
		{
			boat.setCargoHold(null);
		}
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

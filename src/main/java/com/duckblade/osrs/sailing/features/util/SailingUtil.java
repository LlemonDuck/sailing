package com.duckblade.osrs.sailing.features.util;

import com.duckblade.osrs.sailing.model.Boat;
import com.google.common.collect.ImmutableSet;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SailingUtil
{
	public static final ImmutableSet<Integer> WORLD_ENTITY_TYPE_BOAT = ImmutableSet.of(
		1, // raft
		2,
		3 // 3? confirm sloop?
	);
	public static final int ACCOUNT_TYPE_UIM = 2;

	public static boolean isSailing(Client client)
	{
		return client.getLocalPlayer() != null && !client.getLocalPlayer().getWorldView().isTopLevel();
	}

	public static boolean isUim(Client client)
	{
		return client.getVarbitValue(VarbitID.IRONMAN) == ACCOUNT_TYPE_UIM;
	}

	// on boats, InteractingChanged fires for the local player but the target is null
	// it DOES fire an event with the expected target for a separate instance of Player with the same ID
	public static boolean isLocalPlayer(Client client, Actor actor)
	{
		return client.getLocalPlayer() != null &&
				actor instanceof Player && ((Player) actor).getId() == client.getLocalPlayer().getId();
	}

	public static ObjectComposition getTransformedObject(Client client, GameObject o)
	{
		ObjectComposition def = client.getObjectDefinition(o.getId());
		if (def == null || def.getImpostorIds() == null) {
			return def;
		}

		return def.getImpostor();
	}

	public static LocalPoint getTopLevelLocalPoint(Client client, BoatTracker boatTracker)
	{
		Player player = client.getLocalPlayer();
		WorldView wv = player.getWorldView();
		if (wv.isTopLevel())
		{
			return player.getLocalLocation();
		}

		Boat boat = boatTracker.getBoat();
		if (boat == null)
		{
			log.warn("getClosestWorldPoint failed due to lack of boat");
			return player.getLocalLocation();
		}

		return boat.getWorldEntity()
			.transformToMainWorld(client.getLocalPlayer().getLocalLocation());
	}

	public static WorldPoint getTopLevelWorldPoint(Client client, BoatTracker boatTracker)
	{
		return WorldPoint.fromLocal(
			client,
			getTopLevelLocalPoint(client, boatTracker)
		);
	}

}

package com.duckblade.osrs.sailing.features.util;

import lombok.RequiredArgsConstructor;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SailingUtil
{
	public static final int WORLD_ENTITY_TYPE_BOAT = 2;
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

	/// Provides a new polygon based on a game object's current position
	public static Polygon getExpandedGameObjectPoly(Client client, GameObject object, int size)
	{
		if (object == null)
			return null;

		WorldPoint wp = object.getWorldLocation();
		LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), new WorldPoint(wp.getX(), wp.getY(), wp.getPlane()));

		if (lp == null) {
			return null;
		}

		return Perspective.getCanvasTileAreaPoly(client, lp, size);
	}

	/// Returns all scene tiles in current top-level world view scene
	public static Set<Tile> getWorldViewTiles(Client client)
	{
		Set<Tile> worldViewTiles = new HashSet<>();

		Scene scene = client.getTopLevelWorldView().getScene();
		Tile[][][] tiles = scene.getTiles();

		int z = client.getPlane();

		for (int x = 0; x < Constants.SCENE_SIZE; ++x)
		{
			for (int y = 0; y < Constants.SCENE_SIZE; ++y)
			{
				Tile tile = tiles[z][x][y];

				if (tile == null)
				{
					continue;
				}

				worldViewTiles.add(tile);
			}
		}

		return worldViewTiles;
	}

	/// Returns all game objects in current top-level world view scene
	public static Set<GameObject> getWorldViewTileGameObjects(Client client)
	{
		return getWorldViewGameObjects(client, null);
	}

	/// Returns all game objects in current top-level world view scene with an objectId whitelist
	public static Set<GameObject> getWorldViewGameObjects(Client client, Set<Integer> objectIds)
	{
		Set<GameObject> gameObjects = new HashSet<>();

		if (client.getLocalPlayer() == null)
		{
			return gameObjects;
		}

		for (Tile tile : getWorldViewTiles(client))
		{
			if (tile != null)
			{
				GameObject[] tileObjects = tile.getGameObjects();

				if (tileObjects != null)
				{
					for (GameObject gameObject : tileObjects)
					{
						if (gameObject != null)
						{
							ObjectComposition def = client.getObjectDefinition(gameObject.getId());

							if (def == null || def.getImpostorIds() == null || def.getImpostor() == null)
							{
								if (objectIds == null || objectIds.contains(gameObject.getId()))
								{
									gameObjects.add(gameObject);
								}
							}
							else
							{
								if (objectIds == null || objectIds.contains(def.getImpostor().getId()))
								{
									gameObjects.add(gameObject);
								}
							}
						}
					}
				}
			}
		}

		return gameObjects;
	}
}

package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum CannonTier
{

	BRONZE(
		new int[]{
			ObjectID.SAILING_BRONZE_CANNON
		}
	),
	IRON(
		new int[]{
			ObjectID.SAILING_IRON_CANNON
		}
	),
	STEEL(
		new int[]{
			ObjectID.SAILING_STEEL_CANNON
		}
	),
	MITHRIL(
		new int[]{
			ObjectID.SAILING_MITHRIL_CANNON
		}
	),
	ADAMANT(
		new int[]{
			ObjectID.SAILING_ADAMANT_CANNON
		}
	),
	RUNE(
		new int[]{
			ObjectID.SAILING_RUNE_CANNON
		}
	),
	DRAGON(
		new int[]{
			ObjectID.SAILING_DRAGON_CANNON
		}
	),
	GOLD(
		new int[]{
			ObjectID.SAILING_GOLD_CANNON
		}
	),
	;

	private final int[] gameObjectIds;

	public static CannonTier fromGameObjectId(int id)
	{
		for (CannonTier tier : values())
		{
			for (int objectId : tier.getGameObjectIds())
			{
				if (objectId == id)
				{
					return tier;
				}
			}
		}

		return null;
	}

}
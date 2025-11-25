package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum SailTier
{

	WOOD(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_WOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_WOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_WOOD,
		}
	),
	OAK(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_OAK,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_OAK,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_OAK,
		}
	),
	TEAK(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_TEAK,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_TEAK,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_TEAK,
		}
	),
	MAHOGANY(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_MAHOGANY,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_MAHOGANY,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_MAHOGANY,
		}
	),
	CAMPHOR(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_CAMPHOR,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_CAMPHOR,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_CAMPHOR,
		}
	),
	IRONWOOD(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_IRONWOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_IRONWOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_IRONWOOD,
		}
	),
	ROSEWOOD(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_ROSEWOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_ROSEWOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_ROSEWOOD,
		}
	),
	;

	private final int[] gameObjectIds;

	public static SailTier fromGameObjectId(int id)
	{
		for (SailTier tier : values())
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

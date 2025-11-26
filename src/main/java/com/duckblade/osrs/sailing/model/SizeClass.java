package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum SizeClass
{

	RAFT(
		new int[]{
			ObjectID.SAILING_BOAT_HULL_KANDARIN_1X3_WOOD,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_1X3_OAK,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_1X3_TEAK,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_1X3_MAHOGANY,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_1X3_CAMPHOR,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_1X3_IRONWOOD,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_1X3_ROSEWOOD,
		},
		1,
		3
	),
	SKIFF(
		new int[]{
			ObjectID.SAILING_BOAT_HULL_KANDARIN_2X5_WOOD,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_2X5_OAK,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_2X5_TEAK,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_2X5_MAHOGANY,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_2X5_CAMPHOR,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_2X5_IRONWOOD,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_2X5_ROSEWOOD,
		},
		2,
		5
	),
	SLOOP(
		new int[]{
			ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_WOOD,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_OAK,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_TEAK,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_MAHOGANY,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_CAMPHOR,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_IRONWOOD,
			ObjectID.SAILING_BOAT_HULL_KANDARIN_3X8_ROSEWOOD,
		},
		3,
		8
	),
	;

	private final int[] gameObjectIds;
	private final int sizeX;
	private final int sizeY;

	public static SizeClass fromGameObjectId(int id)
	{
		for (SizeClass tier : values())
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

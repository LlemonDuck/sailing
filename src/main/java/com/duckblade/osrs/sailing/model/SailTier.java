package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.ObjectID;

@Slf4j
@RequiredArgsConstructor
@Getter
public enum SailTier
{

	WOOD(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_WOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_WOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_WOOD,
		},
		new int[]{
			20,
			20,
			20,
		}
	),
	OAK(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_OAK,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_OAK,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_OAK,
		},
		new int[]{
			22,
			22,
			22,
		}
	),
	TEAK(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_TEAK,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_TEAK,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_TEAK,
		},
		new int[]{
			24,
			24,
			24,
		}
	),
	MAHOGANY(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_MAHOGANY,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_MAHOGANY,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_MAHOGANY,
		},
		new int[]{
			26, // yes, this is literally the only special case
			27,
			27,
		}
	),
	CAMPHOR(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_CAMPHOR,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_CAMPHOR,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_CAMPHOR,
		},
		new int[]{
			30,
			30,
			30,
		}
	),
	IRONWOOD(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_IRONWOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_IRONWOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_IRONWOOD,
		},
		new int[]{
			33,
			33,
			33,
		}
	),
	ROSEWOOD(
		new int[]{
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_1X3_ROSEWOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_2X5_ROSEWOOD,
			ObjectID.SAILING_BOAT_SAIL_KANDARIN_3X8_ROSEWOOD,
		},
		new int[]{
			36,
			36,
			36,
		}
	),
	;

	private final int[] gameObjectIds;
	private final int[] speedBoostDurations;

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

	public int getSpeedBoostDuration(SizeClass sizeClass)
	{
		switch (sizeClass)
		{
			case RAFT:
				return speedBoostDurations[0];
			case SKIFF:
				return speedBoostDurations[1];
			case SLOOP:
				return speedBoostDurations[2];
			default:
				log.error("Unknown size class {}", sizeClass);
				return -1;
		}
	}
}

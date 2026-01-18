package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum ChumStationTier
{

    
	BASIC(
		new int[]{
			ObjectID.CHUM_STATION_2X5A,
            ObjectID.CHUM_STATION_2X5B,
            ObjectID.CHUM_STATION_3X8A,
            ObjectID.CHUM_STATION_3X8B
		}
	),
    // look at these variables and tell me you dont see it I dare you
	ADVANCED(
		new int[]{
			ObjectID.CHUM_SPREADER_2X5A,
            ObjectID.CHUM_SPREADER_2X5B,
            ObjectID.CHUM_SPREADER_3X8A,
            ObjectID.CHUM_SPREADER_3X8B
		}
	),
	SPREADER(
		new int[]{
			ObjectID.CHUM_STATION_ADVANCED_2X5A,
            ObjectID.CHUM_STATION_ADVANCED_2X5B,
            ObjectID.CHUM_STATION_ADVANCED_3X8A,
            ObjectID.CHUM_STATION_ADVANCED_3X8B
		}
	),
	;

	private final int[] gameObjectIds;

	public static ChumStationTier fromGameObjectId(int id)
	{
		for (ChumStationTier tier : values())
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
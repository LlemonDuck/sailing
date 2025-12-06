package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum ChumStationTier {
    CHUM_STATION(
        new int[]{
            ObjectID.CHUM_STATION_2X5A,
            ObjectID.CHUM_STATION_2X5B,
            ObjectID.CHUM_STATION_3X8A,
            ObjectID.CHUM_STATION_3X8B,
        }
    ),
    CHUM_STATION_ADVANCED(
        new int[]{
            ObjectID.CHUM_STATION_ADVANCED_2X5A,
            ObjectID.CHUM_STATION_ADVANCED_2X5B,
            ObjectID.CHUM_STATION_ADVANCED_3X8A,
            ObjectID.CHUM_STATION_ADVANCED_3X8B,
        }
    ),
    CHUM_SPREADER(
        new int[]{
            ObjectID.CHUM_SPREADER_2X5A,
            ObjectID.CHUM_SPREADER_2X5B,
            ObjectID.CHUM_SPREADER_3X8A,
            ObjectID.CHUM_SPREADER_3X8B,
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

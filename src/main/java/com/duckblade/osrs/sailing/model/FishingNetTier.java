package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum FishingNetTier {
    ROPE(
            new int[]{
                    ObjectID.SAILING_ROPE_TRAWLING_NET,
                    ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_PORT,
                    ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_STARBOARD
            }
    ),
    LINEN(new int[]{
            ObjectID.SAILING_LINEN_TRAWLING_NET,
            ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_PORT,
            ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_STARBOARD
    }),
    HEMP(new int[]{
            ObjectID.SAILING_HEMP_TRAWLING_NET,
            ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_PORT,
            ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_STARBOARD,
    }),
    COTTON(new int[]{
            ObjectID.SAILING_COTTON_TRAWLING_NET,
            ObjectID.SAILING_COTTON_TRAWLING_NET_3X8_PORT,
            ObjectID.SAILING_COTTON_TRAWLING_NET_3X8_STARBOARD,
    });

    private final int[] gameObjectIds;

    public static FishingNetTier fromGameObjectId(int id)
    {
        for (FishingNetTier tier : values())
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

    public int getCapacity() {
        return 125;
    }
}



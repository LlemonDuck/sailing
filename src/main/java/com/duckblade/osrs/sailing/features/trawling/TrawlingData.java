package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.model.FishingAreaType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

class TrawlingData {

    static class ShoalObjectID {
        static final int GIANT_KRILL = ObjectID.SAILING_SHOAL_CLICKBOX_GIANT_KRILL;
        static final int HADDOCK = ObjectID.SAILING_SHOAL_CLICKBOX_HADDOCK;
        static final int YELLOWFIN = ObjectID.SAILING_SHOAL_CLICKBOX_YELLOWFIN;
        static final int HALIBUT = ObjectID.SAILING_SHOAL_CLICKBOX_HALIBUT;
        static final int BLUEFIN = ObjectID.SAILING_SHOAL_CLICKBOX_BLUEFIN;
        static final int MARLIN = ObjectID.SAILING_SHOAL_CLICKBOX_MARLIN;
        static final int SHIMMERING = ObjectID.SAILING_SHOAL_CLICKBOX_SHIMMERING;
        static final int GLISTENING = ObjectID.SAILING_SHOAL_CLICKBOX_GLISTENING;
        static final int VIBRANT = ObjectID.SAILING_SHOAL_CLICKBOX_VIBRANT;
    }

    static class FishingAreas {

        /**
         * Get the fishing area type for a given world location
         * @param location The world point to check
         * @return The fishing area type, or null if not in a known fishing area
         */
		static FishingAreaType getFishingAreaType(final WorldPoint location) {
			if (location == null) {
				return null;
			}

			for (final var area : ShoalFishingArea.AREAS) {
				if (area.contains(location)) {
					return area.getShoal().getDepth();
				}
			}

			return null;
		}

        /**
         * Get the shoal stop duration for a given world location
         * @param location The world point to check
         * @return The stop duration in ticks, or -1 if not in a known fishing area
         */
        static int getStopDurationForLocation(final WorldPoint location) {
            if (location == null) {
                return -1;
            }

            for (final var area : ShoalFishingArea.AREAS) {
                if (area.contains(location)) {
                    return area.getShoal().getStopDuration();
                }
            }

            return -1;
        }
    }
}

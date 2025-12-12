package com.duckblade.osrs.sailing.features.trawling;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

public class TrawlingData {

    public static class ShoalObjectID {
        protected static final int GIANT_KRILL = ObjectID.SAILING_SHOAL_CLICKBOX_GIANT_KRILL;
        protected static final int HADDOCK = ObjectID.SAILING_SHOAL_CLICKBOX_HADDOCK;
        protected static final int YELLOWFIN = ObjectID.SAILING_SHOAL_CLICKBOX_YELLOWFIN;
        protected static final int HALIBUT = ObjectID.SAILING_SHOAL_CLICKBOX_HALIBUT;
        protected static final int BLUEFIN = ObjectID.SAILING_SHOAL_CLICKBOX_BLUEFIN;
        protected static final int MARLIN = ObjectID.SAILING_SHOAL_CLICKBOX_MARLIN;
        protected static final int SHIMMERING = ObjectID.SAILING_SHOAL_CLICKBOX_SHIMMERING;
        protected static final int GLISTENING = ObjectID.SAILING_SHOAL_CLICKBOX_GLISTENING;
        protected static final int VIBRANT = ObjectID.SAILING_SHOAL_CLICKBOX_VIBRANT;
    }

    public static class ShoalStopDuration {
        protected static final int YELLOWFIN = 100;
        protected static final int GIANT_KRILL = 90;
        protected static final int HALIBUT = 78;
        protected static final int BLUEFIN = 66;
        protected static final int MARLIN = 50;
        // Note: Haddock duration would be added here when known
        // protected static final int HADDOCK = ?;
    }

    public static class FishingAreas {
        // Giant krill areas (80 tick duration) - ONE_DEPTH
        protected static final ShoalFishingArea SIMIAN_SEA = new ShoalFishingArea(2745, 2866, 2538, 2649, ShoalStopDuration.GIANT_KRILL);
        protected static final ShoalFishingArea TURTLE_BELT = new ShoalFishingArea(2912, 3037, 2455, 2586, ShoalStopDuration.GIANT_KRILL);
        protected static final ShoalFishingArea GREAT_SOUND = new ShoalFishingArea(1536, 1648, 3317, 3411, ShoalStopDuration.GIANT_KRILL);
        protected static final ShoalFishingArea SUNSET_BAY = new ShoalFishingArea(1477, 1604, 2860, 2959, ShoalStopDuration.GIANT_KRILL);

        // Halibut areas (76 tick duration) - TWO_DEPTH
        protected static final ShoalFishingArea PORT_ROBERTS = new ShoalFishingArea(1821, 2032, 3120, 3420, ShoalStopDuration.HALIBUT);
        protected static final ShoalFishingArea SOUTHERN_EXPANSE = new ShoalFishingArea(1880, 2096, 2282, 2488, ShoalStopDuration.HALIBUT);
        
        // Yellowfin areas (100 tick duration) - TWO_DEPTH
        protected static final ShoalFishingArea DEEPFIN_POINT = new ShoalFishingArea(1633, 1819, 2533, 2731, ShoalStopDuration.YELLOWFIN);
        
        // Bluefin areas (66 tick duration) - THREE_DEPTH
        protected static final ShoalFishingArea RAINBOW_REEF = new ShoalFishingArea(2099, 2386, 2211, 2401, ShoalStopDuration.BLUEFIN);
        protected static final ShoalFishingArea BUCCANEERS_HAVEN = new ShoalFishingArea(1984, 2268, 3594, 3771, ShoalStopDuration.BLUEFIN);
        
        // Marlin areas (50 tick duration) - THREE_DEPTH
        // Weissmere coordinates based on actual in-game location (top-level coordinates)
        // Expanded to ensure full coverage of shoal routes
        protected static final ShoalFishingArea WEISSMERE = new ShoalFishingArea(2570, 2925, 3880, 4200, ShoalStopDuration.MARLIN);

        // Three-depth areas (Bluefin and Marlin)
        private static final ShoalFishingArea[] THREE_DEPTH_AREAS = {
            RAINBOW_REEF,
            BUCCANEERS_HAVEN,
            WEISSMERE
        };

        // All fishing areas for lookup
        private static final ShoalFishingArea[] ALL_AREAS = {
            PORT_ROBERTS,
            SOUTHERN_EXPANSE,
            DEEPFIN_POINT,
            RAINBOW_REEF,
            BUCCANEERS_HAVEN,
            WEISSMERE
        };

        /**
         * Determine if a location is in a three-depth area (Bluefin or Marlin areas)
         * @param location The world point to check
         * @return true if the location is in a three-depth area, false otherwise
         */
        public static boolean isThreeDepthArea(WorldPoint location) {
            if (location == null) {
                return false;
            }

            for (ShoalFishingArea area : THREE_DEPTH_AREAS) {
                if (area.contains(location)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Get the fishing area type for a given world location
         * @param location The world point to check
         * @return The fishing area type, or null if not in a known fishing area
         */
        public static FishingAreaType getFishingAreaType(WorldPoint location) {
            if (location == null) {
                return null;
            }

            if (isThreeDepthArea(location)) {
                return FishingAreaType.THREE_DEPTH;
            }

            // Check if it's in any fishing area at all
            for (ShoalFishingArea area : ALL_AREAS) {
                if (area.contains(location)) {
                    return FishingAreaType.TWO_DEPTH;
                }
            }

            return null;
        }

        /**
         * Get the shoal stop duration for a given world location
         * @param location The world point to check
         * @return The stop duration in ticks, or -1 if not in a known fishing area
         */
        public static int getStopDurationForLocation(WorldPoint location) {
            if (location == null) {
                return -1;
            }

            for (ShoalFishingArea area : ALL_AREAS) {
                if (area.contains(location)) {
                    return area.getStopDuration();
                }
            }

            return -1;
        }
    }
}

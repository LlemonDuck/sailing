package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.model.FishingAreaType;
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
        protected static final int HADDOCK = 0; // One-depth area - no timer needed
    }

    public static class FishingAreas {
        // One-depth areas (no timer needed) - ONE_DEPTH
        protected static final ShoalFishingArea SIMIAN_SEA = new ShoalFishingArea(2745, 2866, 2538, 2649, ShoalStopDuration.GIANT_KRILL);
        protected static final ShoalFishingArea TURTLE_BELT = new ShoalFishingArea(2912, 3037, 2455, 2586, ShoalStopDuration.GIANT_KRILL);
        protected static final ShoalFishingArea GREAT_SOUND = new ShoalFishingArea(1536, 1648, 3317, 3411, ShoalStopDuration.GIANT_KRILL);
        protected static final ShoalFishingArea SUNSET_BAY = new ShoalFishingArea(1477, 1604, 2860, 2959, ShoalStopDuration.GIANT_KRILL);
        protected static final ShoalFishingArea MISTY_SEA = new ShoalFishingArea(1377, 1609, 2607, 2788, ShoalStopDuration.HADDOCK);

        // Halibut areas (76 tick duration) - TWO_DEPTH
        protected static final ShoalFishingArea PORT_ROBERTS = new ShoalFishingArea(1821, 2032, 3120, 3420, ShoalStopDuration.HALIBUT);
        protected static final ShoalFishingArea SOUTHERN_EXPANSE = new ShoalFishingArea(1880, 2096, 2282, 2488, ShoalStopDuration.HALIBUT);
        
        // Yellowfin areas (100 tick duration) - TWO_DEPTH
        protected static final ShoalFishingArea DEEPFIN_POINT = new ShoalFishingArea(1633, 1819, 2533, 2731, ShoalStopDuration.YELLOWFIN);
        
        // Bluefin areas (66 tick duration) - THREE_DEPTH
        protected static final ShoalFishingArea RAINBOW_REEF = new ShoalFishingArea(2099, 2386, 2211, 2401, ShoalStopDuration.BLUEFIN);
        protected static final ShoalFishingArea BUCCANEERS_HAVEN = new ShoalFishingArea(1962, 2274, 3590, 3792, ShoalStopDuration.BLUEFIN);
        
        // Marlin areas (50 tick duration) - THREE_DEPTH
        // Weissmere coordinates based on actual in-game location (top-level coordinates)
        // Expanded to ensure full coverage of shoal routes
        protected static final ShoalFishingArea WEISSMERE = new ShoalFishingArea(2590, 2870, 3945, 4146, ShoalStopDuration.MARLIN);
        protected static final ShoalFishingArea BRITTLE_ISLE = new ShoalFishingArea(1856, 2078, 3963, 4121, ShoalStopDuration.MARLIN);

        // One-depth areas (Giant Krill and Haddock)
        private static final ShoalFishingArea[] ONE_DEPTH_AREAS = {
            SIMIAN_SEA,
            TURTLE_BELT,
            GREAT_SOUND,
            SUNSET_BAY,
            MISTY_SEA
        };

        // Three-depth areas (Bluefin and Marlin)
        private static final ShoalFishingArea[] THREE_DEPTH_AREAS = {
            RAINBOW_REEF,
            BUCCANEERS_HAVEN,
            WEISSMERE,
            BRITTLE_ISLE
        };

        // All fishing areas for lookup
        private static final ShoalFishingArea[] ALL_AREAS = {
            SIMIAN_SEA,
            TURTLE_BELT,
            GREAT_SOUND,
            SUNSET_BAY,
            MISTY_SEA,
            PORT_ROBERTS,
            SOUTHERN_EXPANSE,
            DEEPFIN_POINT,
            RAINBOW_REEF,
            BUCCANEERS_HAVEN,
            WEISSMERE,
            BRITTLE_ISLE
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

            // Check for ONE_DEPTH areas first (Giant Krill areas)
            for (ShoalFishingArea area : ONE_DEPTH_AREAS) {
                if (area.contains(location)) {
                    return FishingAreaType.ONE_DEPTH;
                }
            }

            // Check for THREE_DEPTH areas (Bluefin and Marlin areas)
            if (isThreeDepthArea(location)) {
                return FishingAreaType.THREE_DEPTH;
            }

            // Check if it's in any other fishing area (TWO_DEPTH)
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

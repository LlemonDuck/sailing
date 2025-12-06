package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum FacilityAggregate {

    CARGO_HOLD(
        Arrays.stream(CargoHoldTier.values())
            .map(CargoHoldTier::getGameObjectIds)
            .flatMapToInt(Arrays::stream)
            .toArray()
    ),
    SALVAGING_HOOK(
        Arrays.stream(SalvagingHookTier.values())
            .map(SalvagingHookTier::getGameObjectIds)
            .flatMapToInt(Arrays::stream)
            .toArray()
    ),
    TRAWLING_NET(
        Arrays.stream(TrawlingNetTier.values())
            .map(TrawlingNetTier::getGameObjectIds)
            .flatMapToInt(Arrays::stream)
            .toArray()
    ),
    CHUM_STATION(
        Arrays.stream(ChumStationTier.values())
            .map(ChumStationTier::getGameObjectIds)
            .flatMapToInt(Arrays::stream)
            .toArray()
    ),
    CANNON(
        new int[]{
            ObjectID.SAILING_BRONZE_CANNON,
            ObjectID.SAILING_IRON_CANNON,
            ObjectID.SAILING_STEEL_CANNON,
            ObjectID.SAILING_MITHRIL_CANNON,
            ObjectID.SAILING_ADAMANT_CANNON,
            ObjectID.SAILING_RUNE_CANNON,
            ObjectID.SAILING_DRAGON_CANNON,
        }
    ),
    ETERNAL_BRAZIER(
        new int[]{
            ObjectID.SAILING_BOAT_1X3_ETERNAL_BRAZIER,
            ObjectID.SAILING_BOAT_SKIFF_ETERNAL_BRAZIER,
            ObjectID.SAILING_BOAT_SLOOP_ETERNAL_BRAZIER,
        }
    ),
    RANGE(
        new int[]{
            ObjectID.SAILING_FACILITY_RANGE,
        }
    ),
    KEG(
        new int[]{
            ObjectID.SAILING_KEG_EMPTY,
            ObjectID.SAILING_KEG_GROG,
            ObjectID.SAILING_KEG_CIDER,
            ObjectID.SAILING_KEG_WHIRLPOOL_SURPRISE,
            ObjectID.SAILING_KEG_KRAKEN_INK_STOUT,
            ObjectID.SAILING_KEG_PERILDANCE_BITTER,
            ObjectID.SAILING_KEG_TRAWLERS_TRUST,
            ObjectID.SAILING_KEG_HORIZONS_LURE,
        }
    ),
    INOCULATION_STATION(
        new int[]{
            ObjectID.SAILING_FACILITY_2X5_INOCULATION_STATION,
            ObjectID.SAILING_FACILITY_2X5_INOCULATION_STATION_NOOP,
            ObjectID.SAILING_FACILITY_3X8_INOCULATION_STATION,
        }
    ),
    SALVAGING_STATION(
        new int[]{
            ObjectID.SAILING_SALVAGING_STATION_2X5A,
            ObjectID.SAILING_SALVAGING_STATION_2X5B,
            ObjectID.SAILING_SALVAGING_STATION_3X8,
        }
    ),
    WIND(
        new int[]{
            ObjectID.SAILING_WIND_CATCHER_ACTIVATED,
            ObjectID.SAILING_WIND_CATCHER_DEACTIVATED,
            ObjectID.SAILING_GALE_CATCHER_ACTIVATED,
            ObjectID.SAILING_GALE_CATCHER_DEACTIVATED,
            ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED,
            ObjectID.SAILING_CRYSTAL_EXTRACTOR_DEACTIVATED,
        }
    ),
    ANCHOR(
        new int[]{
            ObjectID.SAILING_ANCHOR_RAISED_2X5,
            ObjectID.SAILING_ANCHOR_LOWERED_2X5,
            ObjectID.SAILING_ANCHOR_RAISED_3X8,
            ObjectID.SAILING_ANCHOR_LOWERED_3X8,
        }
    ),
    TELEPORT_FOCUS(
        new int[]{
            ObjectID.SAILING_TELEPORTATION_FOCUS,
            ObjectID.SAILING_TELEPORTATION_FOCUS_GREATER,
        }
    ),
    FATHOM_DEVICE(
        new int[]{
            ObjectID.SAILING_FATHOM_STONE,
            ObjectID.SAILING_FATHOM_PEARL,
        }
    ),
    ;

    private final int[] gameObjectIds;

    public static FacilityAggregate fromGameObjectId(int id)
    {
        for (FacilityAggregate facility : values())
        {
            for (int objectId : facility.getGameObjectIds())
            {
                if (objectId == id) {
                    return facility;
                }
            }
        }

        return null;
    }
}

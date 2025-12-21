package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.features.trawling.ShoalPathData.*;
import lombok.Getter;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

@Getter
public enum ShoalFishingArea
{
	GREAT_SOUND(
		new WorldArea(1546, 3327, 93, 75, 0),
		ShoalPaths.GIANT_KRILL_GREAT_SOUND,
		new int[]{0, 18, 25, 31, 38, 50, 59},
		Shoal.GIANT_KRILL
	),
	SIMIAN_SEA(
		new WorldArea(2755, 2548, 103, 92, 0),
		ShoalPaths.GIANT_KRILL_SIMIAN_SEA,
		new int[]{0, 6, 12, 20, 27, 39, 49},
		Shoal.GIANT_KRILL
	),
	SUNSET_BAY(
		new WorldArea(1477, 2860, 128, 100, 0),
		ShoalPaths.GIANT_KRILL_SUNSET_BAY,
		new int[]{0, 9, 19, 37, 46, 52, 68},
		Shoal.GIANT_KRILL
	),
	TURTLE_BELT(
		new WorldArea(2922, 2465, 106, 112, 0),
		ShoalPaths.GIANT_KRILL_TURTLE_BELT,
		new int[]{0, 6, 20, 27, 33, 56, 66, 77},
		Shoal.GIANT_KRILL
	),

	ANGLERFISHS_LIGHT(
		new WorldArea(2672, 2295, 162, 159, 0),
		ShoalPaths.HADDOCK_ANGLERFISHS_LIGHT,
		new int[]{0, 6, 22, 41, 48, 60, 74, 84},
		Shoal.HADDOCK
	),
	MISTY_SEA(
		new WorldArea(1377, 2607, 233, 182, 0),
		ShoalPaths.HADDOCK_MISTY_SEA,
		new int[]{0, 20, 45, 60, 64, 70, 87, 99, 112, 118},
		Shoal.HADDOCK
	),
	THE_ONYX_CREST(
		new WorldArea(2929, 2157, 196, 219, 0),
		ShoalPaths.HADDOCK_THE_ONYX_CREST,
		new int[]{0, 4, 15, 34, 52, 68, 83, 108, 129, 142},
		Shoal.HADDOCK
	),

	DEEPFIN_POINT(
		new WorldArea(1781, 2665, 244, 216, 0),
		ShoalPaths.YELLOWFIN_DEEPFIN_POINT,
		new int[]{0, 20, 42, 74, 100, 117, 136, 163, 197, 211, 237},
		Shoal.YELLOWFIN
	),
	SEA_OF_SOULS(
		new WorldArea(2173, 2585, 192, 179, 0),
		ShoalPaths.YELLOWFIN_SEA_OF_SOULS,
		new int[]{0, 18, 38, 43, 53, 84, 107, 124, 140, 145, 155, 183},
		Shoal.YELLOWFIN
	),
	THE_CROWN_JEWEL_TEMP(
		new WorldArea(1633, 2533, 187, 199, 0),
		ShoalPaths.YELLOWFIN_THE_CROWN_JEWEL,
		new int[]{0, 23, 60, 80, 100, 109, 128, 154, 193},
		Shoal.YELLOWFIN
	),
	// 
	PORT_ROBERTS(HalibutPortRoberts.INSTANCE),
	SOUTHERN_EXPANSE(HalibutSouthernExpanse.INSTANCE),
	BUCCANEERS_HAVEN(BluefinBuccaneersHaven.INSTANCE),
	RAINBOW_REEF(BluefinRainbowReef.INSTANCE),
	WEISSMERE(MarlinWeissmere.INSTANCE),
	BRITTLE_ISLE(MarlinBrittleIsle.INSTANCE);

	static final ShoalFishingArea[] AREAS = values();

	private final WorldArea area;
	private final WorldPoint[] path;
	private final int[] stopIndices;
	private final Shoal shoal;
    /**
     * -- GETTER --
     *  Get the ShoalAreaData interface if this area uses the new interface-based approach.
     */
    private final ShoalAreaData areaData; // For interface-based entries

	// Constructor for legacy entries (4 parameters)
	ShoalFishingArea(WorldArea area, WorldPoint[] path, int[] stopIndices, Shoal shoal) {
		this.area = area;
		this.path = path;
		this.stopIndices = stopIndices;
		this.shoal = shoal;
		this.areaData = null;
	}

	// Constructor for interface-based entries (single parameter)
	ShoalFishingArea(ShoalAreaData areaData) {
		this.areaData = areaData;
		this.area = areaData.getArea();
		this.path = areaData.getPositions();
		this.stopIndices = areaData.getStopIndices();
		this.shoal = areaData.getShoalType();
	}

	public boolean contains(final WorldPoint wp)
	{
		return area.contains(wp);
	}
}

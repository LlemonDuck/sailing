package com.duckblade.osrs.sailing.features.trawling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

@Getter
@RequiredArgsConstructor
public enum ShoalFishingArea
{
	GREAT_SOUND(
		new WorldArea(1536, 3317, 113, 95, 0),
		ShoalPaths.GIANT_KRILL_GREAT_SOUND,
		new int[]{0, 10, 19, 29, 43, 48, 53},
		Shoal.GIANT_KRILL
	),
	SIMIAN_SEA(
		new WorldArea(2745, 2538, 122, 112, 0),
		ShoalPaths.GIANT_KRILL_SIMIAN_SEA,
		new int[]{0, 12, 22, 26, 32, 37, 42},
		Shoal.GIANT_KRILL
	),
	SUNSET_BAY(
		new WorldArea(1477, 2860, 128, 100, 0),
		ShoalPaths.GIANT_KRILL_SUNSET_BAY,
		new int[]{0, 17, 29, 36, 46, 64, 73},
		Shoal.GIANT_KRILL
	),
	TURTLE_BELT(
		new WorldArea(2912, 2455, 126, 132, 0),
		ShoalPaths.GIANT_KRILL_TURTLE_BELT,
		new int[]{0, 11, 17, 23, 37, 44, 50, 73},
		Shoal.GIANT_KRILL
	),

	ANGLERFISHS_LIGHT(
		new WorldArea(2672, 2295, 162, 159, 0),
		ShoalPaths.HADDOCK_ANGLERFISHS_LIGHT,
		new int[]{0, 14, 33, 40, 52, 65, 74},
		Shoal.HADDOCK
	),
	MISTY_SEA(
		new WorldArea(1377, 2607, 233, 182, 0),
		ShoalPaths.HADDOCK_MISTY_SEA,
		new int[]{0, 14, 28, 34, 52, 76, 105, 118, 125, 134},
		Shoal.HADDOCK
	),
	THE_ONYX_CREST(
		new WorldArea(2929, 2157, 196, 219, 0),
		ShoalPaths.HADDOCK_THE_ONYX_CREST,
		new int[]{0, 18, 37, 53, 68, 91, 112, 124, 137, 141},
		Shoal.HADDOCK
	),

	DEEPFIN_POINT(
		new WorldArea(1740, 2665, 285, 216, 0),
		ShoalPaths.YELLOWFIN_DEEPFIN_POINT,
		new int[]{0, 18, 37, 58, 90, 116, 125, 144, 171, 207, 220},
		Shoal.YELLOWFIN
	),
	SEA_OF_SOULS(
		new WorldArea(2173, 2585, 192, 179, 0),
		ShoalPaths.YELLOWFIN_SEA_OF_SOULS,
		new int[]{0, 15, 30, 35, 44, 73, 95, 113, 133, 138, 147, 177},
		Shoal.YELLOWFIN
	),
	THE_CROWN_JEWEL_TEMP(
		new WorldArea(1633, 2533, 187, 199, 0),
		ShoalPaths.YELLOWFIN_THE_CROWN_JEWEL,
		new int[]{0, 34, 52, 70, 79, 98, 122, 158},
		Shoal.YELLOWFIN
	),
	// 
	PORT_ROBERTS(
		new WorldArea(1821, 3120, 212, 301, 0),
		ShoalPaths.HALIBUT_PORT_ROBERTS,
		new int[]{0, 35, 55, 75, 98, 124, 144, 171, 188},
		Shoal.HALIBUT
	),
	SOUTHERN_EXPANSE(
		new WorldArea(1880, 2282, 217, 207, 0),
		ShoalPaths.HALIBUT_SOUTHERN_EXPANSE,
		new int[]{0, 23, 46, 80, 128, 145, 176, 201, 229, 241},
		Shoal.HALIBUT
	),

	BUCCANEERS_HAVEN(
		new WorldArea(1962, 3590, 313, 203, 0),
		ShoalPaths.BLUEFIN_BUCCANEERS_HAVEN,
		new int[]{0, 17, 27, 59, 79, 93, 111, 126, 145, 153, 173, 191},
		Shoal.BLUEFIN
	),
	RAINBOW_REEF(
		new WorldArea(2099, 2211, 288, 191, 0),
		ShoalPaths.BLUEFIN_RAINBOW_REEF,
		new int[]{0, 13, 45, 75, 93, 119, 136, 160, 169, 192},
		Shoal.BLUEFIN
	),

	WEISSMERE(
		new WorldArea(2590, 3945, 281, 202, 0),
		ShoalPaths.MARLIN_WEISSMERE,
		new int[]{0, 10, 40, 57, 65, 67, 118, 129},
		Shoal.MARLIN
	),
	BRITTLE_ISLE(
		new WorldArea(1856, 3963, 223, 159, 0),
		ShoalPaths.MARLIN_BRITTLE_ISLE,
		new int[]{0, 13, 29, 58, 80, 103, 134, 165, 200},
		Shoal.MARLIN
	),
	;

	static final ShoalFishingArea[] AREAS = values();

	private final WorldArea area;
	private final WorldPoint[] path;
	private final int[] stopIndices;
	private final Shoal shoal;

	public boolean contains(final WorldPoint wp)
	{
		return area.contains(wp);
	}
}

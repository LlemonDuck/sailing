package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.model.FishingAreaType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Shoal
{
	GIANT_KRILL(FishingAreaType.ONE_DEPTH, 0),
	HADDOCK(FishingAreaType.ONE_DEPTH, 0),
	YELLOWFIN(FishingAreaType.TWO_DEPTH, 100),
	HALIBUT(FishingAreaType.TWO_DEPTH, 76),
	BLUEFIN(FishingAreaType.THREE_DEPTH, 66),
	MARLIN(FishingAreaType.THREE_DEPTH, 50);

	private final FishingAreaType depth;
	private final int stopDuration;
}

package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.model.FishingAreaType;
import java.awt.Color;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Shoal
{

	// Shoal durations here are 2 ticks lower than wiki numbers to handle movement tracking
	GIANT_KRILL("Giant krill", "Giant blue krill", new Color(0xd7774d), FishingAreaType.ONE_DEPTH, 0),
	HADDOCK("Haddock", "Golden haddock", new Color(0x7e919f), FishingAreaType.ONE_DEPTH, 0),
	YELLOWFIN("Yellowfin", "Orangefin", new Color(0xebcd1c), FishingAreaType.TWO_DEPTH, 98),
	HALIBUT("Halibut", "Huge halibut", new Color(0xb08f54), FishingAreaType.TWO_DEPTH, 78),
	BLUEFIN("Bluefin", "Purplefin", new Color(0x2a89a8), FishingAreaType.THREE_DEPTH, 68),
	MARLIN("Marlin", "Swift marlin", new Color(0xb9b7ad), FishingAreaType.THREE_DEPTH, 48);

	private static final Shoal[] VALUES = values();

	private final String name;
	private final String exoticName;
	private final Color color;
	private final FishingAreaType depth;
	private final int stopDuration;

	@Override
	public String toString()
	{
		return name;
	}

	public static @Nullable Shoal byName(final String name)
	{
		for (final var shoal : VALUES)
		{
			if (shoal.name.equalsIgnoreCase(name) || shoal.exoticName.equalsIgnoreCase(name))
			{
				return shoal;
			}
		}

		return null;
	}
}

package com.duckblade.osrs.sailing.features.barracudatrials.locktohelm;

import com.duckblade.osrs.sailing.model.WindFacilityTier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LockToHelmFilter
{
	DISABLED(new int[] {})
		{
			@Override
			public final String toString()
			{
				return "Disabled";
			}
		},

	WIND_GALE_CATCHER(WindFacilityTier.getWindFacilityObjectIDs(WindFacilityTier.WIND_CATCHER, WindFacilityTier.GALE_CATCHER))
		{
			@Override
			public final String toString()
			{
				return "Wind/Gale Catcher";
			}
		},

	CRYSTAL_EXTRACTOR(WindFacilityTier.CRYSTAL_EXTRACTOR.getGameObjectIds())
		{
			@Override
			public String toString()
			{
				return "Crystal Extractor";
			}
		};

	@Getter
	private final int[] facility;
}
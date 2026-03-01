package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;
import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum WindFacilityTier
{

	WIND_CATCHER(
		new int[]{
			ObjectID.SAILING_WIND_CATCHER_ACTIVATED,
			ObjectID.SAILING_WIND_CATCHER_DEACTIVATED
		}
	),

	GALE_CATCHER(
		new int[]{
			ObjectID.SAILING_GALE_CATCHER_ACTIVATED,
			ObjectID.SAILING_GALE_CATCHER_DEACTIVATED
		}
	),

	CRYSTAL_EXTRACTOR(
		new int[]{
			ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED,
			ObjectID.SAILING_CRYSTAL_EXTRACTOR_DEACTIVATED
		}
	);

	private final int[] gameObjectIds;

	public static int[] getWindFacilityObjectIDs(WindFacilityTier... facilities)
	{
		return Arrays.stream(facilities)
			.map(WindFacilityTier::getGameObjectIds)
			.flatMapToInt(Arrays::stream)
			.toArray();
	}

}

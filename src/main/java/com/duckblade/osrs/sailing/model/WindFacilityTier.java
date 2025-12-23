package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum WindFacilityTier
{

	WIND_FACILITY_ACTIVATED(
		new int[]{
			ObjectID.SAILING_WIND_CATCHER_ACTIVATED,
			ObjectID.SAILING_GALE_CATCHER_ACTIVATED,
			ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED,
		}
	),
	WIND_FACILITY_DEACTIVATED(
		new int[]{
			ObjectID.SAILING_WIND_CATCHER_DEACTIVATED,
			ObjectID.SAILING_GALE_CATCHER_DEACTIVATED,
			ObjectID.SAILING_CRYSTAL_EXTRACTOR_DEACTIVATED,
		}
	);

	private final int[] gameObjectIds;

	public static WindFacilityTier fromGameObjectId(int id)
	{
		for (WindFacilityTier facility : values())
		{
			for (int objectId : facility.getGameObjectIds())
			{
				if (objectId == id)
				{
					return facility;
				}
			}
		}

		return null;
	}
}

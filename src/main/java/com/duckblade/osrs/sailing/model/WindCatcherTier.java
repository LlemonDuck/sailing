package com.duckblade.osrs.sailing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum WindCatcherTier
{

	WIND(
		new int[]{
			ObjectID.SAILING_WIND_CATCHER_ACTIVATED,
			ObjectID.SAILING_WIND_CATCHER_DEACTIVATED
		}
	),
	GALE(
		new int[]{
			ObjectID.SAILING_GALE_CATCHER_ACTIVATED,
			ObjectID.SAILING_GALE_CATCHER_DEACTIVATED
		}
	),
	;

	private final int[] gameObjectIds;

	public static WindCatcherTier fromGameObjectId(int id)
	{
		for (WindCatcherTier tier : values())
		{
			for (int objectId : tier.getGameObjectIds())
			{
				if (objectId == id)
				{
					return tier;
				}
			}
		}

		return null;
	}

}
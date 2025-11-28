package com.duckblade.osrs.sailing.features.barracudatrials;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

@Getter
@RequiredArgsConstructor
public enum BarracudaDifficulty
{

	SWORDFISH(2),
	SHARK(3),
	MARLIN(4),
	;

	private final int inTrialVarbValue;

	public static BarracudaDifficulty ofVarbitValue(int varbitValue)
	{
		for (BarracudaDifficulty difficulty : values())
		{
			if (difficulty.getInTrialVarbValue() == varbitValue)
			{
				return difficulty;
			}
		}
		return null;
	}

	public static BarracudaDifficulty current(Client client)
	{
		int varbitValue = client.getVarbitValue(VarbitID.SAILING_BT_IN_TRIAL);
		if (varbitValue == 1)
		{
			// unranked (1) is always the same as swordfish (2)
			varbitValue = 2;
		}

		return ofVarbitValue(varbitValue);
	}

}

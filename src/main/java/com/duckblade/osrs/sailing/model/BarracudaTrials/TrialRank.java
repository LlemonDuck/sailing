package com.duckblade.osrs.sailing.model.BarracudaTrials;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum TrialRank
{
	NONE(0),
	UNRANKED(1),
	SWORDFISH(2),
	SHARK(3),
	MARLIN(4);

	private final int rank;
	private static final Map<Integer, TrialRank> IDS = new HashMap<>();

	static
	{
		for (TrialRank r : TrialRank.values())
		{
			IDS.put(r.rank, r);
		}
	}

	TrialRank(int rank)
	{
		this.rank = rank;
	}

	public static TrialRank fromId(int id)
	{
		return IDS.getOrDefault(id, NONE);
	}
}

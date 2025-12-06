package com.duckblade.osrs.sailing.features.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrewAssignment
{

	SAILS(4),
	REPAIRS(5),
	WIND_CATCHER(7),
	HOOK_SKIFF(10),
	HOOK_SLOOP_1(13),
	HOOK_SLOOP_2(14),
	;

	private final int varbValue;

	public static CrewAssignment fromCrewAssignmentVarb(int varbitValue)
	{
		for (CrewAssignment slot : values())
		{
			if (slot.getVarbValue() == varbitValue)
			{
				return slot;
			}
		}

		return null;
	}

}

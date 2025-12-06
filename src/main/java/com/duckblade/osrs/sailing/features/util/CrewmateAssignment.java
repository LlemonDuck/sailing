package com.duckblade.osrs.sailing.features.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrewmateAssignment
{

	SAILS(4),
	REPAIRS(5),
	WIND_CATCHER(7),
	HOOK_SKIFF(10),
	HOOK_SLOOP_1(13),
	HOOK_SLOOP_2(14),
	;

	private final int varbValue;

	public static CrewmateAssignment fromCrewAssignmentVarb(int varbitValue)
	{
		for (CrewmateAssignment slot : values())
		{
			if (slot.getVarbValue() == varbitValue)
			{
				return slot;
			}
		}

		return null;
	}

	public boolean isHook()
	{
		switch (this)
		{
			case HOOK_SLOOP_1:
			case HOOK_SLOOP_2:
			case HOOK_SKIFF:
				return true;

			default:
				return false;
		}
	}

}

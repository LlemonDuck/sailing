package com.duckblade.osrs.sailing;

import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.coords.WorldPoint;

@RequiredArgsConstructor
@Getter
public enum SeaChartTask
{

	;

	private final int taskId;
	@Varbit
	private final int varbitId;
	private final WorldPoint point;
	@Nullable
	private final NPC npc;
	@Nullable
	private final GameObject gameObject;

}

package com.duckblade.osrs.sailing.features.barracudatrials;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldArea;

@Getter
@RequiredArgsConstructor
public enum BarracudaTrial
{

	TEMPOR_TANTRUM(new WorldArea(2944, 2751, 3136 - 2944, 2943 - 2751, 0)),
	JUBBLY_JIVE(new WorldArea(2210, 2880, 2488 - 2210, 3072 - 2880, 0)),
	GWENITH_GLIDE(new WorldArea(0, 0, 0, 0, 0)),
	;

	private final WorldArea area;

}

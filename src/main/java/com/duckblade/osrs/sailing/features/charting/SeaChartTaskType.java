package com.duckblade.osrs.sailing.features.charting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

@Getter
@RequiredArgsConstructor
public enum SeaChartTaskType
{
	GENERIC("Oddity", ItemID.SAILING_LOG_INITIAL),
	SPYGLASS("Spyglass", ItemID.SAILING_CHARTING_SPYGLASS),
	DRINK_CRATE("Sealed crate", ItemID.SAILING_CHARTING_CROWBAR),
	CURRENT_DUCK("Current duck", ItemID.SAILING_CHARTING_CURRENT_DUCK),
	MERMAID_GUIDE("Diving", ItemID.HUNDRED_PIRATE_DIVING_HELMET),
	WEATHER("Weather", ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY),
	;

	private final String name;
	private final int iconItem;
}

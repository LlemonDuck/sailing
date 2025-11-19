package com.duckblade.osrs.sailing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("sailing")
public interface SailingConfig extends Config
{

	enum ShowChartsMode
	{
		NONE,
		UNCHARTED,
		ALL,
		;
	}

	@ConfigItem(
		keyName = "showCharts",
		name = "Highlight Sea Charting Locations",
		description = "Highlight nearby sea charting locations."
	)
	default ShowChartsMode showCharts()
	{
		return ShowChartsMode.UNCHARTED;
	}

	@ConfigItem(
		keyName = "highlightTrimmableSails",
		name = "Highlight Trimmable Sails",
		description = "Highlight sails when they require trimming."
	)
	default boolean highlightTrimmableSails()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightRapids",
		name = "Highlight Rapids",
		description = "Highlight rapids."
	)
	default boolean highlightRapids()
	{
		return true;
	}

	@ConfigItem(
		keyName = "disableSailsWhenNotAtHelm",
		name = "Sails when at helm only",
		description = "Deprioritizes sail options when not at the helm."
	)
	default boolean disableSailsWhenNotAtHelm()
	{
		return true;
	}

}

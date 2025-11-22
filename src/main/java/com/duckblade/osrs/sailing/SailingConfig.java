package com.duckblade.osrs.sailing;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(SailingConfig.CONFIG_GROUP)
public interface SailingConfig extends Config
{

	String CONFIG_GROUP = "sailing";

	@ConfigSection(
		name = "Sea Charting",
		description = "Settings for Sea Charting",
		position = 100,
		closedByDefault = true
	)
	String SECTION_SEA_CHARTING = "seaCharting";

	@ConfigSection(
		name = "Barracuda Trials",
		description = "Settings for Barracuda Trials",
		position = 200,
		closedByDefault = true
	)
	String SECTION_BARRACUDA_TRIALS = "barracudaTrials";

	@ConfigSection(
		name = "Menu Entry Swaps",
		description = "Settings for Menu Entry Swaps",
		position = 300,
		closedByDefault = true
	)
	String SECTION_MES = "mes";

	@ConfigSection(
		name = "Cargo Hold Tracking",
		description = "Settings for tracking the contents of your cargo hold.",
		position = 400,
		closedByDefault = true
	)
	String SECTION_CARGO_HOLD_TRACKING = "cargoHoldTracking";

	@ConfigSection(
			name = "Salvage Highlighting",
			description = "Settings for highlighting salvage.",
			position = 500,
			closedByDefault = true
	)
	String SECTION_SALVAGE = "salvageHighlight";

	enum ShowChartsMode
	{
		NONE,
		UNCHARTED,
		CHARTED,
		ALL,
		;
	}

	@ConfigItem(
		keyName = "showCharts",
		name = "Highlight Sea Charting Locations",
		description = "Highlight nearby sea charting locations.",
		section = SECTION_SEA_CHARTING,
		position = 1
	)
	default ShowChartsMode showCharts()
	{
		return ShowChartsMode.UNCHARTED;
	}

	@ConfigItem(
		keyName = "chartingUnchartedColor",
		name = "Uncharted Colour",
		description = "Colour to highlight nearby uncharted locations.",
		section = SECTION_SEA_CHARTING,
		position = 2
	)
	@Alpha
	default Color chartingUnchartedColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "chartingChartedColor",
		name = "Charted Colour",
		description = "Colour to highlight nearby charted locations.",
		section = SECTION_SEA_CHARTING,
		position = 3
	)
	@Alpha
	default Color chartingChartedColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		keyName = "chartingWeatherSolver",
		name = "Weather Station Solver",
		description = "Whether to provide a helper for weather charting.",
		section = SECTION_SEA_CHARTING,
		position = 4
	)
	@Alpha
	default boolean chartingWeatherSolver()
	{
		return true;
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
		keyName = "barracudaHighlightLostCrates",
		name = "Highlight Crates",
		description = "Highlight lost crates that need to be collected during Barracuda Trials.",
		section = SECTION_BARRACUDA_TRIALS,
		position = 1
	)
	default boolean barracudaHighlightLostCrates()
	{
		return true;
	}

	@ConfigItem(
		keyName = "barracudaHighlightLostCratesColour",
		name = "Crate Colour",
		description = "The colour to highlight lost crates.",
		section = SECTION_BARRACUDA_TRIALS,
		position = 2
	)
	@Alpha
	default Color barracudaHighlightLostCratesColour()
	{
		return Color.ORANGE;
	}

	@ConfigItem(
		keyName = "disableSailsWhenNotAtHelm",
		name = "Sails At Helm Only",
		description = "Deprioritizes sail options when not at the helm.",
		section = SECTION_MES,
		position = 1
	)
	default boolean disableSailsWhenNotAtHelm()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prioritizeCargoHold",
		name = "Prioritize Cargo Hold",
		description = "Make the Cargo Hold easier to click on by prioritizing it over other objects.",
		section = SECTION_MES,
		position = 2
	)
	default boolean prioritizeCargoHold()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cargoHoldShowCounts",
		name = "Show Item Count",
		description = "Shows total item counts over the cargo hold.",
		section = SECTION_CARGO_HOLD_TRACKING,
		position = 1
	)
	default boolean cargoHoldShowCounts()
	{
		return true;
	}


	@ConfigItem(
			keyName = "salvageHighlight",
			name = "Highlight Salvage",
			description = "Shows which ships you can salvage.",
			section = SECTION_SALVAGE,
			position = 1
	)
	default boolean salvageHighlight()
	{
		return true;
	}

	@ConfigItem(
			keyName = "salvageHighlightColor",
			name = "Salvage Colour",
			description = "Colour to highlight ships you can salvage.",
			section = SECTION_SALVAGE,
			position = 2
	)
	@Alpha
	default Color salvageHighlightColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
			keyName = "salvageHighlightSunkColor",
			name = "Salvage Sunk Colour",
			description = "Colour to highlight ships that are sunken.",
			section = SECTION_SALVAGE,
			position = 3
	)
	@Alpha
	default Color salvageHighlightSunkColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
			keyName = "salvageHighlightNotLevelColor",
			name = "Salvage Unmet Requirement Colour",
			description = "Colour to highlight ships that you don't have the level for.",
			section = SECTION_SALVAGE,
			position = 4
	)
	@Alpha
	default Color salvageHighlightNotLevelColor()
	{
		return Color.RED;
	}
}

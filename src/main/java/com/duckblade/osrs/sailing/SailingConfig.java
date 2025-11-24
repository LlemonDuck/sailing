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

	// Config sections — keep at the top in declared order
	@ConfigSection(
		name = "Navigation",
		description = "Settings for navigating the world with your ship.",
		position = 100,
		closedByDefault = true
	)
	String SECTION_NAVIGATION = "navigation";

	@ConfigSection(
		name = "Facilities",
		description = "Settings for your ship facilities and components.",
		position = 200,
		closedByDefault = true
	)
	String SECTION_FACILITIES = "facilities";

	@ConfigSection(
		name = "Crewmates",
		description = "Settings for your crewmates.",
		position = 300,
		closedByDefault = true
	)
	String SECTION_CREWMATES = "crewmates";

	@ConfigSection(
		name = "Menu Entry Swaps",
		description = "Settings for Menu Entry Swaps",
		position = 400,
		closedByDefault = true
	)
	String SECTION_MES = "mes";

	@ConfigSection(
		name = "Sea Charting",
		description = "Settings for Sea Charting",
		position = 500,
		closedByDefault = true
	)
	String SECTION_SEA_CHARTING = "seaCharting";

	@ConfigSection(
		name = "Barracuda Trials",
		description = "Settings for Barracuda Trials",
		position = 600,
		closedByDefault = true
	)
	String SECTION_BARRACUDA_TRIALS = "barracudaTrials";

	@ConfigSection(
		name = "Courier Tasks",
		description = "Settings for courier tasks (aka port tasks)",
		position = 700,
		closedByDefault = true
	)
	String SECTION_COURIER_TASKS = "courier";

	@ConfigSection(
		name = "Cargo Hold Tracking",
		description = "Settings for tracking the contents of your cargo hold.",
		position = 800,
		closedByDefault = true
	)
	String SECTION_CARGO_HOLD_TRACKING = "cargoHoldTracking";

	@ConfigSection(
		name = "Boat Hider",
		description = "Settings for hiding parts of the boat.",
		position = 900,
		closedByDefault = true
	)
	String SECTION_BOAT_HIDER = "boatHider";

	@ConfigItem(
		keyName = "highlightRapids",
		name = "Highlight Rapids",
		description = "Highlight rapids.",
		section = SECTION_NAVIGATION,
		position = 1
	)
	default boolean highlightRapids()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightTrimmableSails",
		name = "Highlight Trimmable Sails",
		description = "Highlight sails when they require trimming.",
		section = SECTION_FACILITIES,
		position = 1
	)
	default boolean highlightTrimmableSails()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightSail",
		name = "Highlight Sail",
		description = "Highlight outline of the sail.",
		section = SECTION_FACILITIES,
		position = 2
	)
	default boolean highlightSail()
	{
		return false;
	}

	@ConfigItem(
		keyName = "colorSailOutline",
		name = "Sail",
		description = "Outline color of the sail.",
		section = SECTION_FACILITIES,
		position = 3
	)
	default Color colorSailOutline()
	{
		return Color.BLACK;
	}

	enum CrewmateMuteMode
	{
		NONE,
		OTHER_BOATS,
		ALL,
		;
	}

	@ConfigItem(
		keyName = "crewmatesMuteOverheads",
		name = "Mute Overhead Text",
		description = "Mutes the overhead text of crewmates.",
		section = SECTION_CREWMATES,
		position = 1
	)
	default CrewmateMuteMode crewmatesMuteOverheads()
	{
		return CrewmateMuteMode.NONE;
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
	default boolean chartingWeatherSolver()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chartingDuckSolver",
		name = "Current Duck Solver",
		description = "Whether to provide a helper for current duck trails.",
		section = SECTION_SEA_CHARTING,
		position = 4
	)
	default boolean chartingDuckSolver()
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
		keyName = "courierItemIdentification",
		name = "Destination on Items",
		description = "Show the destination port on cargo crates in your inventory and cargo hold.",
		section = SECTION_COURIER_TASKS,
		position = 1
	)
	default boolean courierItemIdentification()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cargoHoldShowCounts",
		name = "Show Item Count",
		description = "Shows total item counts over the cargo hold.",
		section = SECTION_CARGO_HOLD_TRACKING,
		position = 1,
		hidden = true // todo
	)
	default boolean cargoHoldShowCounts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideBoatSail",
		name = "Hide Boat Sail",
		description = "Hides the sail of the boat.",
		section = SECTION_BOAT_HIDER,
		position = 1
	)
	default boolean hideBoatSail()
	{
		return false;
	}
}

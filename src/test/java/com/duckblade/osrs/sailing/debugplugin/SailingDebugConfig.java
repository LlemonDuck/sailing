package com.duckblade.osrs.sailing.debugplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SailingDebugConfig.CONFIG_GROUP)
public interface SailingDebugConfig extends Config
{

	String CONFIG_GROUP = "sailingdebug";

	@ConfigItem(
		keyName = "boatInfo",
		name = "Boat Info",
		description = ""
	)
	default boolean boatInfo()
	{
		return false;
	}

	@ConfigItem(
		keyName = "localBoatInfo",
		name = "Local Boat Info Panel",
		description = ""
	)
	default boolean localBoatInfo()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tlwp",
		name = "Top Level WP Default On",
		description = "also toggleable with ::tlwp"
	)
	default boolean tlwp()
	{
		return false;
	}

	@ConfigItem(
		keyName = "courierTaskInfo",
		name = "Courier Task Info Panel",
		description = ""
	)
	default boolean courierTaskInfo()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeOverlayDefaultOn",
		name = "Route Default On",
		description = "also toggleable with ::route"
	)
	default boolean routeOverlayDefaultOn()
	{
		return true;
	}

	@ConfigItem(
		keyName = "facilities",
		name = "Facilities Overlay",
		description = ""
	)
	default boolean facilities()
	{
		return false;
	}

}

package com.duckblade.osrs.sailing.debugplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("sailingdebug")
public interface SailingDebugConfig extends Config
{

	@ConfigItem(
		keyName = "boatInfoDefaultOn",
		name = "Boat Info Default On",
		description = "also toggleable with ::boats"
	)
	default boolean boatInfoDefaultOn()
	{
		return false;
	}

	@ConfigItem(
		keyName = "localBoatInfo",
		name = "Local Boat Info Panel",
		description = "also toggleable with ::tlwp"
	)
	default boolean localBoatInfo()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tlwpOverlayDefaultOn",
		name = "Top Level WP Default On",
		description = ""
	)
	default boolean tlwpOverlayDefaultOn()
	{
		return true;
	}

}

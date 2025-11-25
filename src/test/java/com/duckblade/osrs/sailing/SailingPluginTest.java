package com.duckblade.osrs.sailing;

import com.duckblade.osrs.sailing.debugplugin.SailingDebugPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SailingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(
			SailingPlugin.class,
			SailingDebugPlugin.class
		);
		RuneLite.main(args);
	}
}

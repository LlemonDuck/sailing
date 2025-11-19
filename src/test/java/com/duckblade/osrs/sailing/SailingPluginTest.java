package com.duckblade.osrs.sailing;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SailingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SailingPlugin.class);
		RuneLite.main(args);
	}
}

package com.duckblade.osrs.sailing.debugplugin;

import com.duckblade.osrs.sailing.debugplugin.module.DebugComponentManager;
import com.duckblade.osrs.sailing.debugplugin.module.DebugModule;
import com.google.inject.Binder;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "SailingDebug",
	developerPlugin = true
)
public class SailingDebugPlugin extends Plugin
{

	@Inject
	private DebugComponentManager componentManager;

	@Override
	public void configure(Binder binder)
	{
		binder.install(new DebugModule());
	}

	@Override
	protected void startUp() throws Exception
	{
		componentManager.onPluginStart();
	}

	@Override
	protected void shutDown() throws Exception
	{
		componentManager.onPluginStop();
	}
}

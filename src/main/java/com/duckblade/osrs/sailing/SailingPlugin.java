package com.duckblade.osrs.sailing;

import com.duckblade.osrs.sailing.module.ComponentManager;
import com.duckblade.osrs.sailing.module.SailingModule;
import com.google.inject.Binder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = "Sailing"
)
public class SailingPlugin extends Plugin
{

	@Inject
	private ComponentManager componentManager;

	@Override
	public void configure(Binder binder)
	{
		binder.install(new SailingModule());
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

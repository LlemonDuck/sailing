package com.duckblade.osrs.sailing;

import com.duckblade.osrs.sailing.module.ComponentManager;
import com.duckblade.osrs.sailing.module.SailingModule;
import com.google.inject.Binder;
import java.io.File;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Sailing"
)
public class SailingPlugin extends Plugin
{

	public static final File SAILING_DIR = new File(RuneLite.RUNELITE_DIR, "sailing");

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

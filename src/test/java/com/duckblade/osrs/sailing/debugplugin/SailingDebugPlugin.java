package com.duckblade.osrs.sailing.debugplugin;

import com.google.inject.Inject;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "SailingDebug",
	developerPlugin = true
)
public class SailingDebugPlugin extends Plugin
{

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private SailingDebugConfig config;

	@Inject
	private SailingDebugBoatInfoOverlay boatInfoOverlayPanel;

	@Inject
	private SailingDebugLocalBoatInfoOverlayPanel localBoatInfoOverlayPanel;

	@Inject
	private SailingDebugTlwpOverlay tlwpOverlay;

	private List<?> components;

	@Override
	protected void startUp() throws Exception
	{
		components = Arrays.asList(
			boatInfoOverlayPanel,
			localBoatInfoOverlayPanel,
			tlwpOverlay
		);

		components.forEach(c ->
		{
			eventBus.register(c);
			if (c instanceof Overlay)
			{
				overlayManager.add((Overlay) c);
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		components.forEach(c ->
		{
			if (c instanceof Overlay)
			{
				overlayManager.remove((Overlay) c);
			}
			eventBus.unregister(c);
		});
	}

	@Provides
	public SailingDebugConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SailingDebugConfig.class);
	}
}

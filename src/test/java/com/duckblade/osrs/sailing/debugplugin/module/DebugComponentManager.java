package com.duckblade.osrs.sailing.debugplugin.module;

import com.duckblade.osrs.sailing.debugplugin.SailingDebugConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.GameEventManager;

/**
 * Manages all the subcomponents of the plugin
 * so they can register themselves to RuneLite resources
 * e.g. EventBus/OverlayManager/init on startup/etc
 * instead of the {@link com.duckblade.osrs.sailing.SailingPlugin} class handling everything.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DebugComponentManager
{

	private final EventBus eventBus;
	private final OverlayManager overlayManager;
	private final InfoBoxManager infoBoxManager;
	private final GameEventManager gameEventManager;
	private final SailingDebugConfig config;
	private final Set<DebugLifecycleComponent> components;

	private final Map<DebugLifecycleComponent, Boolean> states = new HashMap<>();

	public void onPluginStart()
	{
		eventBus.register(this);
		components.forEach(c -> states.put(c, false));
		revalidateComponentStates();
	}

	public void onPluginStop()
	{
		eventBus.unregister(this);
		components.stream()
			.filter(states::get)
			.forEach(this::tryShutDown);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!SailingDebugConfig.CONFIG_GROUP.equals(e.getGroup()))
		{
			return;
		}

		revalidateComponentStates();
	}

	private void revalidateComponentStates()
	{
		components.forEach(c ->
		{
			boolean shouldBeEnabled = c.isEnabled(config);
			boolean isEnabled = states.get(c);
			if (shouldBeEnabled == isEnabled)
			{
				return;
			}

			if (shouldBeEnabled)
			{
				tryStartUp(c);
			}
			else
			{
				tryShutDown(c);
			}
		});
	}

	private void tryStartUp(DebugLifecycleComponent component)
	{
		if (states.get(component))
		{
			return;
		}

		if (log.isDebugEnabled())
		{
			log.debug("Enabling Sailing component [{}]", component.getClass().getName());
		}

		try
		{
			component.startUp();

			eventBus.register(component);
			if (component instanceof Overlay)
			{
				overlayManager.add((Overlay) component);
			}
			if (component instanceof InfoBox)
			{
				infoBoxManager.addInfoBox((InfoBox) component);
			}

			gameEventManager.simulateGameEvents(component);
			states.put(component, true);
		}
		catch (Throwable e)
		{
			log.error("Failed to start Sailing component [{}]", component.getClass().getName(), e);
		}
	}

	private void tryShutDown(DebugLifecycleComponent component)
	{
		eventBus.unregister(component);
		if (component instanceof Overlay)
		{
			overlayManager.remove((Overlay) component);
		}
		if (component instanceof InfoBox)
		{
			infoBoxManager.removeInfoBox((InfoBox) component);
		}

		if (!states.get(component))
		{
			return;
		}

		if (log.isDebugEnabled())
		{
			log.debug("Disabling Sailing component [{}]", component.getClass().getName());
		}

		try
		{
			component.shutDown();
		}
		catch (Throwable e)
		{
			log.error("Failed to cleanly shut down Sailing component [{}]", component.getClass().getName());
		}
		finally
		{
			states.put(component, false);
		}
	}

}

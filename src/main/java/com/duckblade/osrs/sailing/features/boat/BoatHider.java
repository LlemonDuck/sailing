package com.duckblade.osrs.sailing.features.boat;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BoatHider implements PluginLifecycleComponent, RenderCallback
{
	@Inject
	private Client client;
	@Inject
	private SailingConfig config;
	@Inject
	private BoatTracker boatTracker;
	@Inject
	private RenderCallbackManager renderCallbackManager;

	private Boat boat;

	@Override
	public boolean isEnabled(final SailingConfig config)
	{
		return config.hideBoatSail();
	}

	@Override
	public void startUp()
	{
		renderCallbackManager.register(this);
	}

	@Override
	public void shutDown()
	{
		renderCallbackManager.unregister(this);
	}

	@Subscribe
	public void onGameTick(final GameTick e)
	{
		if (boat == null && SailingUtil.isSailing(client))
		{
			// must be called on client thread
			boat = boatTracker.getBoat();
		}
	}

	@Override
	public boolean drawObject(final Scene scene, final TileObject object)
	{
		if (boat != null)
		{
			final var wv = object.getWorldView();
			if (boat.getWorldViewId() == wv.getId())
			{
				return !(object == boat.getSail() && config.hideBoatSail());
			}
		}
		return true;
	}
}

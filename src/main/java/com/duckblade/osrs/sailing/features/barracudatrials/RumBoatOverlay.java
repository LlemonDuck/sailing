package com.duckblade.osrs.sailing.features.barracudatrials;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BarracudaTrialTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Singleton
public class RumBoatOverlay
		extends Overlay
		implements PluginLifecycleComponent
{
	private static final int TEMPOR_TANTRUM_RUM_BOAT_X_COORD_SEPARATOR = 3855;

	private static final int TEMPOR_TANTRUM_RUM_BOAT_HULL_OBJECT = 58930;
	private static final int TEMPOR_TANTRUM_RUM_BOAT_SAIL_OBJECT = 58932;
	private static final int TEMPOR_TANTRUM_RUM_BOAT_HELM_OBJECT = 58933;

	private static final Set<Integer> TEMPOR_TANTRUM_RUM_BOAT_OBJECTS = ImmutableSet.of(
			TEMPOR_TANTRUM_RUM_BOAT_HELM_OBJECT,
			TEMPOR_TANTRUM_RUM_BOAT_HULL_OBJECT,
			TEMPOR_TANTRUM_RUM_BOAT_SAIL_OBJECT
	);

	private final Client client;
	private final SailingConfig config;
	private final BarracudaTrialTracker trialTracker;

	private final Set<GameObject> rumBoatObjects = new HashSet<>();
	private Color rumBoatColor;

	@Inject
	public RumBoatOverlay(Client client, SailingConfig config, BarracudaTrialTracker trialTracker)
	{
		this.client = client;
		this.config = config;
		this.trialTracker = trialTracker;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		rumBoatColor = config.barracudaHighlightTemporTantrumRumBoatColor();
		return config.barracudaHighlightTemporTantrumRumBoat();
	}

	@Override
	public void shutDown()
	{
		rumBoatObjects.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGIN_SCREEN || e.getGameState() == GameState.HOPPING)
		{
			rumBoatObjects.clear();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();

		if (TEMPOR_TANTRUM_RUM_BOAT_OBJECTS.contains(o.getId()))
		{
			rumBoatObjects.add(o);
		}
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		rumBoatObjects.removeIf(o -> o.getWorldView() == e.getWorldView());
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!SailingUtil.isSailing(client) || !config.barracudaHighlightTemporTantrumRumBoat()
				|| !trialTracker.isInTrial() || rumBoatObjects.isEmpty())
		{
			return null;
		}

		for (GameObject o : rumBoatObjects)
		{
			if (o == null || o.getWorldView() == null)
			{
				continue;
			}

			if (shouldHighlight(o))
			{
				Shape convexHull = o.getConvexHull();
				if (convexHull != null)
				{
					OverlayUtil.renderPolygon(g, convexHull, rumBoatColor);
				}
			}
		}

		return null;
	}

	// If rum is NOT COLLECTED and GREATER THAN 3855, display.
	// If rum is COLLECTED AND LESS THAN 3855, display.
	private boolean shouldHighlight(GameObject o)
	{
		WorldPoint wp = o.getWorldLocation();

		// WorldView IDs are not ideally update-safe, per Adam/Abex, so strive away from using them.
		// Both boat object sets share the same region, so we cannot differentiate with them as the objectIds are the same.
		return (trialTracker.isRumCollected() && wp.getX() < TEMPOR_TANTRUM_RUM_BOAT_X_COORD_SEPARATOR)
				|| (!trialTracker.isRumCollected() && wp.getX() > TEMPOR_TANTRUM_RUM_BOAT_X_COORD_SEPARATOR);
	}
}

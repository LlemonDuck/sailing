package com.duckblade.osrs.sailing.features.barracudatrials;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
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
public class BarracudaTrialHelper
	extends Overlay
	implements PluginLifecycleComponent
{
	private static final Set<Integer> LOST_CARGO_MORPHED_COLLECTABLES = ImmutableSet.of(
			ObjectID.SAILING_BT_GWENITH_GLIDE_COLLECTABLE_SUPPLIES,
			ObjectID.SAILING_BT_JUBBLY_JIVE_COLLECTABLE_SUPPLIES,
			ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_SUPPLIES
	);

	private final Client client;
	private final SailingConfig config;

	private boolean inTrial;
	private Set<GameObject> lostCargo = new HashSet<>();
	private Color crateColour;

	@Inject
	public BarracudaTrialHelper(Client client, SailingConfig config)
	{
		this.client = client;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		crateColour = config.barracudaHighlightLostCratesColour();
		return config.barracudaHighlightLostCrates();
	}

	@Override
	public void shutDown()
	{
		inTrial = false;
		lostCargo.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGIN_SCREEN || e.getGameState() == GameState.HOPPING)
		{
			inTrial = false;
			lostCargo.clear();
			log.debug("onGameStateChanged: cleared lost crates");
		}
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		if (client.getGameState() != GameState.LOGGED_IN || !inTrial)
			return;

		if (!lostCargo.isEmpty())
		{
			lostCargo.clear();
		}

		lostCargo = SailingUtil.getWorldViewGameObjects(client, LOST_CARGO_MORPHED_COLLECTABLES);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarbitId() == VarbitID.SAILING_BT_IN_TRIAL)
		{
			if (e.getValue() > 0) // In Trial, regardless of difficulty
			{
				inTrial = true;
				log.debug("entered trial");
			}
			else
			{
				inTrial = false;
				lostCargo.clear();
				log.debug("exited trial");
			}
		}
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		for (GameObject cargo : lostCargo)
		{
			if (cargo != null)
			{
				if (config.barracudaExpandHighlightLostCrates())
				{
					Polygon poly = SailingUtil.getExpandedGameObjectPoly(client, cargo, 4);

					if (poly != null)
					{
						OverlayUtil.renderPolygon(g, poly, crateColour);
					}
				}
				else
				{
					OverlayUtil.renderTileOverlay(g, cargo, "", crateColour);
				}
			}
		}

		return null;
	}
}

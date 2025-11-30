package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.Renderable;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
@Singleton
public class CrystalExtractorHighlight
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final int ANIMATION_CRYSTAL_EXTRACTOR_CRYSTAL_HARVESTABLE = 13177;

	private final Client client;

	private final Map<Integer, GameObject> extractors = new HashMap<>();

	private boolean highlightHarvestable;
	private Color harvestableColour;

	private boolean highlightInactive;
	private Color inactiveColour;

	@Inject
	public CrystalExtractorHighlight(Client client)
	{
		this.client = client;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		highlightHarvestable = config.highlightCrystalExtractorHarvestable();
		harvestableColour = config.highlightCrystalExtractorHarvestableColour();

		highlightInactive = config.highlightCrystalExtractorInactive();
		inactiveColour = config.highlightCrystalExtractorInactiveColour();

		return highlightHarvestable || highlightInactive;
	}

	@Override
	public void shutDown()
	{
		extractors.clear();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject go = e.getGameObject();
		if (go.getId() == ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED ||
			go.getId() == ObjectID.SAILING_CRYSTAL_EXTRACTOR_DEACTIVATED)
		{
			extractors.put(go.getWorldView().getId(), go);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		if (e.getGameObject().getId() == ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED ||
			e.getGameObject().getId() == ObjectID.SAILING_CRYSTAL_EXTRACTOR_DEACTIVATED)
		{
			extractors.remove(e.getGameObject().getWorldView().getId());
		}
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		extractors.remove(e.getWorldView().getId());
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!SailingUtil.isSailing(client))
		{
			return null;
		}

		int wvId = client.getLocalPlayer().getWorldView().getId();
		GameObject extractor = extractors.get(wvId);
		Shape hull = extractor != null ? extractor.getConvexHull() : null;
		if (extractor == null || hull == null)
		{
			return null;
		}

		if ((extractor.getId() == ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED && !highlightHarvestable) ||
			(extractor.getId() == ObjectID.SAILING_CRYSTAL_EXTRACTOR_DEACTIVATED && !highlightInactive))
		{
			return null;
		}

		if (extractor.getId() == ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED)
		{
			Renderable r = extractor.getRenderable();
			if (!(r instanceof DynamicObject))
			{
				return null;
			}

			DynamicObject dyn = (DynamicObject) r;
			int anim = dyn.getAnimation() != null ? dyn.getAnimation().getId() : -1;
			if (anim != ANIMATION_CRYSTAL_EXTRACTOR_CRYSTAL_HARVESTABLE)
			{
				return null;
			}
		}

		Color colour = extractor.getId() == ObjectID.SAILING_CRYSTAL_EXTRACTOR_ACTIVATED ? harvestableColour : inactiveColour;
		OverlayUtil.renderPolygon(g, hull, colour);

		return null;
	}
}

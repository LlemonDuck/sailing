package com.duckblade.osrs.sailing.features.navigation;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
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

@Singleton
public class LightningCloudsOverlay
		extends Overlay
		implements PluginLifecycleComponent
{

	private static final ImmutableSet<Integer> LIGHTNING_CLOUDS_WARNING_ANIM_IDS = ImmutableSet.of(
			AnimationID.TEMPOROSS_LIGHTNING_CLOUD_CHARGING_IDLE,
			AnimationID.SAILING_LIGHTNING_CLOUD_ATTACK
	);

	private final Client client;
	private final SailingConfig config;

	private final Set<NPC> clouds = new HashSet<>();

	private Color cloudColor;

	@Inject
	public LightningCloudsOverlay(Client client, SailingConfig config, BoatTracker boatTracker)
	{
		this.client = client;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		cloudColor = config.lightningCloudStrikeColour();
		return config.highlightLightningCloudStrikes();
	}

	public void shutDown()
	{
		clouds.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		NPC npc = e.getNpc();

		if (npc.getId() == NpcID.SAILING_SEA_STORMY_CLOUD)
		{
			clouds.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		clouds.remove(e.getNpc());
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		if (e.getWorldView().isTopLevel())
		{
			client.getTopLevelWorldView()
					.npcs()
					.stream()
					.forEach(clouds::remove);
		}
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!SailingUtil.isSailing(client) || !config.highlightLightningCloudStrikes())
		{
			return null;
		}

		for (NPC cloud : clouds)
		{
			int anim = cloud.getAnimation();

			if (LIGHTNING_CLOUDS_WARNING_ANIM_IDS.contains(anim))
			{
				Color color = (anim == AnimationID.SAILING_LIGHTNING_CLOUD_ATTACK ? cloudColor.darker() : cloudColor);
				OverlayUtil.renderActorOverlay(g, cloud, "", color);
			}
		}

		return null;
	}
}
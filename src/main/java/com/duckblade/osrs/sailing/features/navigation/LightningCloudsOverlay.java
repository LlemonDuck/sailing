package com.duckblade.osrs.sailing.features.navigation;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
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
public class LightningCloudsOverlay
		extends Overlay
		implements PluginLifecycleComponent
{
	private static final Integer SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_ID = 15490;
	private static final Integer SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_CHARGING_ANIM = 8876;
	private static final Integer SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_CHARGED_ANIM = 8877;
	private static final Integer SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_STRIKING_ANIM = 13141;
	private static final Integer SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_SETTLING_DOWN_ANIM = 8879; //

	private static final ImmutableSet<Integer> SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_WARNING_ANIMS = ImmutableSet.of(
			SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_CHARGED_ANIM,
			SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_STRIKING_ANIM
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
		cloudColor = config.lightningCloudStrikeColor();
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

		if (npc.getId() == SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_ID)
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
			clouds.clear();
			client.getTopLevelWorldView().npcs().stream()
					.filter(npc -> npc.getId() == SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_ID)
					.forEach(clouds::add);
		}
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (SailingUtil.isSailing(client) && !config.highlightLightningCloudStrikes())
		{
			return null;
		}

		for (NPC cloud : clouds)
		{
			int anim = cloud.getAnimation();

			if (SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_WARNING_ANIMS.contains(anim))
			{
				Color color = (anim == SAILING_BT_TEMPOR_TANTRUM_LIGHTNING_CLOUD_STRIKING_ANIM ? cloudColor.darker() : cloudColor);
				OverlayUtil.renderActorOverlay(g, cloud, "", color);
			}
		}

		return null;
	}
}

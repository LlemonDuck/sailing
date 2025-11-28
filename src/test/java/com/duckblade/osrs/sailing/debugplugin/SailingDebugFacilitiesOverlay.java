package com.duckblade.osrs.sailing.debugplugin;

import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.model.SalvagingHookTier;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class SailingDebugFacilitiesOverlay extends Overlay
{

	private final Client client;
	private final BoatTracker boatTracker;

	private final Set<String> facilityTypes = new HashSet<>();

	private boolean active;

	@Inject
	public SailingDebugFacilitiesOverlay(Client client, BoatTracker boatTracker, SailingDebugConfig config)
	{
		this.client = client;
		this.boatTracker = boatTracker;
		active = config.boatInfoDefaultOn();

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!active || !SailingUtil.isSailing(client))
		{
			return null;
		}

		Boat boat = boatTracker.getBoat();
		renderFacility(graphics, Color.CYAN, "sail", boat.getSail(), boat.getSailTier());
		renderFacility(graphics, Color.ORANGE, "helm", boat.getHelm(), boat.getHelmTier());
		renderFacility(graphics, Color.GREEN, "cargo", boat.getCargoHold(), boat.getCargoHoldTier());
		for (GameObject hook : boat.getSalvagingHooks())
		{
			renderFacility(graphics, Color.RED, "hook", hook, SalvagingHookTier.fromGameObjectId(hook.getId()));
		}

		return null;
	}

	private void renderFacility(Graphics2D graphics, Color colour, String type, GameObject o, Object tier)
	{
		if (o == null || (!facilityTypes.isEmpty() && !facilityTypes.contains(type)))
		{
			return;
		}

		OverlayUtil.renderTileOverlay(graphics, o, type + "=" + tier, colour);
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted e)
	{
		if (!e.getCommand().equals("facilities"))
		{
			return;
		}

		if (e.getArguments().length == 0)
		{
			active = !active;
			return;
		}

		if (!facilityTypes.add(e.getArguments()[0]))
		{
			facilityTypes.remove(e.getArguments()[0]);
		}
	}

}

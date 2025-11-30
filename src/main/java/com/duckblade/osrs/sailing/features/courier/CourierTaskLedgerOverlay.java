package com.duckblade.osrs.sailing.features.courier;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.model.CourierTask;
import com.duckblade.osrs.sailing.model.Port;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
@Singleton
public class CourierTaskLedgerOverlay
	extends Overlay
	implements PluginLifecycleComponent
{

	private final SailingConfig config;
	private final CourierTaskTracker taskTracker;

	private Color ledgerPickupColour;
	private Color ledgerDropOffColour;

	@Inject
	public CourierTaskLedgerOverlay(SailingConfig config, CourierTaskTracker taskTracker)
	{
		super();
		this.taskTracker = taskTracker;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		ledgerPickupColour = config.courierItemPickupOverlayColor();
		ledgerDropOffColour = config.courierItemDropOffOverlayColor();
		return config.courierItemShowDropOffOverlay() || config.courierItemShowPickupOverlay();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Port activePort = taskTracker.getActivePort();
		GameObject activeLedger = taskTracker.getActiveLedger();
		if (activePort == null || activeLedger == null)
		{
			return null;
		}

		Shape hull = activeLedger.getConvexHull();
		if (hull == null)
		{
			return null;
		}

		List<CourierTask> pickupTasks = taskTracker.getPickupTasksForPort(activePort);
		boolean allCargoRetrieved = pickupTasks.stream().allMatch(CourierTask::hasRetrievedAllCargo);
		if (!allCargoRetrieved && config.courierItemShowPickupOverlay())
		{
			OverlayUtil.renderPolygon(graphics, hull, ledgerPickupColour);
		}

		List<CourierTask> dropOffTasks = taskTracker.getDropOffTasksForPort(activePort);
		boolean allCargoDelivered = dropOffTasks.stream().allMatch(CourierTask::hasDeliveredAllCargo);
		if (!allCargoDelivered && config.courierItemShowDropOffOverlay())
		{
			OverlayUtil.renderPolygon(graphics, hull, ledgerDropOffColour);
		}

		return null;
	}
}

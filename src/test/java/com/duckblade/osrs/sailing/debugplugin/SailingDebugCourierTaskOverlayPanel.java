package com.duckblade.osrs.sailing.debugplugin;

import com.duckblade.osrs.sailing.features.courier.CourierTaskTracker;
import com.duckblade.osrs.sailing.model.CourierTask;
import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Slf4j
public class SailingDebugCourierTaskOverlayPanel extends OverlayPanel
{

	private final CourierTaskTracker taskTracker;
	private final SailingDebugConfig config;

	@Inject
	public SailingDebugCourierTaskOverlayPanel(SailingDebugConfig config, CourierTaskTracker taskTracker)
	{
		this.taskTracker = taskTracker;
		this.config = config;

		setPreferredPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Set<CourierTask> tasks = taskTracker.getTasks();
		if (!config.courierTaskInfo() || tasks.isEmpty())
		{
			return null;
		}

		getPanelComponent().getChildren()
			.add(TitleComponent.builder()
				.text("Courier Tasks")
				.build());

		for (CourierTask task : tasks)
		{
			getPanelComponent().getChildren()
				.add(LineComponent.builder()
					.left(task.getFromPort().toString())
					.right(task.getToPort().toString())
					.build());

			boolean isRetrieved = task.getNumCargoRetrieved() == task.getCargoAmount();
			boolean isDelivered = task.getNumCargoDelivered() == task.getCargoAmount();
			getPanelComponent().getChildren()
				.add(LineComponent.builder()
					.left("RETRIEVED")
					.leftColor(isRetrieved ? Color.GREEN : Color.RED)
					.right("DELIVERED")
					.rightColor(isDelivered ? Color.GREEN : Color.RED)
					.build());
		}

		return super.render(graphics);
	}
}

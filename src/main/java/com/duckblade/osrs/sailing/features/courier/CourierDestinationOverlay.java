package com.duckblade.osrs.sailing.features.courier;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.model.Port;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

@Slf4j
@Singleton
public class CourierDestinationOverlay
	extends WidgetItemOverlay
	implements PluginLifecycleComponent
{

	// item id -> destination
	private static final Map<Integer, Port> ITEM_DESTINATIONS = Arrays.stream(CourierTask.values())
		.collect(Collectors.toMap(
			CourierTask::getItemID,
			CourierTask::getDropOff
		));

	@Inject
	public CourierDestinationOverlay()
	{
		super();

		showOnInventory();
		showOnEquipment();
		showOnInterfaces(InterfaceID.SAILING_BOAT_CARGOHOLD);
		showOnInterfaces(InterfaceID.SAILING_BOAT_CARGOHOLD_SIDE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.courierItemIdentification();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		Port port = ITEM_DESTINATIONS.get(itemId);
		if (port == null)
		{
			return;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());

		Rectangle bounds = widgetItem.getCanvasBounds();
		TextComponent textComponent = new TextComponent();
		textComponent.setText(port.getShortCode());
		textComponent.setPosition(new Point(bounds.x - 1, bounds.y + bounds.height - 1));
		textComponent.render(graphics);
	}
}

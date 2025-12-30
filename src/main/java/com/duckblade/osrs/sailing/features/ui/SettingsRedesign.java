package com.duckblade.osrs.sailing.features.ui;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.duckblade.osrs.sailing.SailingConfig;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.ScriptID;

@Slf4j
@Singleton
public class SettingsRedesign implements PluginLifecycleComponent
{
	private Client client;
	private ClientThread clientThread;

	@Inject
	public SettingsRedesign(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.expandSailingUI();
	}

	@Override
	public void startUp()
	{
		clientThread.invokeLater(() -> runIfSailingPanelVisible(this::updateUI));
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{		
		if (event.getVarbitId() == VarbitID.SAILING_SIDEPANEL_VISIBLE) {
			if (event.getValue() == 1) {
				clientThread.invokeLater(this::updateUI);
			}
		}

		if (event.getVarbitId() == VarbitID.SAILING_SIDEPANEL_TABS) {
			// value is index of tab
			clientThread.invokeLater(this::updateUI);
		}

		if (event.getVarbitId() == VarbitID.SAILING_SIDEPANEL_CREW_ASSIGNATION) {
			clientThread.invokeLater(() -> runIfSailingPanelVisible(() -> toggleCrewAssignmentTitle(event.getValue() == 0)));
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		if (event.getIndex() == VarClientID.TOPLEVEL_PANEL) {
			clientThread.invokeLater(() -> runIfSailingPanelVisible(this::updateUI));
		}
	}

	protected void runIfSailingPanelVisible(Runnable callback) {
		if (client.getVarcIntValue(VarClientID.TOPLEVEL_PANEL) == 0 && client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_VISIBLE) == 1) {
			callback.run();
		}
	}

	protected void toggleCrewAssignmentTitle(boolean hidden) {
		var sailingTabTitleWidget = client.getWidget(InterfaceID.SailingSidepanel.TAB_TITLE);
		if (sailingTabTitleWidget != null) {
			sailingTabTitleWidget.setHidden(hidden);
		}
	}

	protected void updateUI()
	{
		var sailingBoatName = client.getWidget(InterfaceID.SailingSidepanel.BOAT_NAME);
		if (sailingBoatName != null) {
			sailingBoatName.setHidden(false);
			sailingBoatName.setOriginalY(0);
			sailingBoatName.setOriginalX(28);
			sailingBoatName.setWidthMode(WidgetSizeMode.MINUS);
			sailingBoatName.setOriginalWidth(30);
			sailingBoatName.setHeightMode(WidgetSizeMode.ABSOLUTE);
			sailingBoatName.setOriginalHeight(13);
			sailingBoatName.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			sailingBoatName.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			sailingBoatName.revalidate();

			var sailingBoatNameText = sailingBoatName.getChild(0);
			if (sailingBoatNameText != null) {
				sailingBoatNameText.setOriginalY(0);
				sailingBoatNameText.setOriginalX(0);
				sailingBoatNameText.setWidthMode(WidgetSizeMode.MINUS);
				sailingBoatNameText.setOriginalWidth(0);
				sailingBoatNameText.setHeightMode(WidgetSizeMode.MINUS);
				sailingBoatNameText.setOriginalHeight(0);
				sailingBoatNameText.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
				sailingBoatNameText.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
				// Font changes often when pressing buttons, leave it for now until we find a better solution
				// sailingBoatNameText.setFontId(FontID.PLAIN_11);
				sailingBoatNameText.revalidate();
			}
		}

		var sailingHealthWidget = client.getWidget(InterfaceID.SailingSidepanel.HEALTH_BAR);
		if (sailingHealthWidget != null) {
			sailingHealthWidget.setHidden(false);
			sailingHealthWidget.setOriginalY(13);
			sailingHealthWidget.setOriginalX(28);
			sailingHealthWidget.setWidthMode(WidgetSizeMode.MINUS);
			sailingHealthWidget.setOriginalWidth(30);
			sailingHealthWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			sailingHealthWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			sailingHealthWidget.revalidate();

			sailingHealthWidget.getChild(1).setWidthMode(WidgetSizeMode.MINUS).setOriginalWidth(8);
			sailingHealthWidget.getChild(2).setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT).setOriginalX(0);
			sailingHealthWidget.getChild(4).setWidthMode(WidgetSizeMode.MINUS).setOriginalWidth(8);
			sailingHealthWidget.getChild(5).setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT).setOriginalX(0);
			sailingHealthWidget.getChild(7).setWidthMode(WidgetSizeMode.MINUS).setOriginalWidth(8);
			sailingHealthWidget.getChild(8).setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT).setOriginalX(0);

			var textWidget = sailingHealthWidget.getChild(12);
			if (textWidget == null) {
				textWidget = sailingHealthWidget.getChild(9);
			}
			textWidget.setWidthMode(WidgetSizeMode.MINUS).setOriginalWidth(0);

			for (var child : sailingHealthWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}

		var sailingTabsWidget = client.getWidget(InterfaceID.SailingSidepanel.TABS);
		if (sailingTabsWidget != null) {
			sailingTabsWidget.setOriginalY(28);
			sailingTabsWidget.revalidate();
		}

		var sailingContentsWidget = client.getWidget(InterfaceID.SailingSidepanel.CONTENTS_LAYER);
		if (sailingContentsWidget != null) {
			sailingContentsWidget.setOriginalY(2);
			sailingContentsWidget.setOriginalHeight(210);
			sailingContentsWidget.setOriginalWidth(4);
			sailingContentsWidget.setOriginalX(2);
			sailingContentsWidget.revalidate();
		}

		var sailingTabTitleWidget = client.getWidget(InterfaceID.SailingSidepanel.TAB_TITLE);
		if (sailingTabTitleWidget != null) {
			sailingTabTitleWidget.setHidden(true);
		}

		// Facilities

		var facilitiesContentContainerWidget = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CONTAINER);
		if (facilitiesContentContainerWidget != null) {
			facilitiesContentContainerWidget.setOriginalY(2);
			facilitiesContentContainerWidget.setOriginalHeight(2);
			facilitiesContentContainerWidget.revalidate();
		}

		var facilitiesContentWidget = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_CONTENT);
		if (facilitiesContentWidget != null) {
			facilitiesContentWidget.revalidate();

			for (var child : facilitiesContentWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}

		var facilitiesScrollableWidget = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_SCROLLABLE);
		if (facilitiesScrollableWidget != null) {
			facilitiesScrollableWidget.revalidate();
			for (var child : facilitiesScrollableWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}
		
		var facilitiesScrollbarWidget = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_SCROLLBAR);
		if (facilitiesScrollbarWidget != null) {
			facilitiesScrollbarWidget.revalidate();

			for (var child : facilitiesScrollbarWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}

		var facilitiesDividerWidget = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_DIVIDER);
		if (facilitiesDividerWidget != null) {
			facilitiesDividerWidget.setOriginalX(62);
			facilitiesDividerWidget.revalidate();

			for (var child : facilitiesDividerWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}

		if (facilitiesScrollableWidget != null && facilitiesScrollbarWidget != null) {
			updateScrollbar(facilitiesScrollbarWidget.getId(), facilitiesScrollableWidget.getId());
		}

		// Stats

		var statsContentContainerWidget = client.getWidget(InterfaceID.SailingSidepanel.STATS_CONTENT_CONTAINER);
		if (statsContentContainerWidget != null) {
			statsContentContainerWidget.setOriginalY(2);
			statsContentContainerWidget.setOriginalHeight(2);
			statsContentContainerWidget.revalidate();
		}

		var statsContentWidget = client.getWidget(InterfaceID.SailingSidepanel.STATS_CONTENT);
		if (statsContentWidget != null) {
			statsContentWidget.revalidate();

			for (var child : statsContentWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}

		var statsScrollableWidget = client.getWidget(InterfaceID.SailingSidepanel.STATS_SCROLLABLE);
		if (statsScrollableWidget != null) {
			statsScrollableWidget.revalidate();
			for (var child : statsScrollableWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}
		
		var statsScrollbarWidget = client.getWidget(InterfaceID.SailingSidepanel.STATS_SCROLLBAR);
		if (statsScrollbarWidget != null) {
			statsScrollbarWidget.revalidate();

			for (var child : statsScrollbarWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}

		if (statsScrollableWidget != null && statsScrollbarWidget != null) {
			updateScrollbar(statsScrollbarWidget.getId(), statsScrollableWidget.getId());
		}

		// Crew

		var crewContentContainerWidget = client.getWidget(InterfaceID.SailingSidepanel.CREW_CONTENT_CONTAINER);
		if (crewContentContainerWidget != null) {
			crewContentContainerWidget.setOriginalY(2);
			crewContentContainerWidget.setOriginalHeight(2);
			crewContentContainerWidget.revalidate();
		}

		var crewCapacityTextWidget = client.getWidget(InterfaceID.SailingSidepanel.CREW_CAPACITY_TEXT);
		if (crewCapacityTextWidget != null) {
			crewCapacityTextWidget.setOriginalY(0);
			crewCapacityTextWidget.setOriginalX(0);
			crewCapacityTextWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM);
			crewCapacityTextWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
			crewCapacityTextWidget.revalidate();
		}

		var crewContentWidget = client.getWidget(InterfaceID.SailingSidepanel.CREW_CONTENT);
		if (crewContentWidget != null) {
			crewContentWidget.revalidate();

			for (var child : crewContentWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}

		var crewScrollableWidget = client.getWidget(InterfaceID.SailingSidepanel.CREW_SCROLLABLE);
		if (crewScrollableWidget != null) {
			crewScrollableWidget.revalidate();
			for (var child : crewScrollableWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}
		
		var crewScrollbarWidget = client.getWidget(InterfaceID.SailingSidepanel.CREW_SCROLLBAR);
		if (crewScrollbarWidget != null) {
			crewScrollbarWidget.revalidate();

			for (var child : crewScrollbarWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}

		if (crewScrollableWidget != null && crewScrollbarWidget != null) {
			updateScrollbar(crewScrollbarWidget.getId(), crewScrollableWidget.getId());
		}

		var crewRowsWidget = client.getWidget(InterfaceID.SailingSidepanel.CREW_ROWS);
		if (crewRowsWidget != null) {
			crewRowsWidget.revalidate();
			for (var child : crewRowsWidget.getDynamicChildren()) {
				if (child != null) {
					child.revalidate();
				}
			}
		}
	}

	private void updateScrollbar(int scrollbarWidgetId, int scrollableWidgetId)
	{
		this.clientThread.invokeLater(() -> {
			client.runScript(ScriptID.UPDATE_SCROLLBAR, scrollbarWidgetId, scrollableWidgetId, 0);
		});
	}
}


/*
 * Copyright (c) 2025, 2026, marknewan <https://github.com/marknewan>
 * Copyright (c) 2018, 2023, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.duckblade.osrs.sailing.features.sidepanel;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SidePanelReorder implements PluginLifecycleComponent
{
	private static final String MENU_ACTION_UP = "Move up";
	private static final String MENU_ACTION_DOWN = "Move down";
	private static final String MENU_OPTION_LOCK = "Disable";
	private static final String MENU_OPTION_UNLOCK = "Enable";
	private static final String MENU_OPTION_RESET = "Reset";
	private static final String MENU_TARGET = "Sailing Panel Reorder";

	private static final WidgetMenuOption TAB_LOCK_FIXED = new WidgetMenuOption(MENU_OPTION_LOCK, MENU_TARGET,
		InterfaceID.Toplevel.STONE0);
	private static final WidgetMenuOption TAB_UNLOCK_FIXED = new WidgetMenuOption(MENU_OPTION_UNLOCK, MENU_TARGET,
		InterfaceID.Toplevel.STONE0);

	private static final WidgetMenuOption TAB_LOCK_CLASSIC = new WidgetMenuOption(MENU_OPTION_LOCK, MENU_TARGET,
		InterfaceID.ToplevelOsrsStretch.STONE0);
	private static final WidgetMenuOption TAB_UNLOCK_CLASSIC = new WidgetMenuOption(MENU_OPTION_UNLOCK, MENU_TARGET,
		InterfaceID.ToplevelOsrsStretch.STONE0);

	private static final WidgetMenuOption TAB_LOCK_MODERN = new WidgetMenuOption(MENU_OPTION_LOCK, MENU_TARGET,
		InterfaceID.ToplevelPreEoc.STONE0);
	private static final WidgetMenuOption TAB_UNLOCK_MODERN = new WidgetMenuOption(MENU_OPTION_UNLOCK, MENU_TARGET,
		InterfaceID.ToplevelPreEoc.STONE0);

	private static final WidgetMenuOption RESET_FIXED = new WidgetMenuOption(MENU_OPTION_RESET, MENU_TARGET,
		InterfaceID.Toplevel.STONE0);
	private static final WidgetMenuOption RESET_CLASSIC = new WidgetMenuOption(MENU_OPTION_RESET, MENU_TARGET,
		InterfaceID.ToplevelOsrsStretch.STONE0);
	private static final WidgetMenuOption RESET_MODERN = new WidgetMenuOption(MENU_OPTION_RESET, MENU_TARGET,
		InterfaceID.ToplevelPreEoc.STONE0);

	private static final int ROW_HEIGHT = 34;
	private static final int ROW_BASE_Y_EXPANDED = 2;
	private static final int ROW_BASE_Y_DEFAULT = 70;

	private final Client client;
	private final ClientThread clientThread;
	private final SailingConfig config;
	private final ConfigManager configManager;
	private final MenuManager menuManager;
	private final ChatMessageManager chatMessageManager;

	@Nullable
	private int[] customRowOrder;

	private int rowBaseY = ROW_BASE_Y_DEFAULT;

	private boolean reordering;

	@Override
	public void startUp()
	{
		updateConfig();
		refreshTabMenus();

		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				if (isOnPlayerBoat())
				{
					loadCustomRowOrder();
					redrawSidePanel();
				}
			}
		});
	}

	@Override
	public void shutDown()
	{
		customRowOrder = null;
		rowBaseY = ROW_BASE_Y_DEFAULT;
		reordering = false;
		removeTabMenus();
		clientThread.invokeLater(this::redrawSidePanel);
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged e)
	{
		if (e.getGroup().equals(SailingConfig.CONFIG_GROUP))
		{
			if (e.getKey().equals(SailingConfig.CONFIG_KEY_DEFAULT_STEERING_ASSIGN_BUTTON))
			{
				updateConfig();
				clientThread.invokeLater(this::redrawSidePanel);
			}
		}
	}

	@Subscribe
	public void onScriptPostFired(final ScriptPostFired e)
	{
		// https://github.com/runelite/cs2-scripts/blob/master/scripts/%5Bproc%2Cscript8729%5D.cs2
		if (e.getScriptId() == 8729)
		{
			reorderSidePanel();
		}
	}

	@Subscribe
	public void onVarbitChanged(final VarbitChanged e)
	{
		switch (e.getVarbitId())
		{
			case VarbitID.SAILING_PLAYER_IS_ON_PLAYER_BOAT:
				if (reordering && e.getValue() == 0)
				{
					disableReordering();
				}
				break;
			case VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE:
				if (reordering && e.getValue() > 0)
				{
					disableReordering();
				}
				break;
			case VarbitID.SAILING_BOAT_SPAWNED:
				loadCustomRowOrder();
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onMenuOptionClicked(final MenuOptionClicked e)
	{
		if (reordering &&
			customRowOrder != null &&
			e.getMenuAction() == MenuAction.CC_OP &&
			e.getId() == 1 &&
			(e.getMenuOption().equals(MENU_ACTION_UP) || e.getMenuOption().equals(MENU_ACTION_DOWN)))
		{
			e.consume();

			final var w = e.getWidget();
			if (w == null)
			{
				return;
			}

			final var idx = Integer.parseInt(w.getText());
			final var len = customRowOrder.length;
			final int swap;

			switch (e.getMenuOption())
			{
				case MENU_ACTION_UP:
					swap = (idx - 1 + len) % len;
					break;
				case MENU_ACTION_DOWN:
					swap = (idx + 1) % len;
					break;
				default:
					return;
			}

			final var tmp = customRowOrder[idx];
			customRowOrder[idx] = customRowOrder[swap];
			customRowOrder[swap] = tmp;

			saveCustomRowOrder();

			clientThread.invokeLater(this::redrawSidePanel);
		}
	}

	private void setReordering(final boolean reorder)
	{
		if (reorder)
		{
			if (!isSailingSidePanelOpen())
			{
				sendChatMessage("You can't reorder facilities while the sailing panel is closed.");
				return;
			}

			if (!isOnPlayerBoat())
			{
				sendChatMessage("You can't reorder facilities while not on a boat.");
				return;
			}

			if (isInShipyard())
			{
				sendChatMessage("You can't reorder facilities while in the shipyard.");
				return;
			}
		}

		sendChatMessage(String.format("Sailing panel reordering is now %s.", reorder ? "enabled" : "disabled"));

		reordering = reorder;
		refreshTabMenus();
		redrawSidePanel();
	}

	private void redrawSidePanel()
	{
		assert client.isClientThread();

		final var w = client.getWidget(InterfaceID.SailingSidepanel.UNIVERSE);
		if (w != null)
		{
			client.runScript(w.getOnVarTransmitListener());
		}
	}

	private void reorderSidePanel()
	{
		assert client.isClientThread();

		if (!isSailingSidePanelOpen() || !isFacilityTabOpen() || !isOnPlayerBoat() || isInShipyard() || isMarkerSet())
		{
			return;
		}

		final var rows = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
		if (rows == null || rows.getChildren() == null)
		{
			return;
		}

		final var clickLayer = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER);
		if (clickLayer == null || clickLayer.getChildren() == null)
		{
			return;
		}

		if (rowBaseY == ROW_BASE_Y_EXPANDED)
		{
			alignSteerAssignButton(clickLayer.getChildren(), rows.getChildren());
		}

		// first-run or facilities changed
		final var rowCount = (rows.getOriginalHeight() - rowBaseY) / ROW_HEIGHT;
		if (customRowOrder == null || customRowOrder.length != rowCount)
		{
			customRowOrder = IntStream.range(0, rowCount).toArray();
		}

		reorderRows(rows.getChildren());
		reorderRows(clickLayer.getChildren());
		setMarker();

		if (reordering)
		{
			createReorderWidgets(clickLayer, rows);
		}
	}

	private static void alignSteerAssignButton(final Widget[] clickLayer, final Widget[] rows)
	{
		// these child widget indices are static to every boat/hotspot setup
		clickLayer[7].setOriginalY(clickLayer[0].getOriginalY());

		for (int i = 35, j = 0; i <= 45; i++, j++)
		{
			rows[i].setOriginalY(rows[j].getOriginalY());
		}

		// re-align the graphic
		rows[44].setOriginalY(rows[44].getOriginalY() + 3);
	}

	private void reorderRows(final Widget[] children)
	{
		if (customRowOrder == null)
		{
			return;
		}

		// group widgets by origianlY value
		final var map = new HashMap<Integer, List<Widget>>();
		for (final var w : children)
		{
			if (w == null || w.getOriginalY() < rowBaseY)
			{
				continue;
			}

			final var idx = (w.getOriginalY() - rowBaseY) / ROW_HEIGHT;
			map.computeIfAbsent(idx, k -> new ArrayList<>()).add(w);
		}

		// set widget originalY values according to custom rowOrder
		for (var newIdx = 0; newIdx < customRowOrder.length; newIdx++)
		{
			final var oldIdx = customRowOrder[newIdx];

			final var widgets = map.get(oldIdx);
			if (widgets == null)
			{
				continue;
			}

			for (final var w : widgets)
			{
				final var offset = w.getOriginalY() - (rowBaseY + (oldIdx * ROW_HEIGHT));
				w.setOriginalY(rowBaseY + (newIdx * ROW_HEIGHT) + offset);
				w.revalidate();
			}
		}
	}

	private void createReorderWidgets(final Widget clickLayer, final Widget rows)
	{
		if (customRowOrder == null)
		{
			return;
		}

		clickLayer.deleteAllChildren();

		if (rows.getChildren() != null)
		{
			Arrays.stream(rows.getChildren())
				.filter(w -> w != null && w.isFilled())
				.forEach(w -> w.setHidden(true));
		}

		for (var visualIdx = 0; visualIdx < customRowOrder.length; visualIdx++)
		{
			final var logicalIdx = customRowOrder[visualIdx];
			createRowWidgets(clickLayer, rows, visualIdx, logicalIdx);
		}
	}

	private void createRowWidgets(final Widget clickLayer, final Widget rows, final int visualIdx, final int logicalIdx)
	{
		final var originalY = rowBaseY + (visualIdx * ROW_HEIGHT);
		final var buttonDim = 25;
		final var downButtonX = 44;
		final var upButtonX = 82;
		final var buttonY = originalY + (ROW_HEIGHT - buttonDim) / 2;

		final var downButtonClickMask = clickLayer.createChild(WidgetType.RECTANGLE);
		downButtonClickMask.setOriginalX(downButtonX);
		downButtonClickMask.setOriginalY(buttonY);
		downButtonClickMask.setOriginalWidth(buttonDim);
		downButtonClickMask.setOriginalHeight(buttonDim);
		downButtonClickMask.setText(Integer.toString(visualIdx));
		downButtonClickMask.setOpacity(255);
		downButtonClickMask.setAction(0, MENU_ACTION_DOWN);
		downButtonClickMask.revalidate();

		final var upButtonClickMask = clickLayer.createChild(WidgetType.RECTANGLE);
		upButtonClickMask.setOriginalX(upButtonX);
		upButtonClickMask.setOriginalY(buttonY);
		upButtonClickMask.setOriginalWidth(buttonDim);
		upButtonClickMask.setOriginalHeight(buttonDim);
		upButtonClickMask.setText(Integer.toString(visualIdx));
		upButtonClickMask.setOpacity(255);
		upButtonClickMask.setAction(0, MENU_ACTION_UP);
		upButtonClickMask.revalidate();

		final var background = rows.createChild(WidgetType.RECTANGLE);
		background.setOriginalY(originalY);
		background.setOriginalHeight(ROW_HEIGHT);
		background.setWidthMode(WidgetSizeMode.MINUS);
		background.setTextColor(Color.HSBtoRGB(logicalIdx / 10f, 0.8f, 0.8f) & 0xFFFFFF);
		background.setOpacity(160);
		background.setFilled(true);
		background.revalidate();

		final var seperator = rows.createChild(WidgetType.LINE);
		seperator.setOriginalY(originalY);
		seperator.setWidthMode(WidgetSizeMode.MINUS);
		seperator.revalidate();

		final var downButton = rows.createChild(WidgetType.GRAPHIC);
		downButton.setOriginalX(downButtonX);
		downButton.setOriginalY(buttonY);
		downButton.setOriginalWidth(buttonDim);
		downButton.setOriginalHeight(buttonDim);
		downButton.setSpriteId(SpriteID.Arrows25._2);
		downButton.setBorderType(1);
		downButton.setOnMouseOverListener((JavaScriptCallback) e -> downButton.setBorderType(2));
		downButton.setOnMouseLeaveListener((JavaScriptCallback) e -> downButton.setBorderType(1));
		downButton.setHasListener(true);
		downButton.revalidate();

		final var upButton = rows.createChild(WidgetType.GRAPHIC);
		upButton.setOriginalX(upButtonX);
		upButton.setOriginalY(buttonY);
		upButton.setOriginalWidth(buttonDim);
		upButton.setOriginalHeight(buttonDim);
		upButton.setSpriteId(SpriteID.Arrows25._0);
		upButton.setBorderType(1);
		upButton.setOnMouseOverListener((JavaScriptCallback) e -> upButton.setBorderType(2));
		upButton.setOnMouseLeaveListener((JavaScriptCallback) e -> upButton.setBorderType(1));
		upButton.setHasListener(true);
		upButton.revalidate();

		final var rowNumber = rows.createChild(WidgetType.TEXT);
		rowNumber.setOriginalX(134);
		rowNumber.setOriginalY(originalY + 6);
		rowNumber.setOriginalWidth(20);
		rowNumber.setOriginalHeight(20);
		rowNumber.setXTextAlignment(WidgetTextAlignment.CENTER);
		rowNumber.setYTextAlignment(WidgetTextAlignment.CENTER);
		rowNumber.setFontId(FontID.VERDANA_15);
		rowNumber.setText(Integer.toString(logicalIdx + 1));
		rowNumber.setTextColor(0xffffff);
		rowNumber.setTextShadowed(true);
		rowNumber.revalidate();
	}

	private boolean isSailingSidePanelOpen()
	{
		final var w = client.getWidget(InterfaceID.SailingSidepanel.UNIVERSE);
		return w != null && !w.isHidden();
	}

	private boolean isFacilityTabOpen()
	{
		return client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_TABS) == 0 &&
			client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_CREW_ASSIGNATION) == 0;
	}

	private boolean isInShipyard()
	{
		return client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_SHIPYARD_MODE) == 1;
	}

	private boolean isOnPlayerBoat()
	{
		return client.getVarbitValue(VarbitID.SAILING_PLAYER_IS_ON_PLAYER_BOAT) == 1;
	}

	private int getBoatSlot()
	{
		return client.getVarbitValue(VarbitID.SAILING_BOAT_SPAWNED);
	}

	private boolean isMarkerSet()
	{
		final var w = getMarkerWidget();
		return w != null && !w.getText().isEmpty();
	}

	private void setMarker()
	{
		final var w = getMarkerWidget();
		if (w != null)
		{
			// used to prevent reordering already repositioned widgets
			w.setText("reordered");
		}
	}

	private @Nullable Widget getMarkerWidget()
	{
		final var w = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER);
		return w != null ? w.getChild(0) : null;
	}

	private void refreshTabMenus()
	{
		removeTabMenus();

		if (reordering)
		{
			menuManager.addManagedCustomMenu(TAB_LOCK_FIXED, e -> setReordering(false));
			menuManager.addManagedCustomMenu(TAB_LOCK_CLASSIC, e -> setReordering(false));
			menuManager.addManagedCustomMenu(TAB_LOCK_MODERN, e -> setReordering(false));
		}
		else
		{
			menuManager.addManagedCustomMenu(TAB_UNLOCK_FIXED, e -> setReordering(true));
			menuManager.addManagedCustomMenu(TAB_UNLOCK_CLASSIC, e -> setReordering(true));
			menuManager.addManagedCustomMenu(TAB_UNLOCK_MODERN, e -> setReordering(true));
		}

		menuManager.addManagedCustomMenu(RESET_FIXED, e -> reset());
		menuManager.addManagedCustomMenu(RESET_CLASSIC, e -> reset());
		menuManager.addManagedCustomMenu(RESET_MODERN, e -> reset());
	}

	private void removeTabMenus()
	{
		menuManager.removeManagedCustomMenu(TAB_LOCK_FIXED);
		menuManager.removeManagedCustomMenu(TAB_UNLOCK_FIXED);
		menuManager.removeManagedCustomMenu(TAB_LOCK_CLASSIC);
		menuManager.removeManagedCustomMenu(TAB_UNLOCK_CLASSIC);
		menuManager.removeManagedCustomMenu(TAB_LOCK_MODERN);
		menuManager.removeManagedCustomMenu(TAB_UNLOCK_MODERN);
		menuManager.removeManagedCustomMenu(RESET_FIXED);
		menuManager.removeManagedCustomMenu(RESET_CLASSIC);
		menuManager.removeManagedCustomMenu(RESET_MODERN);
	}

	private void sendChatMessage(final String msg)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(msg)
			.build());
	}

	private void disableReordering()
	{
		reordering = false;
		refreshTabMenus();
	}

	private void reset()
	{
		customRowOrder = null;
		redrawSidePanel();
	}

	private void updateConfig()
	{
		rowBaseY = config.defaultSteeringAssignButton() ? ROW_BASE_Y_DEFAULT : ROW_BASE_Y_EXPANDED;
	}

	private void saveCustomRowOrder()
	{
		if (customRowOrder == null)
		{
			return;
		}

		final var boatSlot = getBoatSlot();
		if (boatSlot <= 0)
		{
			return;
		}

		final var key = configKey(boatSlot);
		final var value = Arrays.stream(customRowOrder)
			.mapToObj(Integer::toString)
			.collect(Collectors.joining(","));
		configManager.setConfiguration(SailingConfig.CONFIG_GROUP, key, value);
	}

	private void loadCustomRowOrder()
	{
		final var boatSlot = getBoatSlot();
		if (boatSlot <= 0)
		{
			return;
		}

		final var key = configKey(boatSlot);
		final var value = configManager.getConfiguration(SailingConfig.CONFIG_GROUP, key);
		if (value == null)
		{
			return;
		}

		customRowOrder = Arrays.stream(value.split(","))
			.mapToInt(Integer::parseInt)
			.toArray();
	}

	private static String configKey(final int boatSlot)
	{
		return SailingConfig.CONFIG_KEY_PREFIX_SIDE_PANEL_REORDER + boatSlot;
	}
}

package com.duckblade.osrs.sailing.features.barracudatrials;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Slf4j
@Singleton
public class JubblyJiveHelper
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final int SPRITE_ID_JUBBLY = 6998;
	private static final Set<Integer> SPRITE_IDS_OUTCROP_FULL = ImmutableSet.of(
		SPRITE_ID_JUBBLY, // jubbly
		6999 // toady
	);

	private static final WorldArea JUBBLY_JIVE_AREA = new WorldArea(2210, 2880, 2488 - 2210, 3072 - 2880, 0);

	// game object to highlight -> dynamic child id to check for state
	private static final Map<Integer, Integer> OUTCROP_WIDGET_CHILDREN_IDS = ImmutableMap.<Integer, Integer>builder()
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_1_PARENT, 1)
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_2_PARENT, 2)
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_3_PARENT, 3)
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_4_PARENT, 4)
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_5_PARENT, 5)
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_6_PARENT, 6)
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_7_PARENT, 7)
		.build();

	private static final Map<Integer, Color> OUTCROP_HIGHLIGHT_COLOURS = ImmutableMap.<Integer, Color>builder()
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_1_PARENT, new Color(0xC8C628))
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_2_PARENT, new Color(0x752622))
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_3_PARENT, new Color(0x213C64))
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_4_PARENT, new Color(0xA77422))
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_5_PARENT, new Color(0x589889))
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_6_PARENT, new Color(0x8F5594))
		.put(ObjectID.SAILING_BT_JUBBLY_JIVE_PILLAR_CLICKBOX_7_PARENT, new Color(0xA69FA9))
		.build();

	private final Client client;

	// realized version of OUTCROP_WIDGET_CHILDREN_IDS
	private final Map<GameObject, Integer> outcrops = new HashMap<>();

	private boolean active;

	@Inject
	public JubblyJiveHelper(Client client)
	{
		this.client = client;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.barracudaJubblyJiveShowToadyTargets();
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		boolean nowActive = client.getVarbitValue(VarbitID.SAILING_BT_IN_TRIAL) != 0 &&
			SailingUtil.isSailing(client) &&
			JUBBLY_JIVE_AREA.contains(SailingUtil.getTopLevelWorldPoint(client));

		if (active != nowActive)
		{
			log.debug("doing jubbly jive = {}", nowActive);
			active = nowActive;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();
		Integer childIndex = OUTCROP_WIDGET_CHILDREN_IDS.get(o.getId());
		if (childIndex != null)
		{
			outcrops.put(o, childIndex);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		outcrops.remove(e.getGameObject());
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		if (e.getWorldView().isTopLevel())
		{
			outcrops.clear();
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!active)
		{
			return null;
		}

		Widget widget = client.getWidget(InterfaceID.SailingBtHud.BT_MIDDLE_CONTENT);
		if (widget == null)
		{
			return null;
		}

		for (Map.Entry<GameObject, Integer> o : outcrops.entrySet())
		{
			GameObject obj = o.getKey();
			int childIx = o.getValue();

			// if we're on the final jubbly, only render the outcrops in front of the jubbly
			if (getJubbliesRemaining() == 1 && childIx <= getJubblyLocation())
			{
				continue;
			}

			// annoyingly these are dynamic children
			Widget stateWidget = widget.getChild(childIx);
			if (stateWidget == null || SPRITE_IDS_OUTCROP_FULL.contains(stateWidget.getSpriteId()))
			{
				continue;
			}

			Shape convexHull = obj.getConvexHull();
			if (convexHull != null)
			{
				graphics.setStroke(new BasicStroke(2));
				graphics.setColor(new Color(0, 0, 0, 50));
				graphics.fill(convexHull);
				graphics.setColor(OUTCROP_HIGHLIGHT_COLOURS.get(obj.getId()));
				graphics.draw(convexHull);
			}
		}

		return null;
	}

	private int getJubbliesRemaining()
	{
		Widget widget = client.getWidget(InterfaceID.SailingBtHud.BT_TRACKER_PROGRESS);
		String text;
		if (widget == null || (text = widget.getText()) == null)
		{
			return 0;
		}

		if (text.length() > 5)
		{
			// compact is "X / Y" but full is "X / Y Jubblies lured"
			text = text.substring(0, 5);
		}

		String[] split = text.split(" / ");
		return Integer.parseInt(split[1]) - Integer.parseInt(split[0]);
	}

	private int getJubblyLocation()
	{
		Widget widget = client.getWidget(InterfaceID.SailingBtHud.BT_MIDDLE_CONTENT);
		if (widget == null)
		{
			return -1;
		}

		for (int i = 0; i < 8; i++)
		{
			// first of the children to have the jubbly sprite
			Widget child = widget.getChild(i);
			if (child != null && child.getSpriteId() == SPRITE_ID_JUBBLY)
			{
				return i;
			}
		}

		return -1;
	}
}

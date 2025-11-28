package com.duckblade.osrs.sailing.features.barracudatrials;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
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
public class TemporTantrumHelper
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final int SPRITE_ID_RUM = 7022;

	private static final Color COLOUR_RUM = new Color(0xB24727);

	private final Client client;

	private boolean active;

	private GameObject pickUp;
	private GameObject dropOff;

	@Inject
	public TemporTantrumHelper(Client client)
	{
		this.client = client;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.barracudaTemporTantrumShowRumTarget();
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		boolean nowActive = client.getVarbitValue(VarbitID.SAILING_BT_IN_TRIAL) != 0 &&
			SailingUtil.isSailing(client) &&
			BarracudaTrial.TEMPOR_TANTRUM.getArea().contains(SailingUtil.getTopLevelWorldPoint(client));

		if (active != nowActive)
		{
			log.debug("doing tempor tantrum = {}", nowActive);
			active = nowActive;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject o = e.getGameObject();
		if (o.getId() == ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_PARENT)
		{
			pickUp = o;
		}
		if (o.getId() == ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_PARENT)
		{
			dropOff = o;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		if (e.getGameObject() == pickUp)
		{
			pickUp = null;
		}
		if (e.getGameObject() == dropOff)
		{
			dropOff = null;
		}
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded e)
	{
		if (pickUp != null && pickUp.getWorldView() == e.getWorldView())
		{
			pickUp = null;
		}
		if (dropOff != null && dropOff.getWorldView() == e.getWorldView())
		{
			dropOff = null;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!active)
		{
			return null;
		}

		GameObject target = hasRum() ? dropOff : pickUp;
		if (target == null)
		{
			return null;
		}

		Shape hull = target.getConvexHull();
		if (hull == null)
		{
			return null;
		}

		graphics.setColor(new Color(0, 0, 0, 50));
		graphics.setStroke(new BasicStroke(2));
		graphics.fill(hull);
		graphics.setColor(COLOUR_RUM);
		graphics.draw(hull);

		return null;
	}

	private boolean hasRum()
	{
		Widget widget = client.getWidget(InterfaceID.SailingBtHud.BT_PARTIAL_GFX);
		return widget != null && widget.getSpriteId() == SPRITE_ID_RUM;
	}
}

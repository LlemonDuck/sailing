package com.duckblade.osrs.sailing.features.navigation;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
@Singleton
public class NavigationOverlay
	extends Overlay
	implements PluginLifecycleComponent
{

	private final BoatTracker boatTracker;
	private final Client client;

	private SailingConfig.NavigationOverlayMode mode;
	private Color colour;
	private boolean speedEnabled;
	private boolean headingEnabled;

	private int sceneBaseX = -1;
	private int sceneBaseY = -1;
	private LocalPoint lastPoint;
	private int speed;

	@Inject
	public NavigationOverlay(BoatTracker boatTracker, Client client)
	{
		this.boatTracker = boatTracker;
		this.client = client;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		mode = config.navigationOverlayMode();
		colour = config.navigationOverlayColour();
		speedEnabled = config.navigationOverlaySpeed();
		headingEnabled = config.navigationOverlayHeading();

		// always on for speed tracking, mode checked in config
		return true;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (!SailingUtil.isSailing(client))
		{
			lastPoint = null;
			return;
		}

		checkSceneBase();

		Boat boat = boatTracker.getBoat();
		WorldEntity we = boat.getWorldEntity();
		LocalPoint lp = we.getTargetLocation();

		if (!lp.equals(lastPoint))
		{
			if (lastPoint != null)
			{
				double trueSpeed = (float) Math.hypot(
					(lastPoint.getX() - lp.getX()),
					(lastPoint.getY() - lp.getY())
				);
				speed = roundToQuarterTile(trueSpeed) / 32;
			}
			lastPoint = lp;
		}
		else
		{
			speed = 0;
		}
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!SailingUtil.isSailing(client) ||
			mode == SailingConfig.NavigationOverlayMode.OFF ||
			(!speedEnabled && !headingEnabled))
		{
			return null;
		}

		if (mode == SailingConfig.NavigationOverlayMode.NAVIGATING &&
			client.getTopLevelWorldView().getYellowClickAction() != Constants.CLICK_ACTION_SET_HEADING)
		{
			return null;
		}

		String text = "";
		Boat boat = boatTracker.getBoat();
		if (headingEnabled)
		{
			text += "Heading: " + jauToDirectionString(boat.getWorldEntity().getTargetOrientation());
		}
		if (headingEnabled && speedEnabled)
		{
			text += "\n";
		}
		if (speedEnabled)
		{
			text += "Speed: " + speed;
		}

		Point textTarget = getRenderPoint();
		g.setFont(FontManager.getRunescapeBoldFont().deriveFont(24f));
		renderCenteredMultilineText(g, text, textTarget, colour);

		return null;
	}

	private void checkSceneBase()
	{
		WorldView tlwv = client.getTopLevelWorldView();
		int baseX = tlwv.getScene().getBaseX();
		int baseY = tlwv.getScene().getBaseY();

		if (baseX == sceneBaseX && baseY == sceneBaseY)
		{
			return;
		}

		if (lastPoint != null)
		{
			int xAdjust = baseX - sceneBaseX;
			int yAdjust = baseY - sceneBaseY;
			// reshift lastPoint to where it would be in the new scene
			lastPoint = new LocalPoint(
				lastPoint.getX() - (xAdjust * Perspective.LOCAL_TILE_SIZE),
				lastPoint.getY() - (yAdjust * Perspective.LOCAL_TILE_SIZE),
				tlwv
			);
		}

		sceneBaseX = client.getTopLevelWorldView().getScene().getBaseX();
		sceneBaseY = client.getTopLevelWorldView().getScene().getBaseY();
	}

	private static int roundToQuarterTile(double trueSpeed)
	{
		int quarterTileFloor = ((int) trueSpeed) & ~0x1F;
		int quarterTileCeil = quarterTileFloor + 0x20;
		log.trace("{} = {} {}", trueSpeed, quarterTileFloor, quarterTileCeil);

		if (quarterTileCeil - trueSpeed < trueSpeed - quarterTileFloor)
		{
			return quarterTileCeil;
		}

		return quarterTileFloor;
	}

	private Point getRenderPoint()
	{
		Boat boat = boatTracker.getBoat();
		WorldEntity we = boat.getWorldEntity();
		GameObject sail = boat.getSail();

		int height = 0;
		LocalPoint lp = boat.getWorldEntity().getLocalLocation();
		if (sail != null)
		{
			switch (boat.getSizeClass())
			{
				case RAFT:
					height = 250;
					lp = sail.getLocalLocation()
						.dx(Perspective.LOCAL_TILE_SIZE / 2)
						.dy(0);
					break;

				case SKIFF:
					height = 450;
					lp = sail.getLocalLocation()
						.dx(-Perspective.LOCAL_TILE_SIZE / 2)
						.dy(-5 * Perspective.LOCAL_TILE_SIZE / 2);
					break;

				case SLOOP:
					height = 550;
					lp = sail.getLocalLocation()
						.dx(-Perspective.LOCAL_TILE_SIZE)
						.dy(-11 * Perspective.LOCAL_TILE_SIZE / 2);
					break;
			}
		}

		return Perspective.localToCanvas(
			client,
			lp,
			we.getWorldView().getPlane(),
			height
		);
	}

	private static void renderCenteredMultilineText(Graphics2D g, String text, Point origin, Color c)
	{
		String[] lines = text.split("\n");

		int lineHeight = g.getFontMetrics().getHeight();
		int stackedHeight = lines.length * lineHeight;

		int y = origin.getY() - stackedHeight / 2;
		for (String line : lines)
		{
			int lineX = origin.getX() - g.getFontMetrics().stringWidth(line) / 2;
			OverlayUtil.renderTextLocation(g, new Point(lineX, y), line, c);

			y += lineHeight;
		}
	}

	private static String jauToDirectionString(int jau)
	{
		int flattened = jau / 128;
		switch (flattened)
		{
			case 0:
				return "S"; // why tho

			case 1:
				return "SSW";

			case 2:
				return "SW";

			case 3:
				return "WSW";

			case 4:
				return "W";

			case 5:
				return "WNW";

			case 6:
				return "NW";

			case 7:
				return "NNW";

			case 8:
				return "N";

			case 9:
				return "NNE";

			case 10:
				return "NE";

			case 11:
				return "ENE";

			case 12:
				return "E";

			case 13:
				return "ESE";

			case 14:
				return "SE";

			case 15:
				return "SSE";

			default:
				return "??? - " + jau + " / " + flattened;
		}
	}
}

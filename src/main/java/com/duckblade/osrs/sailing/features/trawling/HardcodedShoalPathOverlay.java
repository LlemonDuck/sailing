package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Slf4j
@Singleton
public class HardcodedShoalPathOverlay extends Overlay implements PluginLifecycleComponent {

	@Nonnull
	private final Client client;
	private final SailingConfig config;

	@Inject
	public HardcodedShoalPathOverlay(@Nonnull Client client, SailingConfig config) {
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public boolean isEnabled(SailingConfig config) {
		return config.trawlingShowHardcodedShoalPaths();
	}

	@Override
	public void startUp() {
		log.debug("HardcodedShoalPathOverlay started");
	}

	@Override
	public void shutDown() {
		log.debug("HardcodedShoalPathOverlay shut down");
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		if (playerLocation == null) {
			return null;
		}

		Color pathColor = config.trawlingHardcodedShoalPathColour();
		
		// Only render paths if player is in the same region as the path
		if (isInPathRegion(playerLocation, ShoalPaths.HALIBUT_PORT_ROBERTS)) {
			renderPath(graphics, ShoalPaths.HALIBUT_PORT_ROBERTS, pathColor, "Halibut - Port Roberts");
		}
		if (isInPathRegion(playerLocation, ShoalPaths.HALIBUT_SOUTHERN_EXPANSE)) {
			renderPath(graphics, ShoalPaths.HALIBUT_SOUTHERN_EXPANSE, pathColor, "Halibut - Southern Expanse");
		}
		
		return null;
	}

	/**
	 * Check if the player is in the same region as any point in the path.
	 * A region is 64x64 tiles, so we check if the player's region ID matches any region used by the path.
	 */
	private boolean isInPathRegion(WorldPoint playerLocation, WorldPoint[] path) {
		if (path == null || path.length == 0) {
			return false;
		}

		int playerRegionID = playerLocation.getRegionID();
		
		// Check if any point in the path is in the same region as the player
		for (WorldPoint point : path) {
			if (point.getRegionID() == playerRegionID) {
				return true;
			}
		}
		
		return false;
	}

	private void renderPath(Graphics2D graphics, WorldPoint[] path, Color pathColor, String label) {
		if (path == null || path.length < 2) {
			return;
		}

		graphics.setStroke(new BasicStroke(2));
		net.runelite.api.Point previousCanvasPoint = null;
		net.runelite.api.Point firstVisiblePoint = null;

		for (WorldPoint worldPos : path) {
			// Convert WorldPoint to LocalPoint for rendering
			LocalPoint localPos = LocalPoint.fromWorld(client, worldPos);
			if (localPos == null) {
				previousCanvasPoint = null;
				continue;
			}

			net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, localPos, worldPos.getPlane());

			if (canvasPoint == null) {
				previousCanvasPoint = null;
				continue;
			}

			// Track first visible point for label
			if (firstVisiblePoint == null) {
				firstVisiblePoint = canvasPoint;
			}

			// Draw line from previous point
			if (previousCanvasPoint != null) {
				graphics.setColor(pathColor);
				graphics.drawLine(
					previousCanvasPoint.getX(),
					previousCanvasPoint.getY(),
					canvasPoint.getX(),
					canvasPoint.getY()
				);
			}

			// Draw small waypoint marker
			graphics.setColor(pathColor);
			graphics.fillOval(canvasPoint.getX() - 2, canvasPoint.getY() - 2, 4, 4);

			previousCanvasPoint = canvasPoint;
		}

		// Draw line back to start to complete the loop
		if (path.length >= 2) {
			WorldPoint firstWorldPos = path[0];
			WorldPoint lastWorldPos = path[path.length - 1];

			LocalPoint firstLocal = LocalPoint.fromWorld(client, firstWorldPos);
			LocalPoint lastLocal = LocalPoint.fromWorld(client, lastWorldPos);

			if (firstLocal != null && lastLocal != null) {
				net.runelite.api.Point firstCanvas = Perspective.localToCanvas(client, firstLocal, firstWorldPos.getPlane());
				net.runelite.api.Point lastCanvas = Perspective.localToCanvas(client, lastLocal, lastWorldPos.getPlane());

				if (firstCanvas != null && lastCanvas != null) {
					// Draw dashed line to indicate loop
					Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
						0, new float[]{9}, 0);
					graphics.setStroke(dashed);
					graphics.setColor(pathColor);
					graphics.drawLine(
						lastCanvas.getX(),
						lastCanvas.getY(),
						firstCanvas.getX(),
						firstCanvas.getY()
					);
				}
			}
		}

		// Draw label near first visible point
		if (firstVisiblePoint != null && label != null) {
			graphics.setColor(Color.WHITE);
			graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 14f));
			graphics.drawString(label, firstVisiblePoint.getX() + 10, firstVisiblePoint.getY() - 10);
		}
	}
}

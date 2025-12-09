package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
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
public class ShoalPathOverlay extends Overlay implements PluginLifecycleComponent {

	@Nonnull
	private final Client client;
	private final SailingConfig config;
	
	// ya the numbers are magic, figure it out
	private static final ShoalFishingArea PORT_ROBERTS = new ShoalFishingArea(1822, 2050, 3129, 3414);
	private static final ShoalFishingArea SOUTHERN_EXPANSE = new ShoalFishingArea(1870, 2180, 2171, 2512);
	private static final ShoalFishingArea RAINBOW_REEF = new ShoalFishingArea(2075, 2406, 2179, 2450);
	private static final ShoalFishingArea BUCCANEERS_HAVEN = new ShoalFishingArea(1984, 2268, 3594, 3771);
	private static final ShoalFishingArea WEISSMERE = new ShoalFishingArea(2590, 2870, 3945, 4146);

	// Stop points that mark fishing spots on a given route
	private static final int[] PORT_ROBERTS_STOP_INDICES = {0, 45, 79, 139, 168, 214, 258, 306, 337};
	private static final int[] SOUTHERN_EXPANSE_STOP_INDICES = {0, 15, 60, 97, 132, 185, 273, 343, 369, 419};
	private static final int[] RAINBOW_REEF_STOP_INDICES = {0, 20, 52, 73, 108, 155, 188, 221, 264, 313};
	private static final int[] BUCCANEERS_HAVEN_STOP_INDICES = {0, 22, 57, 92, 126, 165, 194, 229, 269, 304, 352, 386};
	private static final int[] WEISSMERE_STOP_INDICES = {0, 6, 42, 72, 89, 104, 138, 148};
	
	// Color for stop point overlays (red)
	private static final Color STOP_POINT_COLOR = Color.RED;

	@Inject
	public ShoalPathOverlay(@Nonnull Client client, SailingConfig config) {
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
		// Only render paths if player is sailing
		if (!SailingUtil.isSailing(client)) {
			return null;
		}
		
		// Get top-level world coordinates (actual world position, not boat instance position)
		WorldPoint playerLocation = SailingUtil.getTopLevelWorldPoint(client);

        Color pathColor = config.trawlingHardcodedShoalPathColour();

		if (PORT_ROBERTS.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.HALIBUT_PORT_ROBERTS, pathColor);
			renderStopPoints(graphics, ShoalPaths.HALIBUT_PORT_ROBERTS, PORT_ROBERTS_STOP_INDICES);
		}

		else if (SOUTHERN_EXPANSE.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.HALIBUT_SOUTHERN_EXPANSE, pathColor);
			renderStopPoints(graphics, ShoalPaths.HALIBUT_SOUTHERN_EXPANSE, SOUTHERN_EXPANSE_STOP_INDICES);
		}

		else if (RAINBOW_REEF.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.BLUEFIN_RAINBOW_REEF, pathColor);
			renderStopPoints(graphics, ShoalPaths.BLUEFIN_RAINBOW_REEF, RAINBOW_REEF_STOP_INDICES);
		}

		else if (BUCCANEERS_HAVEN.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.BLUEFIN_BUCCANEERS_HAVEN, pathColor);
			renderStopPoints(graphics, ShoalPaths.BLUEFIN_BUCCANEERS_HAVEN, BUCCANEERS_HAVEN_STOP_INDICES);
		}

		else if (WEISSMERE.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.MARLIN_WEISSMERE, pathColor);
			renderStopPoints(graphics, ShoalPaths.MARLIN_WEISSMERE, WEISSMERE_STOP_INDICES);
		}
		
		return null;
	}

	private void renderPath(Graphics2D graphics, WorldPoint[] path, Color pathColor) {
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

			previousCanvasPoint = canvasPoint;
		}

		// Draw line back to start to complete the loop
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

	private void renderStopPoints(Graphics2D graphics, WorldPoint[] path, int[] stopIndices) {
		if (path == null || stopIndices == null) {
			return;
		}

		for (int index : stopIndices) {
			if (index >= path.length) {
				continue;
			}

			WorldPoint stopPoint = path[index];
			renderStopPointArea(graphics, stopPoint);
		}
	}

	private void renderStopPointArea(Graphics2D graphics, WorldPoint centerPoint) {
		// Convert WorldPoint to LocalPoint
		LocalPoint localPoint = LocalPoint.fromWorld(client, centerPoint);
		if (localPoint == null) {
			return;
		}

		// Convert to canvas point
		net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, localPoint, centerPoint.getPlane());
		if (canvasPoint == null) {
			return;
		}

		// Draw stop point marker - red filled circle with white outline (matches trace rendering)
		graphics.setColor(STOP_POINT_COLOR);
		graphics.fillOval(canvasPoint.getX() - 5, canvasPoint.getY() - 5, 10, 10);
		graphics.setColor(Color.WHITE);
		graphics.drawOval(canvasPoint.getX() - 5, canvasPoint.getY() - 5, 10, 10);
	}
}

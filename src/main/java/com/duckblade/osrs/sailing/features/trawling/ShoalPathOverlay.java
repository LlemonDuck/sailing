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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;

@Slf4j
@Singleton
public class ShoalPathOverlay extends Overlay implements PluginLifecycleComponent {

	@Nonnull
	private final Client client;
	private final SailingConfig config;
	
	// ya the numbers are magic, figure it out


	// Stop points that mark fishing spots on a given route
	private static final int[] PORT_ROBERTS_STOP_INDICES = {0, 35, 55, 75, 98, 124, 144, 171, 188};
	private static final int[] SOUTHERN_EXPANSE_STOP_INDICES = {0, 23, 46, 80, 128, 145, 176, 201, 229, 241};
	private static final int[] DEEPFIN_POINT_STOP_INDICES = {0, 34, 52, 70, 79, 98, 122, 158};
	private static final int[] RAINBOW_REEF_STOP_INDICES = {0, 13, 45, 75, 93, 119, 136, 160, 169, 192};
	private static final int[] BUCCANEERS_HAVEN_STOP_INDICES = {0, 17, 27, 59, 79, 93, 111, 126, 145, 153, 173, 191};
	private static final int[] WEISSMERE_STOP_INDICES = {0, 1, 54, 61, 75, 84, 108, 123};
	private static final int[] BRITTLE_ISLE_STOP_INDICES = {0, 13, 29, 58, 80, 103, 134, 165, 200};
	private static final int[] SIMIAN_SEA_STOP_INDICES = {0, 12, 22, 26, 32, 37, 42};
	private static final int[] TURTLE_BELT_STOP_INDICES = {0, 11, 17, 23, 37, 44, 50, 73};
	private static final int[] GREAT_SOUND_STOP_INDICES = {0, 10, 19, 29, 43, 48, 53};
	private static final int[] SUNSET_BAY_STOP_INDICES = {0, 17, 29, 36, 46, 64, 73};
	private static final int[] MISTY_SEA_STOP_INDICES = {0, 14, 28, 34, 52, 76, 105, 118, 125, 134};
	private static final int[] ANGLERFISHS_LIGHT_STOP_INDICES = {0, 14, 33, 40, 52, 65, 74};
	private static final int[] THE_ONYX_CREST_STOP_INDICES = {0, 18, 37, 53, 68, 91, 112, 124, 137, 141};
	private static final int[] SEA_OF_SOULS_STOP_INDICES = {0, 15, 30, 35, 44, 73, 95, 113, 133, 138, 147, 177};
	private static final int[] THE_CROWN_JEWEL_STOP_INDICES = {0, 18, 37, 58, 90, 116, 125, 144, 171, 207, 220};

	// Color for stop point overlays (red)
	private static final Color STOP_POINT_COLOR = Color.RED;

	@Inject
	public ShoalPathOverlay(@Nonnull Client client, SailingConfig config) {
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		setPriority(PRIORITY_MED);
	}

	@Override
	public boolean isEnabled(SailingConfig config) {
		return config.trawlingShowShoalPaths();
	}

	@Override
	public void startUp() {
		log.debug("ShoalPathOverlay started");
	}

	@Override
	public void shutDown() {
		log.debug("ShoalPathOverlay shut down");
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		// Only render paths if player is sailing
		if (!SailingUtil.isSailing(client)) {
			return null;
		}
		
		// Get top-level world coordinates (actual world position, not boat instance position)
		WorldPoint playerLocation = SailingUtil.getTopLevelWorldPoint(client);
		// log.debug("ShoalPathOverlay rendering at player location: {}", playerLocation);

        Color pathColor = config.trawlingShoalPathColour();

		if (TrawlingData.FishingAreas.PORT_ROBERTS.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.HALIBUT_PORT_ROBERTS, pathColor);
			renderStopPoints(graphics, ShoalPaths.HALIBUT_PORT_ROBERTS, PORT_ROBERTS_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.SOUTHERN_EXPANSE.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.HALIBUT_SOUTHERN_EXPANSE, pathColor);
			renderStopPoints(graphics, ShoalPaths.HALIBUT_SOUTHERN_EXPANSE, SOUTHERN_EXPANSE_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.DEEPFIN_POINT.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.YELLOWFIN_DEEPFIN_POINT, pathColor);
			renderStopPoints(graphics, ShoalPaths.YELLOWFIN_DEEPFIN_POINT, DEEPFIN_POINT_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.RAINBOW_REEF.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.BLUEFIN_RAINBOW_REEF, pathColor);
			renderStopPoints(graphics, ShoalPaths.BLUEFIN_RAINBOW_REEF, RAINBOW_REEF_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.BUCCANEERS_HAVEN.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.BLUEFIN_BUCCANEERS_HAVEN, pathColor);
			renderStopPoints(graphics, ShoalPaths.BLUEFIN_BUCCANEERS_HAVEN, BUCCANEERS_HAVEN_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.WEISSMERE.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.MARLIN_WEISSMERE, pathColor);
			renderStopPoints(graphics, ShoalPaths.MARLIN_WEISSMERE, WEISSMERE_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.BRITTLE_ISLE.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.MARLIN_BRITTLE_ISLE, pathColor);
			renderStopPoints(graphics, ShoalPaths.MARLIN_BRITTLE_ISLE, BRITTLE_ISLE_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.SIMIAN_SEA.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.GIANT_KRILL_SIMIAN_SEA, pathColor);
			renderStopPoints(graphics, ShoalPaths.GIANT_KRILL_SIMIAN_SEA, SIMIAN_SEA_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.TURTLE_BELT.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.GIANT_KRILL_TURTLE_BELT, pathColor);
			renderStopPoints(graphics, ShoalPaths.GIANT_KRILL_TURTLE_BELT, TURTLE_BELT_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.GREAT_SOUND.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.GIANT_KRILL_GREAT_SOUND, pathColor);
			renderStopPoints(graphics, ShoalPaths.GIANT_KRILL_GREAT_SOUND, GREAT_SOUND_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.SUNSET_BAY.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.GIANT_KRILL_SUNSET_BAY, pathColor);
			renderStopPoints(graphics, ShoalPaths.GIANT_KRILL_SUNSET_BAY, SUNSET_BAY_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.MISTY_SEA.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.HADDOCK_MISTY_SEA, pathColor);
			renderStopPoints(graphics, ShoalPaths.HADDOCK_MISTY_SEA, MISTY_SEA_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.ANGLERFISHS_LIGHT.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.HADDOCK_ANGLERFISHS_LIGHT, pathColor);
			renderStopPoints(graphics, ShoalPaths.HADDOCK_ANGLERFISHS_LIGHT, ANGLERFISHS_LIGHT_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.THE_ONYX_CREST.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.HADDOCK_THE_ONYX_CREST, pathColor);
			renderStopPoints(graphics, ShoalPaths.HADDOCK_THE_ONYX_CREST, THE_ONYX_CREST_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.SEA_OF_SOULS.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.YELLOWFIN_SEA_OF_SOULS, pathColor);
			renderStopPoints(graphics, ShoalPaths.YELLOWFIN_SEA_OF_SOULS, SEA_OF_SOULS_STOP_INDICES);
		}

		else if (TrawlingData.FishingAreas.THE_CROWN_JEWEL.contains(playerLocation)) {
			renderPath(graphics, ShoalPaths.YELLOWFIN_THE_CROWN_JEWEL, pathColor);
			renderStopPoints(graphics, ShoalPaths.YELLOWFIN_THE_CROWN_JEWEL, THE_CROWN_JEWEL_STOP_INDICES);
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

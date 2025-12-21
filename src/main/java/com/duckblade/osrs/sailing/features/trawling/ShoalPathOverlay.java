package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.model.FishingNetTier;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.math.IntMath;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.List;

@Slf4j
@Singleton
public class ShoalPathOverlay extends Overlay implements PluginLifecycleComponent {

	private final Client client;
	private final SailingConfig config;
	private final BoatTracker boatTracker;

	public static final int MAX_SPLITTABLE_DISTANCE = 10;
	
	// Arrow spacing - draw an arrow every N points along the path
	private static final int ARROW_SPACING = 5;
	// Arrow size in pixels
	private static final int ARROW_SIZE = 12;
	// Arrow width (how wide the arrowhead is)
	private static final int ARROW_WIDTH = 8;

	// Color for stop point overlays (red)
	private static final Color STOP_POINT_COLOR = Color.RED;

	@Inject
	public ShoalPathOverlay(
		Client client, 
		SailingConfig config,
		BoatTracker boatTracker
	) 
	{
		this.client = client;
		this.config = config;
		this.boatTracker = boatTracker;
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
		if (!SailingUtil.isSailing(client)) {
			return null;
		}
		if (boatTracker.getBoat() == null || boatTracker.getBoat().getNetTiers().isEmpty()) {
			return null;
		}

		WorldPoint playerLocation = SailingUtil.getTopLevelWorldPoint(client);

		Color pathColor = config.trawlingShoalPathColour();

		for (final var area : ShoalFishingArea.AREAS) {
			if (!area.contains(playerLocation)) {
				continue;
			}

			renderPath(graphics, area.getPath(), pathColor);
			if (config.trawlingShowShoalDirectionArrows()) {
				renderDirectionalArrows(graphics, area.getPath(), pathColor);
			}
			renderStopPoints(graphics, area.getPath(), area.getStopIndices());
		}

		return null;
	}

	private void renderPath(Graphics2D graphics, WorldPoint[] path, Color pathColor) {
		if (path == null || path.length < 2) {
			return;
		}

		graphics.setStroke(new BasicStroke(2));

		WorldPoint previousWorldPoint = null;
		for (WorldPoint worldPoint : path) {
			if (previousWorldPoint == null) {
				previousWorldPoint = worldPoint;
				continue;
			}

			renderSegment(graphics, previousWorldPoint, worldPoint, pathColor);
			previousWorldPoint = worldPoint;
		}

		// Draw line back to start to complete the loop; we use dashed stroke to indicate that
		Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
			0, new float[] {9}, 0);
		graphics.setStroke(dashed);
		renderSegment(graphics, path[path.length - 1], path[0], pathColor);
    }

	private void renderSegment(Graphics2D graphics, WorldPoint worldPoint1, WorldPoint worldPoint2, Color pathColor) {
		LocalPoint localPoint1 = LocalPoint.fromWorld(client, worldPoint1);
		LocalPoint localPoint2 = LocalPoint.fromWorld(client, worldPoint2);
		if (localPoint1 == null || localPoint2 == null) {
			renderSplitSegments(graphics, worldPoint1, worldPoint2, pathColor);
			return;
		}

		Point canvasPoint1 = Perspective.localToCanvas(client, localPoint1, worldPoint1.getPlane());
		Point canvasPoint2 = Perspective.localToCanvas(client, localPoint2, worldPoint1.getPlane());
		if (canvasPoint1 == null || canvasPoint2 == null) {
			renderSplitSegments(graphics, worldPoint1, worldPoint2, pathColor);
			return;
		}

		graphics.setColor(pathColor);
		graphics.drawLine(
			canvasPoint1.getX(),
			canvasPoint1.getY(),
			canvasPoint2.getX(),
			canvasPoint2.getY()
		);
	}

	/**
	 * Splits the given segment in half and tries to draw both halves. Keeps trying recursively
	 * until success or segment becomes too short or no longer splittable.
	 */
	private void renderSplitSegments(Graphics2D graphics, WorldPoint worldPoint1, WorldPoint worldPoint2, Color pathColor) {
		int dx = worldPoint2.getX() - worldPoint1.getX();
		int dy = worldPoint2.getY() - worldPoint1.getY();

		if (Math.hypot(dx, dy) < MAX_SPLITTABLE_DISTANCE) {
			return;
		}

		int maxSteps = IntMath.gcd(Math.abs(dx), Math.abs(dy));
		if (maxSteps <= 2) {
			return;
		}

		int midStep = maxSteps / 2;
		int midX = worldPoint1.getX() + (dx / maxSteps * midStep);
		int midY = worldPoint1.getY() + (dy / maxSteps * midStep);
		WorldPoint midPoint = new WorldPoint(midX, midY, worldPoint1.getPlane());

		renderSegment(graphics, worldPoint1, midPoint, pathColor);
		renderSegment(graphics, midPoint, worldPoint2, pathColor);
	}

	private void renderDirectionalArrows(Graphics2D graphics, WorldPoint[] path, Color pathColor) {
		if (path == null || path.length < 2) {
			return;
		}

		graphics.setStroke(new BasicStroke(2));
		graphics.setColor(pathColor);

		// Draw arrows at regular intervals along the path
		for (int i = 0; i < path.length - 1; i += ARROW_SPACING) {
			WorldPoint currentPoint = path[i];
			WorldPoint nextPoint = path[i + 1];
			
			renderArrowAtSegment(graphics, currentPoint, nextPoint, pathColor);
		}
		
		// Also draw an arrow for the closing segment (back to start)
		if (path.length > 2) {
			renderArrowAtSegment(graphics, path[path.length - 1], path[0], pathColor);
		}
	}

	private void renderArrowAtSegment(Graphics2D graphics, WorldPoint fromPoint, WorldPoint toPoint, Color pathColor) {
		LocalPoint localFrom = LocalPoint.fromWorld(client, fromPoint);
		LocalPoint localTo = LocalPoint.fromWorld(client, toPoint);
		
		if (localFrom == null || localTo == null) {
			return;
		}

		Point canvasFrom = Perspective.localToCanvas(client, localFrom, fromPoint.getPlane());
		Point canvasTo = Perspective.localToCanvas(client, localTo, toPoint.getPlane());
		
		if (canvasFrom == null || canvasTo == null) {
			return;
		}

		// Calculate the midpoint of the segment for arrow placement
		int midX = (canvasFrom.getX() + canvasTo.getX()) / 2;
		int midY = (canvasFrom.getY() + canvasTo.getY()) / 2;

		// Calculate direction vector
		double dx = canvasTo.getX() - canvasFrom.getX();
		double dy = canvasTo.getY() - canvasFrom.getY();
		double length = Math.sqrt(dx * dx + dy * dy);
		
		if (length < 1) {
			return; // Skip if points are too close
		}

		// Normalize direction vector
		dx /= length;
		dy /= length;

		// Draw arrowhead
		drawArrowhead(graphics, midX, midY, dx, dy, pathColor);
	}

	private void drawArrowhead(Graphics2D graphics, int x, int y, double dirX, double dirY, Color color) {
		graphics.setColor(color);
		
		// Create a filled triangular arrowhead for better visibility
		double arrowAngle = Math.PI / 4; // 45 degrees for wider arrowhead
		double arrowLength = ARROW_SIZE;
		
		// Arrow tip point (ahead of the center point)
		int tipX = (int) (x + arrowLength * 0.3 * dirX);
		int tipY = (int) (y + arrowLength * 0.3 * dirY);
		
		// Left wing of arrow
		double leftX = x - arrowLength * (dirX * Math.cos(arrowAngle) - dirY * Math.sin(arrowAngle));
		double leftY = y - arrowLength * (dirY * Math.cos(arrowAngle) + dirX * Math.sin(arrowAngle));
		
		// Right wing of arrow
		double rightX = x - arrowLength * (dirX * Math.cos(-arrowAngle) - dirY * Math.sin(-arrowAngle));
		double rightY = y - arrowLength * (dirY * Math.cos(-arrowAngle) + dirX * Math.sin(-arrowAngle));
		
		// Create triangle points for filled arrowhead
		int[] xPoints = {tipX, (int)leftX, (int)rightX};
		int[] yPoints = {tipY, (int)leftY, (int)rightY};
		
		// Fill the arrowhead triangle
		graphics.fillPolygon(xPoints, yPoints, 3);
		
		// Add a darker outline for better visibility
		graphics.setColor(color.darker());
		graphics.setStroke(new BasicStroke(1));
		graphics.drawPolygon(xPoints, yPoints, 3);
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
		Point canvasPoint = Perspective.localToCanvas(client, localPoint, centerPoint.getPlane());
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

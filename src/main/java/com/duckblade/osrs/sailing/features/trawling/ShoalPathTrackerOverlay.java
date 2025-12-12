package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.List;


@Slf4j
@Singleton
public class ShoalPathTrackerOverlay extends Overlay implements PluginLifecycleComponent {

	@Nonnull
	private final Client client;
	private final SailingConfig config;
	private final ShoalPathTracker shoalPathTracker;
	private final ShoalPathTrackerCommand tracerCommand;

	@Inject
	public ShoalPathTrackerOverlay(@Nonnull Client client, SailingConfig config, ShoalPathTracker shoalPathTracker, ShoalPathTrackerCommand tracerCommand) {
		this.client = client;
		this.config = config;
		this.shoalPathTracker = shoalPathTracker;
		this.tracerCommand = tracerCommand;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW); // Draw paths below the shoal highlights
	}

	@Override
	public boolean isEnabled(SailingConfig config) {
		// Enabled via chat command: ::traceroutes, ::traceroutes on, ::traceroutes off
		return tracerCommand.isTracingEnabled();
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
		ShoalPathTracker.ShoalPath path = shoalPathTracker.getCurrentPath();

		if (path == null || !path.hasValidPath()) {
			return null;
		}

		renderShoalPath(graphics, path);
		return null;
	}

	private void renderShoalPath(Graphics2D graphics, ShoalPathTracker.ShoalPath path) {
		List<ShoalPathTracker.Waypoint> waypoints = path.getWaypoints();
		if (waypoints.size() < 2) {
			return;
		}

		// Use in-progress color (yellow) for live tracing
		Color pathColor = config.trawlingShoalPathColour();

		// Draw lines connecting the waypoints
		graphics.setStroke(new BasicStroke(1));

		net.runelite.api.Point previousCanvasPoint = null;

        for (int i = 0; i < waypoints.size(); i++) {
            ShoalPathTracker.Waypoint waypoint = waypoints.get(i);
            net.runelite.api.coords.WorldPoint worldPos = waypoint.getPosition();

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

            // Draw waypoint marker - different colors for stop points
            if (waypoint.isStopPoint()) {
                // Stop point - draw larger red circle
                graphics.setColor(Color.RED);
                graphics.fillOval(canvasPoint.getX() - 5, canvasPoint.getY() - 5, 10, 10);
                graphics.setColor(Color.WHITE);
                graphics.drawOval(canvasPoint.getX() - 5, canvasPoint.getY() - 5, 10, 10);
            } else {
                // Regular waypoint - small circle in path color
                graphics.setColor(pathColor);
                graphics.fillOval(canvasPoint.getX() - 3, canvasPoint.getY() - 3, 6, 6);
            }
            // draw waypoint index
            graphics.setColor(Color.GREEN);
            graphics.drawString(String.valueOf(i), canvasPoint.getX() - 3, canvasPoint.getY() - 3);

            previousCanvasPoint = canvasPoint;
        }
	}
}

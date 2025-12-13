package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.math.DoubleMath;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/*
    * Tracks the path of moving shoals (Bluefin and Vibrant) for route tracing.
    * Update with different shoal IDs to trace other shoals. Enable the tracer in config and
    * disable it once a route is fully traced to export the path to logs.
    * Note that the GameObject spawns are used to get accurate positions, while the WorldEntity
    * is used to track movement over time. Note that the stop points are not always accurate and may
    * require some manual adjustment.
 */
@Slf4j
@Singleton
public class ShoalPathTracker implements PluginLifecycleComponent {
	private static final int MIN_PATH_POINTS = 2; // Minimum points before we consider it a valid path
	private static final int MIN_WAYPOINT_DISTANCE = 1; // World coordinate units (tiles)
	private static final int MAX_WAYPOINT_DISTANCE = 30; // World coordinate units (tiles)
	private static final int MAX_PLAYER_DISTANCE = 300; // World coordinate units (tiles)
	private static final int AREA_MARGIN = 10; // World coordinate units (tiles)

	private final Client client;
	private final ShoalPathTrackerCommand tracerCommand;
	private final ShoalTracker shoalTracker;
	
	// Track the shoal path
	@Getter
    private ShoalPath currentPath = null;
	
	private Integer currentShoalId = null;

	@Inject
	public ShoalPathTracker(Client client, ShoalPathTrackerCommand tracerCommand, ShoalTracker shoalTracker) {
		this.client = client;
		this.tracerCommand = tracerCommand;
		this.shoalTracker = shoalTracker;
	}

	@Override
	public boolean isEnabled(SailingConfig config) {
		// Enabled via chat command: !traceroutes [on|off]
		return tracerCommand.isTracingEnabled();
	}

	@Override
	public void startUp() {
		log.debug("Route tracing enabled");
	}

	@Override
	public void shutDown() {
		log.debug("Route tracing disabled");
		exportPath();
		currentPath = null;
		currentShoalId = null;
	}

	private void exportPath() {
		if (currentPath == null) {
			log.debug("No shoal path to export");
			return;
		}
		
		if (currentPath.hasValidPath()) {
			log.debug("Exporting shoal path with {} waypoints", currentPath.getWaypoints().size());
			currentPath.logCompletedPath();
		} else {
			log.debug("Path too short to export (need at least {} points, have {})", 
				MIN_PATH_POINTS, currentPath.getWaypoints().size());
		}
	}


    
    private String getShoalName(int objectId) {
    	if (objectId == TrawlingData.ShoalObjectID.GIANT_KRILL) return "Giant Krill";
    	if (objectId == TrawlingData.ShoalObjectID.HADDOCK) return "Haddock";
    	if (objectId == TrawlingData.ShoalObjectID.YELLOWFIN) return "Yellowfin";
    	if (objectId == TrawlingData.ShoalObjectID.HALIBUT) return "Halibut";
    	if (objectId == TrawlingData.ShoalObjectID.BLUEFIN) return "Bluefin";
    	if (objectId == TrawlingData.ShoalObjectID.MARLIN) return "Marlin";
    	if (objectId == TrawlingData.ShoalObjectID.SHIMMERING) return "Shimmering";
    	if (objectId == TrawlingData.ShoalObjectID.GLISTENING) return "Glistening";
    	if (objectId == TrawlingData.ShoalObjectID.VIBRANT) return "Vibrant";
    	return "Unknown(" + objectId + ")";
    }

	@Subscribe
	public void onGameTick(GameTick e) {
		if (!shoalTracker.hasShoal()) {
			return;
		}
		
		// Initialize path when shoal is first detected
		if (currentPath == null && shoalTracker.hasShoal()) {
			// Get the first available shoal object to determine type
			Set<GameObject> shoalObjects = shoalTracker.getShoalObjects();
			if (!shoalObjects.isEmpty()) {
				GameObject firstShoal = shoalObjects.iterator().next();
				int objectId = firstShoal.getId();
				currentPath = new ShoalPath(objectId, shoalTracker);
				currentShoalId = objectId;
				log.debug("Path tracking initialized for {} (ID: {})", getShoalName(objectId), objectId);
			}
		}
		
		if (currentPath == null) {
			return;
		}
		
		// Update location from ShoalTracker
		shoalTracker.updateLocation();
		WorldPoint currentLocation = shoalTracker.getCurrentLocation();
		
		if (currentLocation != null) {
			// Check if shoal type changed (e.g., Halibut -> Glistening)
			Set<GameObject> shoalObjects = shoalTracker.getShoalObjects();
			if (!shoalObjects.isEmpty()) {
				GameObject currentShoal = shoalObjects.iterator().next();
				int objectId = currentShoal.getId();
				if (currentShoalId != null && currentShoalId != objectId) {
					log.debug("Shoal changed from {} to {}", getShoalName(currentShoalId), getShoalName(objectId));
					currentShoalId = objectId;
				}
			}
			
			currentPath.updatePosition(currentLocation);
		}
	}

    @Getter
	public class ShoalPath {
		private final int shoalId;
		private final ShoalTracker shoalTracker;
		private final LinkedList<Waypoint> waypoints = new LinkedList<>();
		private int ticksAtCurrentPosition = 0;

		public ShoalPath(int shoalId, ShoalTracker shoalTracker) {
			this.shoalId = shoalId;
			this.shoalTracker = shoalTracker;
		}

		public void addPosition(WorldPoint position) {
			WorldPoint playerLocation = SailingUtil.getTopLevelWorldPoint(client);
			boolean isTooFar = !isNearPosition(playerLocation, position, MAX_PLAYER_DISTANCE);

			// First position
			if (waypoints.isEmpty()) {
				if (!isTooFar) {
					waypoints.add(new Waypoint(position, false));
				}

				ticksAtCurrentPosition = 0;
				return;
			}

			Waypoint lastWaypoint = waypoints.peekLast();
			WorldPoint lastPosition = lastWaypoint.getPosition();
			ticksAtCurrentPosition++;

			// Only add if it's a new position (not too close to last recorded) and
			// not a buggy location (from when a shoal turns into a mixed fish shoal)
			boolean isTooClose = isNearPosition(lastPosition, position, MIN_WAYPOINT_DISTANCE);
			if (isTooClose || isTooFar) {
				return;
			}

			// Mark previous waypoint as a stop point if we stayed there for 10+ ticks
			if (ticksAtCurrentPosition >= 10) {
				lastWaypoint.setStopPoint(true);
				// Get the actual stationary duration from ShoalTracker
				int stationaryDuration = shoalTracker.getStationaryTicks();
				lastWaypoint.setStopDuration(stationaryDuration);
			}

			// combine sequential segments with the same slope to reduce number of waypoints
			if (!lastWaypoint.isStopPoint() && waypoints.size() >= 2) {
				Waypoint penultimateWaypoint = waypoints.get(waypoints.size() - 2);
				WorldPoint penultimatePosition = penultimateWaypoint.getPosition();
				double previousSlope = getSlope(penultimatePosition, lastPosition);
				double currentSlope = getSlope(lastPosition, position);

				boolean isSameSlope = DoubleMath.fuzzyEquals(previousSlope, currentSlope, 0.01);
				boolean isSegmentTooLong = !isNearPosition(lastPosition, penultimatePosition, MAX_WAYPOINT_DISTANCE);
				if (isSameSlope && !isSegmentTooLong) {
					waypoints.removeLast();
				}
			}

			waypoints.add(new Waypoint(position, false));
			ticksAtCurrentPosition = 0;
		}

		public void updatePosition(WorldPoint position) {
			addPosition(position);
		}

		private boolean isNearPosition(WorldPoint p1, WorldPoint p2, int range) {
			int dx = p1.getX() - p2.getX();
			int dy = p1.getY() - p2.getY();
			int distanceSquared = dx * dx + dy * dy;
			return distanceSquared < (range * range);
		}

		private double getSlope(WorldPoint p1, WorldPoint p2) {
			double dx = p1.getX() - p2.getX();
			double dy = p1.getY() - p2.getY();
			return dx / dy;
		}

		public boolean hasValidPath() {
			return waypoints.size() >= MIN_PATH_POINTS
				&& waypoints.stream().anyMatch(Waypoint::isStopPoint);
		}

		public List<Waypoint> getWaypoints() {
			return Collections.unmodifiableList(waypoints);
		}

		public void logCompletedPath() {
			// make sure first waypoint is a stop
			while (!Objects.requireNonNull(waypoints.peekFirst()).isStopPoint()) {
				waypoints.add(waypoints.pop());
			}

			String shoalName = ShoalPathTracker.this.getShoalName(shoalId);
			log.debug("=== SHOAL PATH EXPORT (ID: {}, Name: {}) ===", shoalId, shoalName);
			log.debug("Total waypoints: {}", waypoints.size());
			log.debug("");
			log.debug("// Shoal: {} (ID: {}) - Copy this into ShoalPaths.java:", shoalName, shoalId);
			log.debug("public static final WorldPoint[] SHOAL_{}_PATH = {", shoalId);

			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			List<Integer> stopPoints = new ArrayList<>();
			for (int i = 0; i < waypoints.size(); i++) {
				Waypoint wp = waypoints.get(i);
				WorldPoint pos = wp.getPosition();
				String comment = wp.isStopPoint() ? " // STOP POINT" : "";
				log.debug("    new WorldPoint({}, {}, {}),{}",
					pos.getX(), pos.getY(), pos.getPlane(), comment);

				minX = Math.min(minX, pos.getX());
				minY = Math.min(minY, pos.getY());
				maxX = Math.max(maxX, pos.getX());
				maxY = Math.max(maxY, pos.getY());

				if (wp.isStopPoint()) {
					stopPoints.add(i);
				}
			}
			
			log.debug("};");
			log.debug("");
			log.debug("Stop points: {}", waypoints.stream().filter(Waypoint::isStopPoint).count());
			log.debug("");
			
			// Log stop durations for analysis
			log.debug("Stop durations (ticks):");
			for (int i = 0; i < waypoints.size(); i++) {
				Waypoint wp = waypoints.get(i);
				if (wp.isStopPoint() && wp.getStopDuration() > 0) {
					log.debug("  Stop {} (index {}): {} ticks at {}", 
						stopPoints.indexOf(i) + 1, i, wp.getStopDuration(), wp.getPosition());
				}
			}
			
			// Calculate average stop duration
			List<Integer> durations = waypoints.stream()
				.filter(Waypoint::isStopPoint)
				.mapToInt(Waypoint::getStopDuration)
				.filter(d -> d > 0)
				.boxed()
				.collect(Collectors.toList());
			
			if (!durations.isEmpty()) {
				double avgDuration = durations.stream().mapToInt(Integer::intValue).average().orElse(0.0);
				int minDuration = durations.stream().mapToInt(Integer::intValue).min().orElse(0);
				int maxDuration = durations.stream().mapToInt(Integer::intValue).max().orElse(0);
				log.debug("Duration stats - Avg: {}, Min: {}, Max: {} ticks", avgDuration, minDuration, maxDuration);
			}
			else
			{
				log.debug("Duration empty, we simply just dont know");
			}
			log.debug("");
			
			log.debug("// Copy this into TrawlingData.java:");
			log.debug("AREA = {}, {}, {}, {}",
				minX - AREA_MARGIN, maxX + AREA_MARGIN, minY - AREA_MARGIN, maxY + AREA_MARGIN
			);
			log.debug("// Copy this into ShoalPathOverlay.java:");
			log.debug("STOP_INDICES = {};", stopPoints);
			log.debug("=====================================");
		}
	}

	@Getter
	public static class Waypoint {
		private final WorldPoint position;
		@Setter
        private boolean stopPoint;
		@Setter
		private int stopDuration = 0; // Duration in ticks that shoal was stationary at this point

		public Waypoint(WorldPoint position, boolean stopPoint) {
			this.position = position;
			this.stopPoint = stopPoint;
		}

    }
}

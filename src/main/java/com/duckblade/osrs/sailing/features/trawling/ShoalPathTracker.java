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
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;


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

	// WorldEntity config ID for moving shoals
	private static final int SHOAL_WORLD_ENTITY_CONFIG_ID = 4;
	
	// Bluefin/Vibrant shoal GameObject IDs - same route, different spawns change these to trace other shoals
	private static final int BLUEFIN_SHOAL_ID = TrawlingData.ShoalObjectID.GIANT_KRILL;
	private static final int VIBRANT_SHOAL_ID = TrawlingData.ShoalObjectID.SHIMMERING;
	
	private static final int MIN_PATH_POINTS = 2; // Minimum points before we consider it a valid path
	private static final int MIN_WAYPOINT_DISTANCE = 1; // World coordinate units (tiles)
	private static final int MAX_PLAYER_DISTANCE = 300; // World coordinate units (tiles)
	private static final int AREA_MARGIN = 10; // World coordinate units (tiles)

	private final Client client;
	private final SailingConfig config;
	private final ShoalPathTracerCommand tracerCommand;
	
	// Track the shoal path (Halibut or Glistening - same route)
	@Getter
    private ShoalPath currentPath = null;
	
	// Track the WorldEntity (moving shoal)
	private WorldEntity movingShoal = null;
	private Integer currentShoalId = null;
	
	private boolean wasTracking = false;

	@Inject
	public ShoalPathTracker(Client client, SailingConfig config, ShoalPathTracerCommand tracerCommand) {
		this.client = client;
		this.config = config;
		this.tracerCommand = tracerCommand;
	}

	@Override
	public boolean isEnabled(SailingConfig config) {
		// Enabled via chat command: !traceroutes [on|off]
		return tracerCommand.isTracingEnabled();
	}

	@Override
	public void startUp() {
		log.debug("Route tracing ENABLED - tracking Bluefin/Vibrant shoal (IDs: {}, {})", 
			BLUEFIN_SHOAL_ID, VIBRANT_SHOAL_ID);
		wasTracking = true;
	}

	@Override
	public void shutDown() {
		log.debug("Route tracing DISABLED");
		exportPath();
		currentPath = null;
		movingShoal = null;
		currentShoalId = null;
		wasTracking = false;
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

	@Subscribe
	public void onWorldEntitySpawned(WorldEntitySpawned e) {
		WorldEntity entity = e.getWorldEntity();
		
		// Only track shoal WorldEntity
		if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
			movingShoal = entity;
			log.debug("Shoal WorldEntity spawned, tracking movement");
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e) {
		GameObject obj = e.getGameObject();
		int objectId = obj.getId();
		
		// Only track Bluefin or Vibrant shoals
		if (objectId != BLUEFIN_SHOAL_ID && objectId != VIBRANT_SHOAL_ID) {
			return;
		}

		// Initialize path if needed
		if (currentPath == null) {
			currentPath = new ShoalPath(objectId);
			log.debug("Started tracking shoal ID {} ({})", objectId, 
				objectId == BLUEFIN_SHOAL_ID ? "Bluefin" : "Vibrant");
		} else if (currentShoalId != null && currentShoalId != objectId) {
			// Shoal changed type (e.g., Bluefin -> Vibrant)
			log.debug("Shoal changed from {} to {} - continuing same path", 
				currentShoalId == BLUEFIN_SHOAL_ID ? "Bluefin" : "Vibrant",
				objectId == BLUEFIN_SHOAL_ID ? "Bluefin" : "Vibrant");
		}
		
		// Store the current shoal type
		currentShoalId = objectId;
		
		// Convert to WorldPoint for absolute positioning
		LocalPoint localPos = obj.getLocalLocation();
		WorldPoint worldPos = WorldPoint.fromLocal(client, localPos);

        currentPath.addPosition(worldPos);
        log.debug("Shoal ID {} at {} (path size: {})", objectId, worldPos, currentPath.getWaypoints().size());
    }

	@Subscribe
	public void onGameTick(GameTick e) {
		if (movingShoal != null && currentShoalId != null && currentPath != null) {
			LocalPoint localPos = movingShoal.getCameraFocus();
			if (localPos != null) {
				WorldPoint worldPos = WorldPoint.fromLocal(client, localPos);
                currentPath.updatePosition(worldPos);
            }
		}
	}

    @Getter
	public class ShoalPath {
		private final int shoalId;
		private final LinkedList<Waypoint> waypoints = new LinkedList<>();
		private int ticksAtCurrentPosition = 0;

		public ShoalPath(int shoalId) {
			this.shoalId = shoalId;
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
			ticksAtCurrentPosition++;

			// Only add if it's a new position (not too close to last recorded) and
			// not a buggy location (from when a shoal turns into a mixed fish shoal)
			boolean isTooClose = isNearPosition(lastWaypoint.getPosition(), position, MIN_WAYPOINT_DISTANCE);
			if (isTooClose || isTooFar) {
				return;
			}

			// Mark previous waypoint as a stop point if we stayed there for 10+ ticks
			if (ticksAtCurrentPosition >= 10) {
				lastWaypoint.setStopPoint(true);
			}

			// combine sequential segments with the same slope to reduce number of waypoints
			if (!lastWaypoint.isStopPoint() && waypoints.size() >= 2) {
				Waypoint penultimateWaypoint = waypoints.get(waypoints.size() - 2);
				double previousSlope = getSlope(penultimateWaypoint.getPosition(), lastWaypoint.getPosition());
				double currentSlope = getSlope(lastWaypoint.getPosition(), position);

				if (DoubleMath.fuzzyEquals(previousSlope, currentSlope, 0.01)) {
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
			while (!waypoints.peekFirst().isStopPoint()) {
				waypoints.add(waypoints.pop());
			}

			log.debug("=== SHOAL PATH EXPORT (ID: {}) ===", shoalId);
			log.debug("Total waypoints: {}", waypoints.size());
			log.debug("");
			log.debug("// Shoal ID: {} - Copy this into ShoalPaths.java:", shoalId);
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

		public Waypoint(WorldPoint position, boolean stopPoint) {
			this.position = position;
			this.stopPoint = stopPoint;
		}

    }
}

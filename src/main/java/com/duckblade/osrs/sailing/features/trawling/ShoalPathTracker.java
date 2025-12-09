package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
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
import net.runelite.client.events.ConfigChanged;

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
	private static final int BLUEFIN_SHOAL_ID = 59739;
	private static final int VIBRANT_SHOAL_ID = 59742;
	
	private static final int MIN_PATH_POINTS = 8; // Minimum points before we consider it a valid path
	private static final int POSITION_TOLERANCE = 4; // World coordinate units (tiles)

	private final Client client;
	private final SailingConfig config;
	
	// Track the shoal path (Halibut or Glistening - same route)
	@Getter
    private ShoalPath currentPath = null;
	
	// Track the WorldEntity (moving shoal)
	private WorldEntity movingShoal = null;
	private Integer currentShoalId = null;
	
	private boolean wasTracking = false;
	private int tickCounter = 0;

	@Inject
	public ShoalPathTracker(Client client, SailingConfig config) {
		this.client = client;
		this.config = config;
	}

	@Override
	public boolean isEnabled(SailingConfig config) {
		return config.trawlingEnableRouteTracing();
	}

	@Override
	public void startUp() {
		log.info("Route tracing ENABLED - tracking Bluefin/Vibrant shoal (IDs: {}, {})", 
			BLUEFIN_SHOAL_ID, VIBRANT_SHOAL_ID);
		wasTracking = true;
	}

	@Override
	public void shutDown() {
		log.info("Route tracing DISABLED");
		exportPath();
		currentPath = null;
		movingShoal = null;
		currentShoalId = null;
		wasTracking = false;
		tickCounter = 0;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals("sailing")) {
			return;
		}
		
		if (event.getKey().equals("trawlingEnableRouteTracing")) {
			boolean isEnabled = config.trawlingEnableRouteTracing();
			
			// Detect when tracing is turned off
			if (wasTracking && !isEnabled) {
				log.info("Route tracing config disabled - exporting path");
				exportPath();
			}
		}
	}
	
	private void exportPath() {
		if (currentPath == null) {
			log.info("No shoal path to export");
			return;
		}
		
		if (currentPath.hasValidPath()) {
			log.info("Exporting shoal path with {} waypoints", currentPath.getWaypoints().size());
			currentPath.logCompletedPath();
		} else {
			log.info("Path too short to export (need at least {} points, have {})", 
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
			log.info("Started tracking shoal ID {} ({})", objectId, 
				objectId == BLUEFIN_SHOAL_ID ? "Bluefin" : "Vibrant");
		} else if (currentShoalId != null && currentShoalId != objectId) {
			// Shoal changed type (e.g., Bluefin -> Vibrant)
			log.info("Shoal changed from {} to {} - continuing same path", 
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
		// Track the moving shoal's position every 3 ticks
		tickCounter++;
		if (tickCounter >= 2 && movingShoal != null && currentShoalId != null && currentPath != null) {
			LocalPoint localPos = movingShoal.getCameraFocus();
			if (localPos != null) {
				WorldPoint worldPos = WorldPoint.fromLocal(client, localPos);
                currentPath.updatePosition(worldPos);
            }
			tickCounter = 0;
		}
	}

    @Getter
	public static class ShoalPath {
		private final int shoalId;
		private final List<Waypoint> waypoints = new ArrayList<>();
		private WorldPoint lastRecordedPosition;
		private int ticksAtCurrentPosition = 0;

		public ShoalPath(int shoalId) {
			this.shoalId = shoalId;
		}

		public void addPosition(WorldPoint position) {
			if (waypoints.isEmpty()) {
				// First position
				waypoints.add(new Waypoint(position, false));
				lastRecordedPosition = position;
				ticksAtCurrentPosition = 0;
				return;
			}

			// Only add if it's a new position (not too close to last recorded)
			if (lastRecordedPosition == null || !isNearPosition(position, lastRecordedPosition)) {
				// Mark previous waypoint as a stop point if we stayed there for 10+ ticks
				if (!waypoints.isEmpty() && ticksAtCurrentPosition >= 10) {
					waypoints.get(waypoints.size() - 1).setStopPoint(true);
				}
				
				waypoints.add(new Waypoint(position, false));
				lastRecordedPosition = position;
				ticksAtCurrentPosition = 0;
			} else {
				// Still at same position, increment tick counter
				ticksAtCurrentPosition++;
			}
		}

		public void updatePosition(WorldPoint position) {
			addPosition(position);
		}

		private boolean isNearPosition(WorldPoint p1, WorldPoint p2) {
			int dx = p1.getX() - p2.getX();
			int dy = p1.getY() - p2.getY();
			int distanceSquared = dx * dx + dy * dy;
			return distanceSquared < (POSITION_TOLERANCE * POSITION_TOLERANCE);
		}

		public boolean hasValidPath() {
			return waypoints.size() >= MIN_PATH_POINTS;
		}

		public List<Waypoint> getWaypoints() {
			return Collections.unmodifiableList(waypoints);
		}

		public void logCompletedPath() {
			log.info("=== SHOAL PATH EXPORT (ID: {}) ===", shoalId);
			log.info("Total waypoints: {}", waypoints.size());
			log.info("");
			log.info("// Shoal ID: {} - Copy this into ShoalPaths.java:", shoalId);
			log.info("public static final WorldPoint[] SHOAL_{}_PATH = {{", shoalId);
			
			for (int i = 0; i < waypoints.size(); i++) {
				Waypoint wp = waypoints.get(i);
				WorldPoint pos = wp.getPosition();
				String comment = wp.isStopPoint() ? " // STOP POINT" : "";
				String comma = (i < waypoints.size() - 1) ? "," : "";
				log.info("    new WorldPoint({}, {}, {}){}{}",
					pos.getX(), pos.getY(), pos.getPlane(), comma, comment);
			}
			
			log.info("}};");
			log.info("");
			log.info("Stop points: {}", waypoints.stream().filter(Waypoint::isStopPoint).count());
			log.info("=====================================");
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

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
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
	
	// Output file configuration
	private static final String OUTPUT_DIR = "src/main/java/com/duckblade/osrs/sailing/features/trawling/ShoalPathData";
	private static final String OUTPUT_FILE_PREFIX = "";
	private static final String OUTPUT_FILE_EXTENSION = ".java";

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
		// Enabled via chat command: ::trackroutes
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
				
				// Use more sophisticated path smoothing to eliminate small zigzags
				if (shouldSmoothPath(penultimatePosition, lastPosition, position)) {
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

		/**
		 * Determines if a path segment should be smoothed out to eliminate small zigzags.
		 * Uses multiple criteria to detect unnecessary waypoints that don't meaningfully
		 * contribute to following the shoal path.
		 */
		private boolean shouldSmoothPath(WorldPoint p1, WorldPoint p2, WorldPoint p3) {
			// Don't smooth if segment is too long (might be important waypoint)
			boolean isSegmentTooLong = !isNearPosition(p2, p1, MAX_WAYPOINT_DISTANCE);
			if (isSegmentTooLong) {
				return false;
			}

			// Check if the three points are nearly collinear (small zigzag)
			if (arePointsNearlyCollinear(p1, p2, p3)) {
				return true;
			}

			// Check if the deviation from direct path is small
			if (isSmallDeviation(p1, p2, p3)) {
				return true;
			}

			// Check if slopes are similar (more conservative than exact match)
			double previousSlope = getSlope(p1, p2);
			double currentSlope = getSlope(p2, p3);
			return DoubleMath.fuzzyEquals(previousSlope, currentSlope, 0.05); // More conservative tolerance
		}

		/**
		 * Checks if three points are nearly collinear using the cross product method.
		 * Small cross products indicate the points are nearly in a straight line.
		 */
		private boolean arePointsNearlyCollinear(WorldPoint p1, WorldPoint p2, WorldPoint p3) {
			// Calculate cross product of vectors (p1->p2) and (p2->p3)
			double dx1 = p2.getX() - p1.getX();
			double dy1 = p2.getY() - p1.getY();
			double dx2 = p3.getX() - p2.getX();
			double dy2 = p3.getY() - p2.getY();
			
			double crossProduct = Math.abs(dx1 * dy2 - dy1 * dx2);
			
			// More conservative threshold - only remove very straight lines
			// Reduced from 2.0 to 1.0 for more conservative smoothing
			return crossProduct < 1.0;
		}

		/**
		 * Checks if the middle point deviates only slightly from the direct path
		 * between the first and third points. Small deviations indicate unnecessary waypoints.
		 */
		private boolean isSmallDeviation(WorldPoint p1, WorldPoint p2, WorldPoint p3) {
			// Calculate distance from p2 to the line segment p1-p3
			double distanceToLine = distanceFromPointToLine(p2, p1, p3);
			
			// More conservative threshold - only remove points very close to the line
			// Reduced from 3.0 to 1.5 tiles for more conservative smoothing
			return distanceToLine < 1.5;
		}

		/**
		 * Calculates the perpendicular distance from a point to a line segment.
		 */
		private double distanceFromPointToLine(WorldPoint point, WorldPoint lineStart, WorldPoint lineEnd) {
			double dx = lineEnd.getX() - lineStart.getX();
			double dy = lineEnd.getY() - lineStart.getY();
			
			// If line segment has zero length, return distance to start point
			if (dx == 0 && dy == 0) {
				return Math.hypot(point.getX() - lineStart.getX(), point.getY() - lineStart.getY());
			}
			
			// Calculate the perpendicular distance using the formula:
			// distance = |ax + by + c| / sqrt(a² + b²)
			// where the line is ax + by + c = 0
			double a = dy;
			double b = -dx;
			double c = dx * lineStart.getY() - dy * lineStart.getX();
			
			return Math.abs(a * point.getX() + b * point.getY() + c) / Math.sqrt(a * a + b * b);
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
			
			// Write to file
			try {
				writePathToFile(shoalName);
				ShoalPathTracker.log.info("Shoal path exported to file: {}/{}{}{}", 
					OUTPUT_DIR, OUTPUT_FILE_PREFIX, shoalId, OUTPUT_FILE_EXTENSION);
			} catch (IOException e) {
				ShoalPathTracker.log.error("Failed to write path to file", e);
				// Fallback to log output
				logPathToConsole(shoalName);
			}
		}
		
		private void writePathToFile(String shoalName) throws IOException {
			// Create output directory if it doesn't exist
			Path outputDir = Paths.get(OUTPUT_DIR);
			if (!Files.exists(outputDir)) {
				Files.createDirectories(outputDir);
			}
			
			// Generate class/enum name
			String className = "Shoal" + shoalName.replaceAll("[^A-Za-z0-9]", "") + "Area";
			
			// Create output file with timestamp
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
			String filename = String.format("%s%s%s", OUTPUT_FILE_PREFIX, className, OUTPUT_FILE_EXTENSION);
			Path outputFile = outputDir.resolve(filename);
			
			// Calculate bounds and stop points
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			List<Integer> stopPoints = new ArrayList<>();
			
			for (int i = 0; i < waypoints.size(); i++) {
				Waypoint wp = waypoints.get(i);
				WorldPoint pos = wp.getPosition();
				
				minX = Math.min(minX, pos.getX());
				minY = Math.min(minY, pos.getY());
				maxX = Math.max(maxX, pos.getX());
				maxY = Math.max(maxY, pos.getY());
				
				if (wp.isStopPoint()) {
					stopPoints.add(i);
				}
			}
			
			String enumName = shoalName.toUpperCase().replaceAll("[^A-Z0-9]", "_") + "_AREA";
			
			// Write to file
			try (BufferedWriter writer = Files.newBufferedWriter(outputFile, 
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				
				// File header
				writer.write("// ========================================\n");
				writer.write("// Shoal Area Export\n");
				writer.write("// ========================================\n");
				writer.write("// Shoal: " + shoalName + "\n");
				writer.write("// Shoal ID: " + shoalId + "\n");
				writer.write("// Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
				writer.write("// Total waypoints: " + waypoints.size() + "\n");
				writer.write("// Stop points: " + stopPoints.size() + "\n");
				writer.write("// Stop duration: Retrieved from Shoal." + shoalName.toUpperCase().replace(" ", "_") + ".getStopDuration()\n");
				writer.write("// ========================================\n\n");
				
				// Package and imports
				writer.write("package com.duckblade.osrs.sailing.features.trawling.ShoalPathData;\n\n");
				writer.write("import com.duckblade.osrs.sailing.features.trawling.Shoal;\n");
				writer.write("import com.duckblade.osrs.sailing.features.trawling.ShoalAreaData;\n");
				writer.write("import com.duckblade.osrs.sailing.features.trawling.ShoalWaypoint;\n");
				writer.write("import net.runelite.api.coords.WorldArea;\n");
				writer.write("import net.runelite.api.coords.WorldPoint;\n\n");
				
				// Class definition with complete area data
				writer.write("/**\n");
				writer.write(" * Shoal area definition for " + shoalName + " (ID: " + shoalId + ")\n");
				writer.write(" * Contains waypoint path and area bounds.\n");
				writer.write(" * Stop duration is retrieved from the Shoal enum.\n");
				writer.write(" * Generated by ShoalPathTracker on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
				writer.write(" */\n");
				writer.write("public class " + className + " implements ShoalAreaData {\n\n");
				
				// Area bounds
				int areaX = minX - AREA_MARGIN;
				int areaY = minY - AREA_MARGIN;
				int areaWidth = maxX - minX + 2 * AREA_MARGIN;
				int areaHeight = maxY - minY + 2 * AREA_MARGIN;
				
				writer.write("\t/** Area bounds for this shoal region */\n");
				writer.write(String.format("\tpublic static final WorldArea AREA = new WorldArea(%d, %d, %d, %d, 0);\n\n",
					areaX, areaY, areaWidth, areaHeight));
				
				// Shoal type
				writer.write("\t/** Shoal type for this area */\n");
				writer.write("\tpublic static final Shoal SHOAL_TYPE = Shoal." + shoalName.toUpperCase().replace(" ", "_") + ";\n\n");
				
				// Waypoints array
				writer.write("\t/** Complete waypoint path with stop point information */\n");
				writer.write("\tpublic static final ShoalWaypoint[] WAYPOINTS = {\n");
				
				for (Waypoint wp : waypoints) {
					WorldPoint pos = wp.getPosition();
					writer.write(String.format("\t\tnew ShoalWaypoint(new WorldPoint(%d, %d, %d), %s),\n",
						pos.getX(), pos.getY(), pos.getPlane(), 
						wp.isStopPoint() ? "true" : "false"));
				}
				
				writer.write("\t};\n\n");
				
				// Singleton instance and interface implementations
				writer.write("\t// Singleton instance for interface access\n");
				writer.write("\tpublic static final " + className + " INSTANCE = new " + className + "();\n");
				writer.write("\t\n");
				writer.write("\tprivate " + className + "() {} // Private constructor\n");
				writer.write("\t\n");
				writer.write("\t// Interface implementations\n");
				writer.write("\t@Override\n");
				writer.write("\tpublic WorldArea getArea() { return AREA; }\n");
				writer.write("\t\n");
				writer.write("\t@Override\n");
				writer.write("\tpublic ShoalWaypoint[] getWaypoints() { return WAYPOINTS; }\n");
				writer.write("\t\n");
				writer.write("\t@Override\n");
				writer.write("\tpublic Shoal getShoalType() { return SHOAL_TYPE; }\n");
				
				writer.write("}\n\n");
				
				// Enum entry for ShoalFishingArea
				writer.write("// ========================================\n");
				writer.write("// Integration with ShoalFishingArea enum\n");
				writer.write("// ========================================\n");
				writer.write("// Add this entry to ShoalFishingArea enum:\n");
				writer.write("/*\n");
				writer.write(enumName + "(" + className + ".INSTANCE),\n");
				writer.write("*/\n\n");
				
				// Usage examples
				writer.write("// ========================================\n");
				writer.write("// Usage Examples\n");
				writer.write("// ========================================\n");
				writer.write("// Check if player is in area:\n");
				writer.write("// boolean inArea = " + className + ".INSTANCE.contains(playerLocation);\n\n");
				writer.write("// Get waypoints for rendering:\n");
				writer.write("// WorldPoint[] path = " + className + ".INSTANCE.getPositions();\n\n");
				writer.write("// Get stop duration (from Shoal enum):\n");
				writer.write("// int duration = " + className + ".INSTANCE.getStopDuration();\n\n");
				writer.write("// Access static fields directly:\n");
				writer.write("// WorldArea area = " + className + ".AREA;\n");
				writer.write("// ShoalWaypoint[] waypoints = " + className + ".WAYPOINTS;\n");
				writer.write("// Shoal shoalType = " + className + ".SHOAL_TYPE;\n\n");
				
				// Detailed analysis
				writer.write("// ========================================\n");
				writer.write("// Analysis Data\n");
				writer.write("// ========================================\n");
				writer.write("// Area bounds: " + areaX + ", " + areaY + ", " + areaWidth + ", " + areaHeight + "\n");
				writer.write("// Stop points: " + stopPoints.size() + " total\n");
				writer.write("// Stop duration: Retrieved from " + shoalName.toUpperCase().replace(" ", "_") + " shoal type\n");
				
				// Stop point details
				writer.write("// Stop point details:\n");
				for (int i = 0; i < waypoints.size(); i++) {
					Waypoint wp = waypoints.get(i);
					if (wp.isStopPoint()) {
						int stopNumber = stopPoints.indexOf(i) + 1;
						writer.write(String.format("// Stop %d (index %d): %s\n",
							stopNumber, i, wp.getPosition()));
					}
				}
				
				writer.write("\n// ========================================\n");
				writer.write("// End of Export\n");
				writer.write("// ========================================\n");
			}
		}
		
		private void logPathToConsole(String shoalName) {
			// Fallback: log to console in old format
			ShoalPathTracker.log.debug("=== SHOAL PATH EXPORT (ID: {}, Name: {}) ===", shoalId, shoalName);
			ShoalPathTracker.log.debug("Total waypoints: {}", waypoints.size());
			ShoalPathTracker.log.debug("");
			ShoalPathTracker.log.debug("// Shoal: {} (ID: {}) - Copy this into ShoalPaths.java:", shoalName, shoalId);
			ShoalPathTracker.log.debug("public static final WorldPoint[] SHOAL_{}_PATH = {", shoalId);

			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			List<Integer> stopPoints = new ArrayList<>();
			for (int i = 0; i < waypoints.size(); i++) {
				Waypoint wp = waypoints.get(i);
				WorldPoint pos = wp.getPosition();
				String comment = wp.isStopPoint() ? " // STOP POINT" : "";
				ShoalPathTracker.log.debug("    new WorldPoint({}, {}, {}),{}",
					pos.getX(), pos.getY(), pos.getPlane(), comment);

				minX = Math.min(minX, pos.getX());
				minY = Math.min(minY, pos.getY());
				maxX = Math.max(maxX, pos.getX());
				maxY = Math.max(maxY, pos.getY());

				if (wp.isStopPoint()) {
					stopPoints.add(i);
				}
			}
			
			ShoalPathTracker.log.debug("};");
			ShoalPathTracker.log.debug("");
			ShoalPathTracker.log.debug("Stop points: {}", waypoints.stream().filter(Waypoint::isStopPoint).count());
			ShoalPathTracker.log.debug("");
			
			// Log stop durations for analysis
			ShoalPathTracker.log.debug("Stop durations (ticks):");
			for (int i = 0; i < waypoints.size(); i++) {
				Waypoint wp = waypoints.get(i);
				if (wp.isStopPoint() && wp.getStopDuration() > 0) {
					ShoalPathTracker.log.debug("  Stop {} (index {}): {} ticks at {}", 
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
				ShoalPathTracker.log.debug("Duration stats - Avg: {}, Min: {}, Max: {} ticks", avgDuration, minDuration, maxDuration);
			}
			else
			{
				ShoalPathTracker.log.debug("Duration empty, we simply just dont know");
			}
			ShoalPathTracker.log.debug("");
			
			ShoalPathTracker.log.debug("// Copy this into TrawlingData.java:");
			ShoalPathTracker.log.debug("AREA = {}, {}, {}, {}",
				minX - AREA_MARGIN, maxX + AREA_MARGIN, minY - AREA_MARGIN, maxY + AREA_MARGIN
			);
			ShoalPathTracker.log.debug("// Copy this into ShoalPathOverlay.java:");
			ShoalPathTracker.log.debug("STOP_INDICES = {};", stopPoints);
			ShoalPathTracker.log.debug("=====================================");
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

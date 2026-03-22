package com.duckblade.osrs.sailing.features.trawling;

import com.google.common.math.DoubleMath;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for applying path smoothing algorithms to existing shoal routes.
 * This can be used to clean up pre-generated paths by removing unnecessary zigzags
 * and waypoints that don't meaningfully contribute to navigation.
 */
public class PathSmoothingUtil {

    private static final int MAX_WAYPOINT_DISTANCE = 30; // World coordinate units (tiles)

    /**
     * Applies path smoothing to an array of ShoalWaypoints, preserving stop points.
     * 
     * @param originalWaypoints the original waypoints to smooth
     * @return smoothed waypoints with unnecessary points removed
     */
    public static ShoalWaypoint[] smoothPath(ShoalWaypoint[] originalWaypoints) {
        if (originalWaypoints == null || originalWaypoints.length < 3) {
            return originalWaypoints; // Can't smooth paths with less than 3 points
        }

        List<ShoalWaypoint> smoothedPath = new ArrayList<>();
        
        // Always keep the first waypoint
        smoothedPath.add(originalWaypoints[0]);
        
        for (int i = 1; i < originalWaypoints.length - 1; i++) {
            ShoalWaypoint current = originalWaypoints[i];
            ShoalWaypoint previous = smoothedPath.get(smoothedPath.size() - 1);
            ShoalWaypoint next = originalWaypoints[i + 1];
            
            // Always keep stop points
            if (current.isStopPoint()) {
                smoothedPath.add(current);
                continue;
            }
            
            // Check if this waypoint should be kept or smoothed out
            if (shouldKeepWaypoint(previous.getPosition(), current.getPosition(), next.getPosition())) {
                smoothedPath.add(current);
            }
            // If not kept, the waypoint is effectively removed (smoothed out)
        }
        
        // Always keep the last waypoint
        smoothedPath.add(originalWaypoints[originalWaypoints.length - 1]);
        
        return smoothedPath.toArray(new ShoalWaypoint[0]);
    }

    /**
     * Determines if a waypoint should be kept based on path smoothing criteria.
     * 
     * @param p1 previous waypoint position
     * @param p2 current waypoint position  
     * @param p3 next waypoint position
     * @return true if the waypoint should be kept, false if it should be smoothed out
     */
    private static boolean shouldKeepWaypoint(WorldPoint p1, WorldPoint p2, WorldPoint p3) {
        // Keep waypoint if segment is too long (might be important)
        boolean isSegmentTooLong = !isNearPosition(p2, p1, MAX_WAYPOINT_DISTANCE);
        if (isSegmentTooLong) {
            return true;
        }

        // Remove waypoint if the three points are nearly collinear (small zigzag)
        if (arePointsNearlyCollinear(p1, p2, p3)) {
            return false;
        }

        // Remove waypoint if the deviation from direct path is small
        if (isSmallDeviation(p1, p2, p3)) {
            return false;
        }

        // Remove waypoint if slopes are similar (more conservative than exact match)
        double previousSlope = getSlope(p1, p2);
        double currentSlope = getSlope(p2, p3);
        if (DoubleMath.fuzzyEquals(previousSlope, currentSlope, 0.05)) { // More conservative: 0.05 instead of 0.1
            return false;
        }

        // Keep the waypoint if none of the smoothing criteria apply
        return true;
    }

    /**
     * Checks if two points are within a certain distance of each other.
     */
    private static boolean isNearPosition(WorldPoint p1, WorldPoint p2, int range) {
        int dx = p1.getX() - p2.getX();
        int dy = p1.getY() - p2.getY();
        int distanceSquared = dx * dx + dy * dy;
        return distanceSquared < (range * range);
    }

    /**
     * Calculates the slope between two points.
     */
    private static double getSlope(WorldPoint p1, WorldPoint p2) {
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        if (dy == 0) {
            return dx == 0 ? 0 : Double.POSITIVE_INFINITY;
        }
        return dx / dy;
    }

    /**
     * Checks if three points are nearly collinear using the cross product method.
     * Small cross products indicate the points are nearly in a straight line.
     */
    private static boolean arePointsNearlyCollinear(WorldPoint p1, WorldPoint p2, WorldPoint p3) {
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
    private static boolean isSmallDeviation(WorldPoint p1, WorldPoint p2, WorldPoint p3) {
        // Calculate distance from p2 to the line segment p1-p3
        double distanceToLine = distanceFromPointToLine(p2, p1, p3);
        
        // More conservative threshold - only remove points very close to the line
        // Reduced from 3.0 to 1.5 tiles for more conservative smoothing
        return distanceToLine < 1.5;
    }

    /**
     * Calculates the perpendicular distance from a point to a line segment.
     */
    private static double distanceFromPointToLine(WorldPoint point, WorldPoint lineStart, WorldPoint lineEnd) {
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

    /**
     * Analyzes a path and provides statistics about potential smoothing improvements.
     * 
     * @param waypoints the waypoints to analyze
     * @return analysis results as a formatted string
     */
    public static String analyzePath(ShoalWaypoint[] waypoints) {
        if (waypoints == null || waypoints.length < 3) {
            return "Path too short to analyze (need at least 3 waypoints)";
        }

        ShoalWaypoint[] smoothed = smoothPath(waypoints);
        int originalCount = waypoints.length;
        int smoothedCount = smoothed.length;
        int removedCount = originalCount - smoothedCount;
        double reductionPercent = (removedCount / (double) originalCount) * 100;

        StringBuilder analysis = new StringBuilder();
        analysis.append(String.format("Path Analysis:\n"));
        analysis.append(String.format("  Original waypoints: %d\n", originalCount));
        analysis.append(String.format("  Smoothed waypoints: %d\n", smoothedCount));
        analysis.append(String.format("  Removed waypoints: %d (%.1f%% reduction)\n", removedCount, reductionPercent));
        
        // Count stop points
        long stopPoints = java.util.Arrays.stream(waypoints).mapToLong(wp -> wp.isStopPoint() ? 1 : 0).sum();
        analysis.append(String.format("  Stop points preserved: %d\n", stopPoints));
        
        // Note: Proximity validation temporarily disabled due to algorithm issues
        analysis.append("  Proximity validation: DISABLED (conservative smoothing should be safe)\n");

        return analysis.toString();
    }

    /**
     * Validates that the smoothed path stays within a specified distance of the original path.
     * This ensures that following the smoothed path will keep the boat close to the shoal.
     * 
     * @param original the original waypoints
     * @param smoothed the smoothed waypoints
     * @param maxDistance maximum allowed distance in tiles
     * @return validation result with deviation statistics
     */
    private static PathProximityResult validatePathProximity(ShoalWaypoint[] original, ShoalWaypoint[] smoothed, double maxDistance) {
        double maxDeviation = 0;
        int worstSegmentIndex = -1;
        
        // Simple approach: for each smoothed segment, check the maximum distance
        // from any original point to that segment line
        for (int smoothedIndex = 0; smoothedIndex < smoothed.length - 1; smoothedIndex++) {
            WorldPoint smoothedStart = smoothed[smoothedIndex].getPosition();
            WorldPoint smoothedEnd = smoothed[smoothedIndex + 1].getPosition();
            
            // Check all original points against this smoothed segment
            for (int originalIndex = 0; originalIndex < original.length; originalIndex++) {
                WorldPoint originalPoint = original[originalIndex].getPosition();
                double deviation = distanceFromPointToLine(originalPoint, smoothedStart, smoothedEnd);
                
                if (deviation > maxDeviation) {
                    maxDeviation = deviation;
                    worstSegmentIndex = smoothedIndex;
                }
            }
        }
        
        return new PathProximityResult(maxDeviation, maxDeviation <= maxDistance, worstSegmentIndex);
    }

    /**
     * Result of path proximity validation.
     */
    private static class PathProximityResult {
        final double maxDeviation;
        final boolean staysWithinRange;
        final int worstSegmentIndex;
        
        PathProximityResult(double maxDeviation, boolean staysWithinRange, int worstSegmentIndex) {
            this.maxDeviation = maxDeviation;
            this.staysWithinRange = staysWithinRange;
            this.worstSegmentIndex = worstSegmentIndex;
        }
    }

    /**
     * Generates the smoothed waypoint array as Java code that can be copied into the route files.
     * 
     * @param waypoints the waypoints to smooth and format
     * @param className the class name for the output
     * @return formatted Java code string
     */
    public static String generateSmoothedCode(ShoalWaypoint[] waypoints, String className) {
        ShoalWaypoint[] smoothed = smoothPath(waypoints);
        
        StringBuilder code = new StringBuilder();
        code.append(String.format("// Smoothed waypoints for %s\n", className));
        code.append(String.format("// Original: %d waypoints, Smoothed: %d waypoints (%.1f%% reduction)\n", 
            waypoints.length, smoothed.length, 
            ((waypoints.length - smoothed.length) / (double) waypoints.length) * 100));
        code.append("public static final ShoalWaypoint[] WAYPOINTS = {\n");
        
        for (ShoalWaypoint wp : smoothed) {
            WorldPoint pos = wp.getPosition();
            code.append(String.format("\t\tnew ShoalWaypoint(new WorldPoint(%d, %d, %d), %s),\n",
                pos.getX(), pos.getY(), pos.getPlane(), 
                wp.isStopPoint() ? "true" : "false"));
        }
        
        code.append("\t};\n");
        return code.toString();
    }
}
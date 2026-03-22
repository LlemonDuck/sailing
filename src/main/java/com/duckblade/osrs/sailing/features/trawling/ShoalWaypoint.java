package com.duckblade.osrs.sailing.features.trawling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a waypoint in a shoal's movement path.
 * Contains position information and whether it's a stop point.
 * Stop duration is area-specific, not waypoint-specific.
 */
@Getter
@RequiredArgsConstructor
public class ShoalWaypoint {
    private final WorldPoint position;
    private final boolean stopPoint;

    /**
     * Convenience constructor for non-stop waypoints.
     */
    public ShoalWaypoint(WorldPoint position) {
        this(position, false);
    }

    /**
     * Extract all positions from an array of waypoints.
     * Useful for compatibility with existing code that expects WorldPoint arrays.
     */
    public static WorldPoint[] getPositions(ShoalWaypoint[] waypoints) {
        return Arrays.stream(waypoints)
            .map(ShoalWaypoint::getPosition)
            .toArray(WorldPoint[]::new);
    }

    /**
     * Get indices of all stop points in the waypoint array.
     * Useful for compatibility with existing code that uses stop index arrays.
     */
    public static int[] getStopIndices(ShoalWaypoint[] waypoints) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < waypoints.length; i++) {
            if (waypoints[i].isStopPoint()) {
                indices.add(i);
            }
        }
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Get all stop point waypoints from the array.
     */
    public static ShoalWaypoint[] getStopPoints(ShoalWaypoint[] waypoints) {
        return Arrays.stream(waypoints)
            .filter(ShoalWaypoint::isStopPoint)
            .toArray(ShoalWaypoint[]::new);
    }

    /**
     * Count the number of stop points in the waypoint array.
     */
    public static int getStopPointCount(ShoalWaypoint[] waypoints) {
        return (int) Arrays.stream(waypoints)
            .filter(ShoalWaypoint::isStopPoint)
            .count();
    }

    @Override
    public String toString() {
        return String.format("ShoalWaypoint{position=%s, stopPoint=%s}", position, stopPoint);
    }
}
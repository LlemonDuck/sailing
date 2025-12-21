package com.duckblade.osrs.sailing.features.trawling;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * Interface for shoal area data classes.
 * Provides common methods for all shoal areas while allowing static usage.
 */
public interface ShoalAreaData {
    
    /**
     * Get the area bounds for this shoal region.
     */
    WorldArea getArea();
    
    /**
     * Get the complete waypoint path with stop point information.
     */
    ShoalWaypoint[] getWaypoints();
    
    /**
     * Get the shoal type for this area.
     */
    Shoal getShoalType();
    
    /**
     * Get the duration in ticks that shoals stop at each stop point in this area.
     */
    default int getStopDuration() { return getShoalType().getStopDuration(); }
    
    // Default implementations for common operations
    
    /**
     * Check if a world point is within this shoal area.
     */
    default boolean contains(WorldPoint point) {
        return getArea().contains(point);
    }
    
    /**
     * Get all waypoint positions as WorldPoint array (for compatibility).
     */
    default WorldPoint[] getPositions() {
        return ShoalWaypoint.getPositions(getWaypoints());
    }
    
    /**
     * Get stop point indices (for compatibility).
     */
    default int[] getStopIndices() {
        return ShoalWaypoint.getStopIndices(getWaypoints());
    }
    
    /**
     * Get the number of stop points in this area.
     */
    default int getStopPointCount() {
        return ShoalWaypoint.getStopPointCount(getWaypoints());
    }
    
    /**
     * Get all stop point waypoints from the array.
     */
    default ShoalWaypoint[] getStopPoints() {
        return ShoalWaypoint.getStopPoints(getWaypoints());
    }
    
    /**
     * Check if this area has valid data.
     */
    default boolean isValidArea() {
        return getArea() != null && getWaypoints() != null && getWaypoints().length > 0;
    }
}
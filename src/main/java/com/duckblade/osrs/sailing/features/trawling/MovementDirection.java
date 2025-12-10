package com.duckblade.osrs.sailing.features.trawling;

/**
 * Represents the direction of shoal movement for depth transitions
 */
public enum MovementDirection {
    SHALLOWER,  // Moderate → Shallow
    DEEPER,     // Moderate → Deep
    UNKNOWN     // No direction detected yet
}
package com.duckblade.osrs.sailing.features.trawling;

/**
 * Represents the depth levels for fishing nets in trawling
 */
public enum NetDepth {
    SHALLOW(0),
    MODERATE(1),
    DEEP(2);

    private final int level;

    NetDepth(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isShallowerThan(NetDepth other) {
        return this.level < other.level;
    }

    public boolean isDeeperThan(NetDepth other) {
        return this.level > other.level;
    }
}
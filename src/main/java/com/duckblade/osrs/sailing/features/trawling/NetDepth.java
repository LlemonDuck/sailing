package com.duckblade.osrs.sailing.features.trawling;

import lombok.Getter;

/**
 * Represents the depth levels for fishing nets in trawling
 */
@Getter
public enum NetDepth {
    SHALLOW(1),
    MODERATE(2),
    DEEP(3);

    private final int level;

    NetDepth(int level) {
        this.level = level;
    }

    public boolean isShallowerThan(NetDepth other) {
        return this.level < other.level;
    }

    public boolean isDeeperThan(NetDepth other) {
        return this.level > other.level;
    }
}
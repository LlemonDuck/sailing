package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.model.ShoalDepth;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service component that tracks the current depth of both trawling nets using varbits
 */
@Slf4j
@Singleton
public class NetDepthTracker implements PluginLifecycleComponent {

    // Varbit IDs for trawling net depths
    private static final int TRAWLING_NET_PORT_VARBIT = VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_1_DEPTH;
    private static final int TRAWLING_NET_STARBOARD_VARBIT = VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_0_DEPTH;

    private final Client client;

    // Cached values for performance
    private ShoalDepth portNetDepth;
    private ShoalDepth starboardNetDepth;
    private boolean portCacheValid = false;
    private boolean starboardCacheValid = false;

    /**
     * Creates a new NetDepthTracker with the specified client.
     *
     * @param client the RuneLite client instance
     */
    @Inject
    public NetDepthTracker(Client client) {
        this.client = client;
    }

    @Override
    public void startUp() {
        log.debug("NetDepthTracker started");
        // Don't read varbits during startup - they will be read lazily when needed
    }

    @Override
    public void shutDown() {
        log.debug("NetDepthTracker shut down");
        invalidateCache();
    }

    /**
     * Gets the current port net depth.
     *
     * @return the port net depth, or null if net is not lowered
     */
    public ShoalDepth getPortNetDepth() {
        if (!portCacheValid) {
            portNetDepth = getNetDepthFromVarbit(TRAWLING_NET_PORT_VARBIT);
            portCacheValid = true;
            log.debug("Port net depth (fresh): {}", portNetDepth);
        }
        return portNetDepth;
    }

    /**
     * Gets the current starboard net depth.
     *
     * @return the starboard net depth, or null if net is not lowered
     */
    public ShoalDepth getStarboardNetDepth() {
        if (!starboardCacheValid) {
            starboardNetDepth = getNetDepthFromVarbit(TRAWLING_NET_STARBOARD_VARBIT);
            starboardCacheValid = true;
            log.debug("Starboard net depth (fresh): {}", starboardNetDepth);
        }
        return starboardNetDepth;
    }

    /**
     * Checks if both nets are at the same depth.
     *
     * @return true if both nets are at the same depth, false otherwise
     */
    public boolean areNetsAtSameDepth() {
        ShoalDepth port = getPortNetDepth();
        ShoalDepth starboard = getStarboardNetDepth();
        return port != null && port == starboard;
    }

    /**
     * Checks if both nets are at the specified depth.
     *
     * @param targetDepth the depth to check against
     * @return true if both nets are at the target depth, false otherwise
     */
    public boolean areNetsAtDepth(ShoalDepth targetDepth) {
        return getPortNetDepth() == targetDepth && getStarboardNetDepth() == targetDepth;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged e) {
        int varbitId = e.getVarbitId();
        
        if (varbitId == TRAWLING_NET_PORT_VARBIT) {
            ShoalDepth oldDepth = portNetDepth;
            int varbitValue = e.getValue();
            portNetDepth = getNetDepthFromVarbit(TRAWLING_NET_PORT_VARBIT);
            portCacheValid = true;
            log.debug("Port net depth changed: {} -> {} (varbit: {}, value: {}, converted: {})", 
                     oldDepth, portNetDepth, varbitId, varbitValue, portNetDepth);
        } else if (varbitId == TRAWLING_NET_STARBOARD_VARBIT) {
            ShoalDepth oldDepth = starboardNetDepth;
            int varbitValue = e.getValue();
            starboardNetDepth = getNetDepthFromVarbit(TRAWLING_NET_STARBOARD_VARBIT);
            starboardCacheValid = true;
            log.debug("Starboard net depth changed: {} -> {} (varbit: {}, value: {}, converted: {})", 
                     oldDepth, starboardNetDepth, varbitId, varbitValue, starboardNetDepth);
        }
    }

    /**
     * Convert varbit value to ShoalDepth enum
     */
    private ShoalDepth getNetDepthFromVarbit(int varbitId) {
        int varbitValue = client.getVarbitValue(varbitId);
        
        // Convert varbit value to ShoalDepth (0=net not lowered, 1=shallow, 2=moderate, 3=deep)
        switch (varbitValue) {
            case 0:
                return null; // Net not lowered
            case 1:
                return ShoalDepth.SHALLOW;
            case 2:
                return ShoalDepth.MODERATE;
            case 3:
                return ShoalDepth.DEEP;
            default:
                log.warn("Unknown varbit value for net depth: {} (varbit: {})", varbitValue, varbitId);
                return null;
        }
    }

    /**
     * Update cached values from current varbit state
     */
    private void updateCachedValues() {
        portNetDepth = getNetDepthFromVarbit(TRAWLING_NET_PORT_VARBIT);
        starboardNetDepth = getNetDepthFromVarbit(TRAWLING_NET_STARBOARD_VARBIT);
        portCacheValid = true;
        starboardCacheValid = true;
        log.debug("Updated cached net depths - Port: {}, Starboard: {}", portNetDepth, starboardNetDepth);
    }

    /**
     * Forces refresh of cached values (useful for debugging or when cache might be stale).
     */
    public void refreshCache() {
        log.debug("Force refreshing net depth cache");
        invalidateCache();
        updateCachedValues();
    }

    /**
     * Invalidates the cache, forcing fresh reads on next access.
     */
    private void invalidateCache() {
        portNetDepth = null;
        starboardNetDepth = null;
        portCacheValid = false;
        starboardCacheValid = false;
    }
}
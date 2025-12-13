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

    @Inject
    public NetDepthTracker(Client client) {
        this.client = client;
    }

    @Override
    public void startUp() {
        log.debug("NetDepthTracker started");
        updateCachedValues();
    }

    @Override
    public void shutDown() {
        log.debug("NetDepthTracker shut down");
        portNetDepth = null;
        starboardNetDepth = null;
    }

    /**
     * Get the current port net depth
     */
    public ShoalDepth getPortNetDepth() {
        if (portNetDepth == null) {
            portNetDepth = getNetDepthFromVarbit(TRAWLING_NET_PORT_VARBIT);
            log.debug("Port net depth (fresh): {}", portNetDepth);
        }
        return portNetDepth;
    }

    /**
     * Get the current starboard net depth
     */
    public ShoalDepth getStarboardNetDepth() {
        if (starboardNetDepth == null) {
            starboardNetDepth = getNetDepthFromVarbit(TRAWLING_NET_STARBOARD_VARBIT);
            log.debug("Starboard net depth (fresh): {}", starboardNetDepth);
        }
        return starboardNetDepth;
    }

    /**
     * Check if both nets are at the same depth
     */
    public boolean areNetsAtSameDepth() {
        ShoalDepth port = getPortNetDepth();
        ShoalDepth starboard = getStarboardNetDepth();
        return port != null && port == starboard;
    }

    /**
     * Check if both nets are at the specified depth
     */
    public boolean areNetsAtDepth(ShoalDepth targetDepth) {
        return getPortNetDepth() == targetDepth && getStarboardNetDepth() == targetDepth;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged e) {
        int varbitId = e.getVarbitId();
        
        if (varbitId == TRAWLING_NET_PORT_VARBIT) {
            ShoalDepth oldDepth = portNetDepth;
            portNetDepth = getNetDepthFromVarbit(TRAWLING_NET_PORT_VARBIT);
            log.debug("Port net depth changed: {} -> {} (varbit: {}, value: {})", 
                     oldDepth, portNetDepth, varbitId, e.getValue());
        } else if (varbitId == TRAWLING_NET_STARBOARD_VARBIT) {
            ShoalDepth oldDepth = starboardNetDepth;
            starboardNetDepth = getNetDepthFromVarbit(TRAWLING_NET_STARBOARD_VARBIT);
            log.debug("Starboard net depth changed: {} -> {} (varbit: {}, value: {})", 
                     oldDepth, starboardNetDepth, varbitId, e.getValue());
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
        log.debug("Updated cached net depths - Port: {}, Starboard: {}", portNetDepth, starboardNetDepth);
    }

    /**
     * Force refresh of cached values (useful for debugging or when cache might be stale)
     */
    public void refreshCache() {
        log.debug("Force refreshing net depth cache");
        portNetDepth = null;
        starboardNetDepth = null;
        updateCachedValues();
    }
}
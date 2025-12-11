package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
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
    // Net 0 = Port, Net 1 = Starboard
    private static final int TRAWLING_NET_PORT_VARBIT = VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_0_DEPTH;
    private static final int TRAWLING_NET_STARBOARD_VARBIT = VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_1_DEPTH;

    private final Client client;

    // Cached values for performance
    private NetDepth portNetDepth;
    private NetDepth starboardNetDepth;

    @Inject
    public NetDepthTracker(Client client) {
        this.client = client;
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        // Service component - always enabled
        return true;
    }

    @Override
    public void startUp() {
        log.debug("NetDepthTracker started");
        // Initialize cached values
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
    public NetDepth getPortNetDepth() {
        if (portNetDepth == null) {
            portNetDepth = getNetDepthFromVarbit(TRAWLING_NET_PORT_VARBIT);
        }
        return portNetDepth;
    }

    /**
     * Get the current starboard net depth
     */
    public NetDepth getStarboardNetDepth() {
        if (starboardNetDepth == null) {
            starboardNetDepth = getNetDepthFromVarbit(TRAWLING_NET_STARBOARD_VARBIT);
        }
        return starboardNetDepth;
    }

    /**
     * Check if both nets are at the same depth
     */
    public boolean areNetsAtSameDepth() {
        NetDepth port = getPortNetDepth();
        NetDepth starboard = getStarboardNetDepth();
        return port != null && port == starboard;
    }

    /**
     * Check if both nets are at the specified depth
     */
    public boolean areNetsAtDepth(NetDepth targetDepth) {
        return getPortNetDepth() == targetDepth && getStarboardNetDepth() == targetDepth;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged e) {
        int varbitId = e.getVarbitId();
        
        if (varbitId == TRAWLING_NET_PORT_VARBIT) {
            NetDepth oldDepth = portNetDepth;
            portNetDepth = getNetDepthFromVarbit(TRAWLING_NET_PORT_VARBIT);
            log.debug("Port net depth changed: {} -> {}", oldDepth, portNetDepth);
        } else if (varbitId == TRAWLING_NET_STARBOARD_VARBIT) {
            NetDepth oldDepth = starboardNetDepth;
            starboardNetDepth = getNetDepthFromVarbit(TRAWLING_NET_STARBOARD_VARBIT);
            log.debug("Starboard net depth changed: {} -> {}", oldDepth, starboardNetDepth);
        }
    }

    /**
     * Convert varbit value to NetDepth enum
     */
    private NetDepth getNetDepthFromVarbit(int varbitId) {
        int varbitValue = client.getVarbitValue(varbitId);
        
        // Convert varbit value to NetDepth (0=net not lowered, 1=shallow, 2=moderate, 3=deep)
        switch (varbitValue) {
            case 0:
                return null; // Net not lowered
            case 1:
                return NetDepth.SHALLOW;
            case 2:
                return NetDepth.MODERATE;
            case 3:
                return NetDepth.DEEP;
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
}
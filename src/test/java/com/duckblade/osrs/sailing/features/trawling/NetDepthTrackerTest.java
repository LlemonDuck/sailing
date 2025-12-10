package com.duckblade.osrs.sailing.features.trawling;

import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for NetDepthTracker
 */
public class NetDepthTrackerTest {

    @Mock
    private Client client;
    
    @Mock
    private VarbitChanged varbitChanged;
    
    private NetDepthTracker tracker;
    
    // Test varbit IDs (using the real RuneLite API constants)
    private static final int TRAWLING_NET_PORT_VARBIT = 19206; // VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_0_DEPTH
    private static final int TRAWLING_NET_STARBOARD_VARBIT = 19208; // VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_1_DEPTH

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tracker = new NetDepthTracker(client);
    }

    @Test
    public void testGetPortNetDepth_shallow() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(0);
        
        NetDepth result = tracker.getPortNetDepth();
        
        assertEquals(NetDepth.SHALLOW, result);
    }

    @Test
    public void testGetPortNetDepth_moderate() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        
        NetDepth result = tracker.getPortNetDepth();
        
        assertEquals(NetDepth.MODERATE, result);
    }

    @Test
    public void testGetPortNetDepth_deep() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(2);
        
        NetDepth result = tracker.getPortNetDepth();
        
        assertEquals(NetDepth.DEEP, result);
    }

    @Test
    public void testGetStarboardNetDepth_shallow() {
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(0);
        
        NetDepth result = tracker.getStarboardNetDepth();
        
        assertEquals(NetDepth.SHALLOW, result);
    }

    @Test
    public void testGetStarboardNetDepth_moderate() {
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(1);
        
        NetDepth result = tracker.getStarboardNetDepth();
        
        assertEquals(NetDepth.MODERATE, result);
    }

    @Test
    public void testGetStarboardNetDepth_deep() {
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        
        NetDepth result = tracker.getStarboardNetDepth();
        
        assertEquals(NetDepth.DEEP, result);
    }

    @Test
    public void testAreNetsAtSameDepth_true() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(1);
        
        boolean result = tracker.areNetsAtSameDepth();
        
        assertTrue(result);
    }

    @Test
    public void testAreNetsAtSameDepth_false() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(0);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        
        boolean result = tracker.areNetsAtSameDepth();
        
        assertFalse(result);
    }

    @Test
    public void testAreNetsAtDepth_true() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(1);
        
        boolean result = tracker.areNetsAtDepth(NetDepth.MODERATE);
        
        assertTrue(result);
    }

    @Test
    public void testAreNetsAtDepth_false_portDifferent() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(0);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(1);
        
        boolean result = tracker.areNetsAtDepth(NetDepth.MODERATE);
        
        assertFalse(result);
    }

    @Test
    public void testAreNetsAtDepth_false_starboardDifferent() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        
        boolean result = tracker.areNetsAtDepth(NetDepth.MODERATE);
        
        assertFalse(result);
    }

    @Test
    public void testOnVarbitChanged_portNet() {
        // Setup initial state
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(0);
        tracker.startUp(); // Initialize cached values
        
        // Change port net depth
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(2);
        when(varbitChanged.getVarbitId()).thenReturn(TRAWLING_NET_PORT_VARBIT);
        when(varbitChanged.getValue()).thenReturn(2);
        
        tracker.onVarbitChanged(varbitChanged);
        
        assertEquals(NetDepth.DEEP, tracker.getPortNetDepth());
    }

    @Test
    public void testOnVarbitChanged_starboardNet() {
        // Setup initial state
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(1);
        tracker.startUp(); // Initialize cached values
        
        // Change starboard net depth
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(0);
        when(varbitChanged.getVarbitId()).thenReturn(TRAWLING_NET_STARBOARD_VARBIT);
        when(varbitChanged.getValue()).thenReturn(0);
        
        tracker.onVarbitChanged(varbitChanged);
        
        assertEquals(NetDepth.SHALLOW, tracker.getStarboardNetDepth());
    }

    @Test
    public void testOnVarbitChanged_unrelatedVarbit() {
        // Setup initial state
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(1);
        tracker.startUp(); // Initialize cached values
        
        // Trigger unrelated varbit change
        when(varbitChanged.getVarbitId()).thenReturn(99999);
        when(varbitChanged.getValue()).thenReturn(5);
        
        tracker.onVarbitChanged(varbitChanged);
        
        // Values should remain unchanged
        assertEquals(NetDepth.MODERATE, tracker.getPortNetDepth());
        assertEquals(NetDepth.MODERATE, tracker.getStarboardNetDepth());
    }

    @Test
    public void testInvalidVarbitValue() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(99);
        
        NetDepth result = tracker.getPortNetDepth();
        
        assertNull(result);
    }

    @Test
    public void testShutDown() {
        // Setup some state
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        tracker.startUp();
        
        // Verify state is set
        assertEquals(NetDepth.MODERATE, tracker.getPortNetDepth());
        assertEquals(NetDepth.DEEP, tracker.getStarboardNetDepth());
        
        // Shut down
        tracker.shutDown();
        
        // After shutdown, should return fresh values from client (not cached)
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(0);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(0);
        
        assertEquals(NetDepth.SHALLOW, tracker.getPortNetDepth());
        assertEquals(NetDepth.SHALLOW, tracker.getStarboardNetDepth());
    }
}
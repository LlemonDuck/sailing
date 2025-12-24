package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.model.ShoalDepth;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for NetDepthTracker
 */
public class NetDepthTrackerTest {

    @Mock
    private Client client;
    
    @Mock
    private VarbitChanged varbitChanged;
    
    private NetDepthTracker tracker;
    
    private static final int TRAWLING_NET_PORT_VARBIT = 19208; // VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_1_DEPTH
    private static final int TRAWLING_NET_STARBOARD_VARBIT = 19206; // VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_0_DEPTH

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tracker = new NetDepthTracker(client);
    }

    @Test
    public void testGetPortNetDepth_shallow() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        
        ShoalDepth result = tracker.getPortNetDepth();
        
        assertEquals(ShoalDepth.SHALLOW, result);
    }

    @Test
    public void testGetPortNetDepth_moderate() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(2);
        
        ShoalDepth result = tracker.getPortNetDepth();
        
        assertEquals(ShoalDepth.MODERATE, result);
    }

    @Test
    public void testGetPortNetDepth_deep() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(3);
        
        ShoalDepth result = tracker.getPortNetDepth();
        
        assertEquals(ShoalDepth.DEEP, result);
    }

    @Test
    public void testGetStarboardNetDepth_shallow() {
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(1);
        
        ShoalDepth result = tracker.getStarboardNetDepth();
        
        assertEquals(ShoalDepth.SHALLOW, result);
    }

    @Test
    public void testGetStarboardNetDepth_moderate() {
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        
        ShoalDepth result = tracker.getStarboardNetDepth();
        
        assertEquals(ShoalDepth.MODERATE, result);
    }

    @Test
    public void testGetStarboardNetDepth_deep() {
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(3);
        
        ShoalDepth result = tracker.getStarboardNetDepth();
        
        assertEquals(ShoalDepth.DEEP, result);
    }

    @Test
    public void testAreNetsAtSameDepth_true() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(2);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        
        boolean result = tracker.areNetsAtSameDepth();
        
        assertTrue(result);
    }

    @Test
    public void testAreNetsAtSameDepth_false() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(3);
        
        boolean result = tracker.areNetsAtSameDepth();
        
        assertFalse(result);
    }

    @Test
    public void testAreNetsAtDepth_true() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(2);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        
        boolean result = tracker.areNetsAtDepth(ShoalDepth.MODERATE);
        
        assertTrue(result);
    }

    @Test
    public void testAreNetsAtDepth_false_portDifferent() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        
        boolean result = tracker.areNetsAtDepth(ShoalDepth.MODERATE);
        
        assertFalse(result);
    }

    @Test
    public void testAreNetsAtDepth_false_starboardDifferent() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(2);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(3);
        
        boolean result = tracker.areNetsAtDepth(ShoalDepth.MODERATE);
        
        assertFalse(result);
    }

    @Test
    public void testOnVarbitChanged_portNet() {
        // Setup initial state
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        tracker.startUp(); // Initialize cached values
        
        // Change port net depth
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(3);
        when(varbitChanged.getVarbitId()).thenReturn(TRAWLING_NET_PORT_VARBIT);
        when(varbitChanged.getValue()).thenReturn(3);
        
        tracker.onVarbitChanged(varbitChanged);
        
        assertEquals(ShoalDepth.DEEP, tracker.getPortNetDepth());
    }

    @Test
    public void testOnVarbitChanged_starboardNet() {
        // Setup initial state
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        tracker.startUp(); // Initialize cached values
        
        // Change starboard net depth
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(1);
        when(varbitChanged.getVarbitId()).thenReturn(TRAWLING_NET_STARBOARD_VARBIT);
        when(varbitChanged.getValue()).thenReturn(1);
        
        tracker.onVarbitChanged(varbitChanged);
        
        assertEquals(ShoalDepth.SHALLOW, tracker.getStarboardNetDepth());
    }

    @Test
    public void testOnVarbitChanged_unrelatedVarbit() {
        // Setup initial state
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(2);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(2);
        tracker.startUp(); // Initialize cached values
        
        // Trigger unrelated varbit change
        when(varbitChanged.getVarbitId()).thenReturn(99999);
        when(varbitChanged.getValue()).thenReturn(5);
        
        tracker.onVarbitChanged(varbitChanged);
        
        // Values should remain unchanged
        assertEquals(ShoalDepth.MODERATE, tracker.getPortNetDepth());
        assertEquals(ShoalDepth.MODERATE, tracker.getStarboardNetDepth());
    }

    @Test
    public void testInvalidVarbitValue() {
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(99);
        
        ShoalDepth result = tracker.getPortNetDepth();
        
        assertNull(result);
    }

    @Test
    public void testShutDown() {
        // Setup some state
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(2);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(3);
        tracker.startUp();
        
        // Verify state is set
        assertEquals(ShoalDepth.MODERATE, tracker.getPortNetDepth());
        assertEquals(ShoalDepth.DEEP, tracker.getStarboardNetDepth());
        
        // Shut down
        tracker.shutDown();
        
        // After shutdown, should return fresh values from client (not cached)
        when(client.getVarbitValue(TRAWLING_NET_PORT_VARBIT)).thenReturn(1);
        when(client.getVarbitValue(TRAWLING_NET_STARBOARD_VARBIT)).thenReturn(1);
        
        assertEquals(ShoalDepth.SHALLOW, tracker.getPortNetDepth());
        assertEquals(ShoalDepth.SHALLOW, tracker.getStarboardNetDepth());
    }
}
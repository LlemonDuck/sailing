package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.model.FishingNetTier;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ShoalOverlay color logic and depth matching
 */
public class ShoalOverlayTest {

    @Mock
    private Client client;
    
    @Mock
    private SailingConfig config;
    
    @Mock
    private ShoalDepthTracker shoalDepthTracker;
    
    @Mock
    private BoatTracker boatTracker;
    
    @Mock
    private Boat boat;
    
    @Mock
    private Widget facilitiesWidget;
    
    @Mock
    private Widget depthWidget;
    
    private ShoalOverlay overlay;

    // Sprite IDs for each depth level (from ShoalOverlay)
    private static final int SPRITE_SHALLOW = 7081;
    private static final int SPRITE_MODERATE = 7082;
    private static final int SPRITE_DEEP = 7083;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        overlay = new ShoalOverlay(client, config, shoalDepthTracker, boatTracker);
        
        // Setup default config color
        when(config.trawlingShoalHighlightColour()).thenReturn(Color.CYAN);
    }

    /**
     * **Feature: trawling-depth-tracking, Property 1: Depth mismatch shows red highlight**
     * **Validates: Requirements 1.1**
     * 
     * Property: For any combination of player net depth and shoal depth where they differ,
     * the ShoalOverlay should render the shoal highlight in red color, regardless of whether
     * the shoal is a special type (red has highest priority).
     */
    @Test
    public void testDepthMismatchShowsRedHighlight() throws Exception {
        // Test all combinations of different depths
        NetDepth[] allDepths = {NetDepth.SHALLOW, NetDepth.MODERATE, NetDepth.DEEP};
        int[] allShoalIds = {
            TrawlingData.ShoalObjectID.BLUEFIN,
            TrawlingData.ShoalObjectID.MARLIN,
            TrawlingData.ShoalObjectID.HALIBUT,
            TrawlingData.ShoalObjectID.YELLOWFIN,
            TrawlingData.ShoalObjectID.VIBRANT,    // Special shoal
            TrawlingData.ShoalObjectID.GLISTENING, // Special shoal
            TrawlingData.ShoalObjectID.SHIMMERING  // Special shoal
        };

        for (NetDepth shoalDepth : allDepths) {
            for (NetDepth playerDepth : allDepths) {
                // Skip matching depths (different property)
                if (shoalDepth == playerDepth) {
                    continue;
                }
                
                for (int shoalId : allShoalIds) {
                    // Setup mocks for this test case
                    when(shoalDepthTracker.getCurrentDepth()).thenReturn(shoalDepth);
                    setupPlayerNetDepth(playerDepth);
                    
                    // Get the color using reflection to access private method
                    Color color = getShoalColorViaReflection(shoalId);
                    
                    // Verify the property: mismatched depths should always return red
                    assertEquals("Shoal ID " + shoalId + " with shoal depth " + shoalDepth + 
                               " and player depth " + playerDepth + " should show red highlight",
                               Color.RED, color);
                }
            }
        }
    }

    /**
     * Helper method to setup player net depth mocking
     */
    private void setupPlayerNetDepth(NetDepth depth) {
        // Mock boat with nets
        when(boatTracker.getBoat()).thenReturn(boat);
        when(boat.getNetTiers()).thenReturn(Arrays.asList(FishingNetTier.ROPE)); // Non-empty list
        
        // Mock facilities widget
        when(client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS)).thenReturn(facilitiesWidget);
        when(facilitiesWidget.getChild(96)).thenReturn(depthWidget); // Starboard depth widget
        
        // Set sprite based on depth
        int spriteId;
        switch (depth) {
            case SHALLOW:
                spriteId = SPRITE_SHALLOW;
                break;
            case MODERATE:
                spriteId = SPRITE_MODERATE;
                break;
            case DEEP:
                spriteId = SPRITE_DEEP;
                break;
            default:
                throw new IllegalArgumentException("Unknown depth: " + depth);
        }
        when(depthWidget.getSpriteId()).thenReturn(spriteId);
    }

    /**
     * Helper method to access private getShoalColor method via reflection
     */
    private Color getShoalColorViaReflection(int objectId) throws Exception {
        Method getShoalColorMethod = ShoalOverlay.class.getDeclaredMethod("getShoalColor", int.class);
        getShoalColorMethod.setAccessible(true);
        return (Color) getShoalColorMethod.invoke(overlay, objectId);
    }

    /**
     * Helper method to setup no player net depth (null case)
     */
    private void setupNoPlayerNetDepth() {
        // Mock boat with no nets
        when(boatTracker.getBoat()).thenReturn(boat);
        when(boat.getNetTiers()).thenReturn(Collections.emptyList());
    }

    /**
     * Helper method to setup no facilities widget (null case)
     */
    private void setupNoFacilitiesWidget() {
        when(boatTracker.getBoat()).thenReturn(boat);
        when(boat.getNetTiers()).thenReturn(Arrays.asList(FishingNetTier.ROPE));
        when(client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS)).thenReturn(null);
    }

    /**
     * **Feature: trawling-depth-tracking, Property 2: Depth match shows configured color for normal shoals**
     * **Validates: Requirements 1.2**
     * 
     * Property: For any combination of player net depth and shoal depth where they match,
     * and the shoal is not a special type (VIBRANT, GLISTENING, SHIMMERING), the ShoalOverlay
     * should render the shoal highlight using the configured color from settings.
     */
    @Test
    public void testDepthMatchShowsConfiguredColorForNormalShoals() throws Exception {
        // Test all depth combinations where they match
        NetDepth[] allDepths = {NetDepth.SHALLOW, NetDepth.MODERATE, NetDepth.DEEP};
        
        // Normal (non-special) shoal IDs
        int[] normalShoalIds = {
            TrawlingData.ShoalObjectID.BLUEFIN,
            TrawlingData.ShoalObjectID.MARLIN,
            TrawlingData.ShoalObjectID.HALIBUT,
            TrawlingData.ShoalObjectID.YELLOWFIN,
            TrawlingData.ShoalObjectID.HADDOCK,
            TrawlingData.ShoalObjectID.GIANT_KRILL
        };

        // Test different configured colors
        Color[] testColors = {Color.CYAN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.ORANGE};

        for (Color configuredColor : testColors) {
            // Set the configured color
            when(config.trawlingShoalHighlightColour()).thenReturn(configuredColor);
            
            for (NetDepth depth : allDepths) {
                for (int shoalId : normalShoalIds) {
                    // Setup mocks for matching depths
                    when(shoalDepthTracker.getCurrentDepth()).thenReturn(depth);
                    setupPlayerNetDepth(depth); // Same depth = matching
                    
                    // Get the color using reflection
                    Color color = getShoalColorViaReflection(shoalId);
                    
                    // Verify the property: matching depths on normal shoals should use configured color
                    assertEquals("Normal shoal ID " + shoalId + " with matching depth " + depth + 
                               " should use configured color " + configuredColor,
                               configuredColor, color);
                }
            }
        }
    }

    /**
     * **Feature: trawling-depth-tracking, Property 3: Special shoals use green when depth matches**
     * **Validates: Requirements 1.2**
     * 
     * Property: For any special shoal (VIBRANT, GLISTENING, SHIMMERING), when the player net depth
     * matches the shoal depth, the ShoalOverlay should render the shoal highlight in green color.
     */
    @Test
    public void testSpecialShoalsUseGreenWhenDepthMatches() throws Exception {
        // Special shoal IDs
        int[] specialShoalIds = {
            TrawlingData.ShoalObjectID.VIBRANT,
            TrawlingData.ShoalObjectID.GLISTENING,
            TrawlingData.ShoalObjectID.SHIMMERING
        };

        // Test all depth combinations where they match
        NetDepth[] allDepths = {NetDepth.SHALLOW, NetDepth.MODERATE, NetDepth.DEEP};

        // Test different configured colors to ensure green overrides them
        Color[] testColors = {Color.CYAN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.ORANGE};

        for (Color configuredColor : testColors) {
            // Set the configured color (should be overridden by green for special shoals)
            when(config.trawlingShoalHighlightColour()).thenReturn(configuredColor);
            
            for (NetDepth depth : allDepths) {
                for (int shoalId : specialShoalIds) {
                    // Setup mocks for matching depths
                    when(shoalDepthTracker.getCurrentDepth()).thenReturn(depth);
                    setupPlayerNetDepth(depth); // Same depth = matching
                    
                    // Get the color using reflection
                    Color color = getShoalColorViaReflection(shoalId);
                    
                    // Verify the property: special shoals with matching depths should use green
                    assertEquals("Special shoal ID " + shoalId + " with matching depth " + depth + 
                               " should use green color regardless of configured color " + configuredColor,
                               Color.GREEN, color);
                }
            }
        }
    }

    /**
     * **Feature: trawling-depth-tracking, Property 4: Depth change updates color within one tick**
     * **Validates: Requirements 1.3**
     * 
     * Property: For any shoal depth change event, the next render call should use the color
     * corresponding to the new depth matching state.
     */
    @Test
    public void testDepthChangeUpdatesColorWithinOneTick() throws Exception {
        // Test different shoal types
        int[] shoalIds = {
            TrawlingData.ShoalObjectID.BLUEFIN,
            TrawlingData.ShoalObjectID.VIBRANT, // Special shoal
            TrawlingData.ShoalObjectID.HALIBUT
        };

        // Test depth transitions
        NetDepth[] allDepths = {NetDepth.SHALLOW, NetDepth.MODERATE, NetDepth.DEEP};
        
        // Fixed player depth for this test
        NetDepth playerDepth = NetDepth.MODERATE;

        for (int shoalId : shoalIds) {
            for (NetDepth initialShoalDepth : allDepths) {
                for (NetDepth newShoalDepth : allDepths) {
                    // Skip same depth (no change)
                    if (initialShoalDepth == newShoalDepth) {
                        continue;
                    }

                    // Setup player depth
                    setupPlayerNetDepth(playerDepth);

                    // Setup initial shoal depth
                    when(shoalDepthTracker.getCurrentDepth()).thenReturn(initialShoalDepth);
                    
                    // Get initial color
                    Color initialColor = getShoalColorViaReflection(shoalId);
                    
                    // Change shoal depth (simulating depth change event)
                    when(shoalDepthTracker.getCurrentDepth()).thenReturn(newShoalDepth);
                    
                    // Get new color (this simulates the next render call)
                    Color newColor = getShoalColorViaReflection(shoalId);
                    
                    // Determine expected colors based on depth matching
                    Color expectedInitialColor = getExpectedColor(shoalId, initialShoalDepth, playerDepth);
                    Color expectedNewColor = getExpectedColor(shoalId, newShoalDepth, playerDepth);
                    
                    // Verify the property: color should reflect current depth state
                    assertEquals("Initial color for shoal " + shoalId + " with shoal depth " + initialShoalDepth + 
                               " and player depth " + playerDepth + " should be correct",
                               expectedInitialColor, initialColor);
                    
                    assertEquals("New color for shoal " + shoalId + " after depth change to " + newShoalDepth + 
                               " with player depth " + playerDepth + " should be updated immediately",
                               expectedNewColor, newColor);
                    
                    // If depths changed from matching to non-matching or vice versa, colors should be different
                    boolean initialMatching = (initialShoalDepth == playerDepth);
                    boolean newMatching = (newShoalDepth == playerDepth);
                    
                    if (initialMatching != newMatching) {
                        assertNotEquals("Color should change when depth matching state changes for shoal " + shoalId,
                                       initialColor, newColor);
                    }
                }
            }
        }
    }

    /**
     * Helper method to determine expected color based on shoal type and depth matching
     */
    private Color getExpectedColor(int shoalId, NetDepth shoalDepth, NetDepth playerDepth) {
        // Priority 1: Depth mismatch = red
        if (shoalDepth != playerDepth) {
            return Color.RED;
        }
        
        // Priority 2: Special shoals = green (when depths match)
        if (shoalId == TrawlingData.ShoalObjectID.VIBRANT ||
            shoalId == TrawlingData.ShoalObjectID.GLISTENING ||
            shoalId == TrawlingData.ShoalObjectID.SHIMMERING) {
            return Color.GREEN;
        }
        
        // Priority 3: Normal shoals = configured color (when depths match)
        return Color.CYAN; // Default configured color from setUp()
    }
}
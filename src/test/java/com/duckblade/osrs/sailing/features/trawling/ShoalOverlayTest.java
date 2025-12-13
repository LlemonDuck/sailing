package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import net.runelite.api.Client;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.Color;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests for ShoalOverlay color logic (depth matching disabled)
 */
public class ShoalOverlayTest {

    @Mock
    private Client client;
    
    @Mock
    private SailingConfig config;
    
    @Mock
    private ShoalTracker shoalTracker;
    
    private ShoalOverlay overlay;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        overlay = new ShoalOverlay(client, config, shoalTracker);
        
        // Setup default config color
        when(config.trawlingShoalHighlightColour()).thenReturn(Color.CYAN);
    }

    /**
     * Test that special shoals use green color
     */
    @Test
    public void testSpecialShoalsUseGreenColor() throws Exception {
        int[] specialShoalIds = {
            TrawlingData.ShoalObjectID.VIBRANT,
            TrawlingData.ShoalObjectID.GLISTENING,
            TrawlingData.ShoalObjectID.SHIMMERING
        };

        for (int shoalId : specialShoalIds) {
            Color color = getShoalColorViaReflection(shoalId);
            assertEquals("Special shoal ID " + shoalId + " should use green color",
                       Color.GREEN, color);
        }
    }

    /**
     * Test that normal shoals use configured color
     */
    @Test
    public void testNormalShoalsUseConfiguredColor() throws Exception {
        int[] normalShoalIds = {
            TrawlingData.ShoalObjectID.BLUEFIN,
            TrawlingData.ShoalObjectID.MARLIN,
            TrawlingData.ShoalObjectID.HALIBUT,
            TrawlingData.ShoalObjectID.YELLOWFIN
        };

        Color testColor = Color.MAGENTA;
        when(config.trawlingShoalHighlightColour()).thenReturn(testColor);

        for (int shoalId : normalShoalIds) {
            Color color = getShoalColorViaReflection(shoalId);
            assertEquals("Normal shoal ID " + shoalId + " should use configured color",
                       testColor, color);
        }
    }

    /**
     * Helper method to access private getShoalColor method via reflection
     */
    private Color getShoalColorViaReflection(int objectId) throws Exception {
        Method getShoalColorMethod = ShoalOverlay.class.getDeclaredMethod("getShoalColor", int.class);
        getShoalColorMethod.setAccessible(true);
        return (Color) getShoalColorMethod.invoke(overlay, objectId);
    }
}
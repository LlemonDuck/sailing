package com.duckblade.osrs.sailing.features.trawling;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.api.gameval.ObjectID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ShoalDepthTracker
 */
public class ShoalDepthTrackerTest {

    @Mock
    private Client client;
    
    @Mock
    private GameObject gameObject;
    
    @Mock
    private WorldEntity worldEntity;
    
    @Mock
    private net.runelite.api.WorldEntityConfig worldEntityConfig;
    
    @Mock
    private ChatMessage chatMessage;
    
    private ShoalDepthTracker tracker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tracker = new ShoalDepthTracker(client);
    }

    /**
     * **Feature: trawling-depth-tracking, Property 5: Shoal spawn initializes correct depth**
     * **Validates: Requirements 2.1**
     * 
     * Property: For any fishing area, when a shoal spawns in that area, 
     * the ShoalDepthTracker should initialize with the starting depth appropriate 
     * for that area's depth pattern.
     */
    @Test
    public void testShoalSpawnInitializesCorrectDepth() {
        // Test data: different fishing areas and their expected starting depths
        TestCase[] testCases = {
            // Marlin areas: start at MODERATE
            new TestCase(2570, 3880, TrawlingData.ShoalStopDuration.MARLIN, NetDepth.MODERATE, true),
            new TestCase(2700, 4000, TrawlingData.ShoalStopDuration.MARLIN, NetDepth.MODERATE, true),
            
            // Bluefin areas: start at SHALLOW
            // RAINBOW_REEF: (2075, 2406, 2179, 2450) - use coordinates clearly within this area
            new TestCase(2200, 2300, TrawlingData.ShoalStopDuration.BLUEFIN, NetDepth.SHALLOW, true),
            // BUCCANEERS_HAVEN: (1984, 2268, 3594, 3771) - use coordinates clearly within this area  
            new TestCase(2100, 3650, TrawlingData.ShoalStopDuration.BLUEFIN, NetDepth.SHALLOW, true),
            
            // Halibut areas: start at SHALLOW
            // PORT_ROBERTS: (1822, 2050, 3129, 3414) - use coordinates clearly within this area
            new TestCase(1900, 3200, TrawlingData.ShoalStopDuration.HALIBUT, NetDepth.SHALLOW, false),
            // SOUTHERN_EXPANSE: (1870, 2180, 2171, 2512) - use coordinates clearly within this area
            new TestCase(1950, 2300, TrawlingData.ShoalStopDuration.HALIBUT, NetDepth.SHALLOW, false),
        };

        for (TestCase testCase : testCases) {
            // Reset tracker state
            tracker.shutDown();
            
            // Setup mocks for this test case
            WorldPoint location = new WorldPoint(testCase.x, testCase.y, 0);
            LocalPoint localPoint = new LocalPoint(testCase.x * 128, testCase.y * 128);
            
            when(worldEntity.getConfig()).thenReturn(worldEntityConfig);
            when(worldEntityConfig.getId()).thenReturn(4); // SHOAL_WORLD_ENTITY_CONFIG_ID
            when(worldEntity.getCameraFocus()).thenReturn(localPoint);
            when(client.getTopLevelWorldView()).thenReturn(null); // Simplified for test
            
            // Mock WorldPoint.fromLocal to return our test location
            // Note: In a real test, we'd need to mock this static method properly
            // For this property test, we'll simulate the behavior
            
            // Create event and trigger spawn
            WorldEntitySpawned event = new WorldEntitySpawned(worldEntity);
            
            // Simulate the initialization directly since we can't easily mock static methods
            simulateWorldEntitySpawn(location);
            
            // Verify the property: correct depth initialization
            assertEquals("Shoal at " + location + " should initialize with correct depth",
                        testCase.expectedDepth, tracker.getCurrentDepth());
            
            // Verify three-depth area flag
            assertEquals("Shoal at " + location + " should have correct three-depth area flag",
                        testCase.expectedThreeDepthArea, tracker.isThreeDepthArea());
            
            // Verify movement direction is reset
            assertEquals("Movement direction should be UNKNOWN on spawn",
                        MovementDirection.UNKNOWN, tracker.getNextMovementDirection());
        }
    }

    /**
     * **Feature: trawling-depth-tracking, Property 7: Despawn clears state**
     * **Validates: Requirements 2.3**
     * 
     * Property: For any active shoal, when a despawn event occurs, 
     * the ShoalDepthTracker should return null for all depth queries.
     */
    @Test
    public void testDespawnClearsState() {
        // Test with different shoal types to ensure property holds for all
        int[] shoalIds = {
            TrawlingData.ShoalObjectID.MARLIN,
            TrawlingData.ShoalObjectID.BLUEFIN,
            TrawlingData.ShoalObjectID.HALIBUT,
            TrawlingData.ShoalObjectID.YELLOWFIN,
            TrawlingData.ShoalObjectID.VIBRANT,
            TrawlingData.ShoalObjectID.GLISTENING,
            TrawlingData.ShoalObjectID.SHIMMERING
        };

        for (int shoalId : shoalIds) {
            // First, initialize tracker with some state
            WorldPoint testLocation = new WorldPoint(2075, 2179, 0); // Bluefin area
            simulateWorldEntitySpawn(testLocation);
            
            // Verify state is initialized
            assertNotNull("Tracker should have state before despawn", tracker.getCurrentDepth());
            
            // Setup despawn event
            when(gameObject.getId()).thenReturn(shoalId);
            GameObjectDespawned despawnEvent = mock(GameObjectDespawned.class);
            when(despawnEvent.getGameObject()).thenReturn(gameObject);
            
            // Trigger despawn
            tracker.onGameObjectDespawned(despawnEvent);
            
            // Verify the property: all state should be cleared
            assertNull("Current depth should be null after despawn for shoal ID " + shoalId,
                      tracker.getCurrentDepth());
            assertFalse("Three-depth area flag should be false after despawn for shoal ID " + shoalId,
                       tracker.isThreeDepthArea());
            assertEquals("Movement direction should be UNKNOWN after despawn for shoal ID " + shoalId,
                        MovementDirection.UNKNOWN, tracker.getNextMovementDirection());
        }
    }

    /**
     * **Feature: trawling-depth-tracking, Property 6: Depth state updates on timing transitions**
     * **Validates: Requirements 2.2**
     * 
     * Property: For any active shoal with a depth transition pattern, when the transition tick is reached,
     * the ShoalDepthTracker should update its tracked depth to the new depth.
     */
    @Test
    public void testDepthStateUpdatesOnTimingTransitions() {
        // Test different depth transitions that can occur
        NetDepth[] startDepths = {NetDepth.SHALLOW, NetDepth.MODERATE, NetDepth.DEEP};
        NetDepth[] endDepths = {NetDepth.SHALLOW, NetDepth.MODERATE, NetDepth.DEEP};
        
        for (NetDepth startDepth : startDepths) {
            for (NetDepth endDepth : endDepths) {
                // Skip same depth transitions (no change expected)
                if (startDepth == endDepth) {
                    continue;
                }
                
                // Initialize tracker with some state
                WorldPoint testLocation = new WorldPoint(2075, 2179, 0); // Bluefin area
                simulateWorldEntitySpawn(testLocation);
                
                // Verify initial state
                assertNotNull("Tracker should have initial state", tracker.getCurrentDepth());
                
                // Simulate a depth change notification (this is how NetDepthTimer communicates transitions)
                tracker.notifyDepthChange(endDepth);
                
                // Verify the property: depth state should be updated
                assertEquals("Depth should be updated to new depth after transition from " + startDepth + " to " + endDepth,
                            endDepth, tracker.getCurrentDepth());
                
                // Reset for next iteration
                tracker.shutDown();
            }
        }
    }

    /**
     * **Feature: trawling-depth-tracking, Property 13: Transition clears movement direction**
     * **Validates: Requirements 4.3**
     * 
     * Property: For any recorded movement direction, when a depth transition completes,
     * the ShoalDepthTracker should clear the recorded direction.
     */
    @Test
    public void testTransitionClearsMovementDirection() {
        // Test all possible movement directions
        MovementDirection[] directions = {MovementDirection.DEEPER, MovementDirection.SHALLOWER, MovementDirection.UNKNOWN};
        NetDepth[] transitionDepths = {NetDepth.SHALLOW, NetDepth.MODERATE, NetDepth.DEEP};
        
        for (MovementDirection initialDirection : directions) {
            for (NetDepth transitionDepth : transitionDepths) {
                // Initialize tracker with some state
                WorldPoint testLocation = new WorldPoint(2075, 2179, 0); // Bluefin area (three-depth)
                simulateWorldEntitySpawn(testLocation);
                
                // Simulate setting a movement direction (this would normally happen via chat message parsing)
                // We'll use reflection or a test helper to set this state
                setMovementDirectionForTesting(initialDirection);
                
                // Verify movement direction is set
                assertEquals("Movement direction should be set before transition",
                            initialDirection, tracker.getNextMovementDirection());
                
                // Simulate a depth transition
                tracker.notifyDepthChange(transitionDepth);
                
                // Verify the property: movement direction should be cleared
                assertEquals("Movement direction should be UNKNOWN after depth transition with initial direction " + initialDirection,
                            MovementDirection.UNKNOWN, tracker.getNextMovementDirection());
                
                // Reset for next iteration
                tracker.shutDown();
            }
        }
    }

    /**
     * **Feature: trawling-depth-tracking, Property 11: Chat message sets movement direction for deep**
     * **Validates: Requirements 4.1**
     * 
     * Property: For any chat message containing text indicating the shoal moved deeper,
     * the ShoalDepthTracker should record the next depth transition as moderate-to-deep.
     */
    @Test
    public void testChatMessageSetsMovementDirectionForDeep() {
        // Test various messages that should indicate "deeper" movement
        String[] deeperMessages = {
            "The shoal moves deeper into the water",
            "Fish swim deeper below the surface", 
            "The school dives deeper",
            "Moving deeper underwater",
            "DEEPER waters ahead",
            "deeper",
            "The fish go DEEPER into the depths"
        };

        for (String message : deeperMessages) {
            // Initialize tracker in a three-depth area (required for chat message processing)
            WorldPoint bluefinLocation = new WorldPoint(2200, 2300, 0); // Bluefin area
            simulateWorldEntitySpawn(bluefinLocation);
            
            // Verify we're in a three-depth area
            assertTrue("Should be in three-depth area for test", tracker.isThreeDepthArea());
            
            // Verify initial movement direction is UNKNOWN
            assertEquals("Initial movement direction should be UNKNOWN", 
                        MovementDirection.UNKNOWN, tracker.getNextMovementDirection());
            
            // Setup chat message mock
            when(chatMessage.getType()).thenReturn(ChatMessageType.GAMEMESSAGE);
            when(chatMessage.getMessage()).thenReturn(message);
            
            // Process the chat message
            tracker.onChatMessage(chatMessage);
            
            // Verify the property: movement direction should be set to DEEPER
            assertEquals("Chat message '" + message + "' should set movement direction to DEEPER",
                        MovementDirection.DEEPER, tracker.getNextMovementDirection());
            
            // Reset for next iteration
            tracker.shutDown();
        }
    }

    /**
     * **Feature: trawling-depth-tracking, Property 12: Chat message sets movement direction for shallow**
     * **Validates: Requirements 4.2**
     * 
     * Property: For any chat message containing text indicating the shoal moved shallower,
     * the ShoalDepthTracker should record the next depth transition as moderate-to-shallow.
     */
    @Test
    public void testChatMessageSetsMovementDirectionForShallow() {
        // Test various messages that should indicate "shallower" movement
        String[] shallowerMessages = {
            "The shoal moves to shallower waters",
            "Fish swim toward shallower areas",
            "The school rises to shallower depths",
            "Moving to shallower water",
            "SHALLOWER regions nearby",
            "shallower",
            "The fish head to SHALLOWER waters"
        };

        for (String message : shallowerMessages) {
            // Initialize tracker in a three-depth area (required for chat message processing)
            WorldPoint bluefinLocation = new WorldPoint(2200, 2300, 0); // Bluefin area
            simulateWorldEntitySpawn(bluefinLocation);
            
            // Verify we're in a three-depth area
            assertTrue("Should be in three-depth area for test", tracker.isThreeDepthArea());
            
            // Verify initial movement direction is UNKNOWN
            assertEquals("Initial movement direction should be UNKNOWN", 
                        MovementDirection.UNKNOWN, tracker.getNextMovementDirection());
            
            // Setup chat message mock
            when(chatMessage.getType()).thenReturn(ChatMessageType.GAMEMESSAGE);
            when(chatMessage.getMessage()).thenReturn(message);
            
            // Process the chat message
            tracker.onChatMessage(chatMessage);
            
            // Verify the property: movement direction should be set to SHALLOWER
            assertEquals("Chat message '" + message + "' should set movement direction to SHALLOWER",
                        MovementDirection.SHALLOWER, tracker.getNextMovementDirection());
            
            // Reset for next iteration
            tracker.shutDown();
        }
    }

    /**
     * **Feature: trawling-depth-tracking, Property 14: Latest chat message wins**
     * **Validates: Requirements 4.4**
     * 
     * Property: For any sequence of chat messages indicating movement direction,
     * the ShoalDepthTracker should use only the most recent message's direction.
     */
    @Test
    public void testLatestChatMessageWins() {
        // Test sequences of messages where the last one should win
        MessageSequence[] sequences = {
            // Deeper then shallower - shallower should win
            new MessageSequence(
                new String[]{"The shoal moves deeper", "Fish swim to shallower waters"},
                MovementDirection.SHALLOWER
            ),
            // Shallower then deeper - deeper should win
            new MessageSequence(
                new String[]{"Moving to shallower water", "The school dives deeper"},
                MovementDirection.DEEPER
            ),
            // Multiple deeper messages - still deeper
            new MessageSequence(
                new String[]{"deeper waters", "even deeper", "going deeper still"},
                MovementDirection.DEEPER
            ),
            // Multiple shallower messages - still shallower
            new MessageSequence(
                new String[]{"shallower areas", "more shallower", "very shallower"},
                MovementDirection.SHALLOWER
            ),
            // Mixed sequence ending with deeper
            new MessageSequence(
                new String[]{"shallower", "deeper", "shallower", "deeper"},
                MovementDirection.DEEPER
            )
        };

        for (int i = 0; i < sequences.length; i++) {
            MessageSequence sequence = sequences[i];
            
            // Initialize tracker in a three-depth area
            WorldPoint bluefinLocation = new WorldPoint(2200, 2300, 0); // Bluefin area
            simulateWorldEntitySpawn(bluefinLocation);
            
            // Verify we're in a three-depth area
            assertTrue("Should be in three-depth area for test", tracker.isThreeDepthArea());
            
            // Process each message in the sequence
            for (String message : sequence.messages) {
                when(chatMessage.getType()).thenReturn(ChatMessageType.GAMEMESSAGE);
                when(chatMessage.getMessage()).thenReturn(message);
                tracker.onChatMessage(chatMessage);
            }
            
            // Verify the property: only the last message's direction should be stored
            assertEquals("Sequence " + i + " should result in direction from last message",
                        sequence.expectedFinalDirection, tracker.getNextMovementDirection());
            
            // Reset for next iteration
            tracker.shutDown();
        }
    }

    // Helper method to simulate world entity spawn without mocking static methods
    private void simulateWorldEntitySpawn(WorldPoint location) {
        // Directly call the initialization logic that would happen in onWorldEntitySpawned
        // This simulates the behavior without complex mocking
        tracker.initializeShoalStateForTesting(location);
    }
    
    // Helper method to set movement direction for testing
    private void setMovementDirectionForTesting(MovementDirection direction) {
        tracker.setMovementDirectionForTesting(direction);
    }

    // Test case data structure
    private static class TestCase {
        final int x, y;
        final int expectedStopDuration;
        final NetDepth expectedDepth;
        final boolean expectedThreeDepthArea;

        TestCase(int x, int y, int expectedStopDuration, NetDepth expectedDepth, boolean expectedThreeDepthArea) {
            this.x = x;
            this.y = y;
            this.expectedStopDuration = expectedStopDuration;
            this.expectedDepth = expectedDepth;
            this.expectedThreeDepthArea = expectedThreeDepthArea;
        }
    }

    // Message sequence test data structure
    private static class MessageSequence {
        final String[] messages;
        final MovementDirection expectedFinalDirection;

        MessageSequence(String[] messages, MovementDirection expectedFinalDirection) {
            this.messages = messages;
            this.expectedFinalDirection = expectedFinalDirection;
        }
    }
}
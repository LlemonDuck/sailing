# RuneLite Sailing Plugin - Trawling System Documentation

## Overview

The trawling system is a comprehensive feature that assists players with deep-sea trawling in Old School RuneScape's Sailing skill. It provides intelligent depth tracking, visual overlays, button highlighting, and timing assistance to optimize fishing efficiency.

## System Architecture

### Core Components

#### 1. Data Layer (`TrawlingData.java`)
- **Purpose**: Central repository for all trawling-related constants and data
- **Key Features**:
  - Shoal object IDs for all fish types (Marlin, Bluefin, Halibut, etc.)
  - Stop durations for different shoal types (50-100 ticks)
  - Fishing area definitions with coordinate boundaries
  - Area type classification (two-depth vs three-depth areas)

#### 2. Enums and Data Structures
- **`NetDepth`**: Represents net depth levels (SHALLOW=1, MODERATE=2, DEEP=3)
- **`MovementDirection`**: Legacy enum for shoal movement (SHALLOWER, DEEPER, UNKNOWN) - deprecated in current implementation
- **`FishingAreaType`**: Classifies areas (TWO_DEPTH, THREE_DEPTH) - used by other components but not by ShoalDepthTracker

### Tracking Components

#### 3. Net Depth Tracker (`NetDepthTracker.java`)
- **Purpose**: Real-time monitoring of both fishing nets using game varbits
- **Key Features**:
  - Tracks port net (varbit 19206) and starboard net (varbit 19208)
  - Caches depth values for performance
  - Provides utility methods for depth comparison
  - Automatically updates on varbit changes

#### 4. Shoal Depth Tracker (`ShoalDepthTracker.java`)
- **Purpose**: Tracks the current depth state of active shoals based entirely on chat messages
- **Key Features**:
  - Activates tracking when shoals are detected via GameObject events
  - Processes definitive depth confirmations ("correct depth for the nearby")
  - Tracks shoal movement messages ("closer to the surface", "shoal swims deeper into")
  - Logs informational feedback messages ("Your net is not deep enough", "Your net is too deep", "your net is too shallow")
  - Conservative approach: only sets depth when game explicitly confirms it
  - No longer relies on area-based initialization, timing predictions, or movement direction tracking
  - Deprecated methods: `isThreeDepthArea()` and `getNextMovementDirection()` for backward compatibility

#### 5. Net Depth Timer (`NetDepthTimer.java`)
- **Purpose**: Provides timing predictions for shoal depth transitions (informational only)
- **Key Features**:
  - Tracks shoal movement and stop patterns
  - Calculates depth change timing based on area type
  - Provides timing information for UI display
  - No longer directly updates shoal depth (handled by chat messages)
  - Handles different timing patterns per fishing area

#### 6. Shoal Path Tracker (`ShoalPathTracker.java`)
- **Purpose**: Development tool for tracing shoal movement routes
- **Key Features**:
  - Records waypoints and stop points
  - Exports path data for route analysis
  - Configurable via chat commands
  - Supports different shoal types

### User Interface Components

#### 7. Shoal Overlay (`ShoalOverlay.java`)
- **Purpose**: Visual highlighting of shoals in the game world
- **Key Features**:
  - Color-coded shoal highlighting based on depth matching
  - Red highlighting for incorrect depth
  - Green highlighting for special shoals (Vibrant, Glistening, Shimmering)
  - Configurable highlight colors

#### 8. Net Depth Button Highlighter (`NetDepthButtonHighlighter.java`)
- **Purpose**: Highlights UI buttons only when net depths need correction to match shoal depth
- **Key Features**:
  - Conditional highlighting: only shows when nets are at incorrect depth
  - Requires confirmed shoal depth (not estimated)
  - Highlights specific buttons (up/down) to correct each net individually
  - Respects UI interaction states (opacity checks)
  - Viewport-aware highlighting
  - Works with any fishing area type

#### 9. Net Depth Timer Overlay (`NetDepthTimerOverlay.java`)
- **Purpose**: Displays timing information for depth changes
- **Key Features**:
  - Shows countdown to depth transitions
  - Status indicators (waiting, calibrating, active)
  - Color-coded urgency (red for imminent changes)

## System Flow

### 1. Shoal Detection and Activation
```
GameObject Spawn → Tracking Activation → Chat Message Processing
```

1. **GameObject Detection**: System detects shoal spawn via `GameObjectSpawned` event
2. **Tracking Activation**: Enables chat message processing for depth tracking
3. **No Initialization**: Shoal depth is unknown until first chat message provides information

### 2. Chat Message-Based Depth Tracking
```
Chat Messages → Message Analysis → Depth Determination → State Updates → UI Notifications
```

1. **Definitive Depth Messages** (sets shoal depth):
   - "correct depth for the nearby" → Sets shoal depth to match current net depth
2. **Shoal Movement Messages** (updates known depth only if depth already established):
   - "closer to the surface" → Moves tracked shoal depth one level shallower
   - "shoal swims deeper into" → Moves tracked shoal depth one level deeper
3. **Informational Feedback Messages** (logged only, no depth changes):
   - "Your net is not deep enough" / "your net is too shallow" → Indicates shoal is deeper than current net
   - "Your net is too deep" → Indicates shoal is shallower than current net
4. **Conservative Approach**: Only sets shoal depth when receiving definitive confirmation messages
5. **Dependency Chain**: Movement messages require established baseline from "correct depth" message first

### 3. Timing System (Informational Only)
```
Movement Detection → Stop Detection → Timer Activation → Timing Display
```

1. **Movement Tracking**: Monitors shoal WorldEntity position changes
2. **Stop Detection**: Identifies when shoal stops moving (2+ ticks at same position)
3. **Timer Management**: Activates/deactivates based on movement patterns
4. **Timing Display**: Provides countdown information for UI (no depth updates)

### 4. User Interface Updates
```
State Changes → Overlay Updates → Button Highlighting → Timer Display
```

1. **Overlay Rendering**: Updates shoal highlighting colors
2. **Button Logic**: Determines which net adjustment buttons to highlight
3. **Timer Display**: Shows countdown and status information

## Chat Message Patterns

### Net Feedback Messages
- **"Your net is not deep enough"**: Indicates shoal is at a deeper level than current net
- **"your net is too shallow"**: Alternative phrasing for shoal being deeper
- **"Your net is too deep"**: Indicates shoal is at a shallower level than current net
- **"correct depth for the nearby"**: Confirms net is at the same depth as shoal

### Shoal Movement Messages
- **"closer to the surface"**: Shoal has moved one depth level shallower
- **"shoal swims deeper into"**: Shoal has moved one depth level deeper

### Depth Determination Logic
- **Definitive Setting**: Only "correct depth" messages set the initial shoal depth
- **Movement Updates**: Apply depth changes only when current depth is already known
- **No Inference**: Net feedback messages provide directional hints but don't set specific depths
- **Accuracy Priority**: Ensures tracked depth is always based on confirmed game information

## Configuration Options

### Available Settings
- `trawlingHighlightShoals`: Enable/disable shoal highlighting
- `trawlingShowNetDepthTimer`: Enable/disable timer and button highlighting
- `trawlingShoalHighlightColour`: Customizable highlight color

### Chat Commands
- `!traceroutes on/off`: Enable/disable route tracing for development

## Technical Implementation Details

### Varbit Integration
- **Port Net**: `VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_0_DEPTH` (19206)
- **Starboard Net**: `VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_1_DEPTH` (19208)
- **Values**: 0=net not lowered, 1=SHALLOW, 2=MODERATE, 3=DEEP

### Event Handling
- **GameObjectSpawned/Despawned**: Shoal lifecycle management
- **WorldEntitySpawned**: Movement tracking initialization
- **VarbitChanged**: Net depth updates
- **ChatMessage**: Depth change notifications
- **GameTick**: Timer updates and movement tracking

### Performance Optimizations
- **Caching**: Net depths cached to avoid repeated varbit queries
- **Selective Logging**: Reduced verbosity with milestone-based logging
- **Viewport Checking**: UI highlighting only for visible elements
- **State Management**: Efficient cleanup on shoal despawn

## Error Handling and Edge Cases

### Robust State Management
- **Null Safety**: Comprehensive null checks throughout
- **State Cleanup**: Proper cleanup on shoal despawn or plugin shutdown
- **Threading Safety**: Client thread assertions for varbit access
- **Fallback Logic**: Graceful degradation when data is unavailable

### Known Limitations
- **Initial State**: Shoal depth remains unknown until "correct depth" confirmation message
- **Confirmation Dependency**: Requires player to achieve correct depth at least once to establish baseline
- **Movement Dependency**: Shoal movement messages only work if depth is already established
- **Conservative Approach**: May miss some depth information to ensure accuracy
- **No Predictive Logic**: System no longer attempts to predict or infer depths from indirect information
- **Deprecated Features**: Movement direction tracking and three-depth area logic no longer functional
- **Animation ID Support**: Currently logging-only, not used for depth detection
- **Route Accuracy**: Path tracing may require manual adjustment

## Development and Debugging

### Logging Levels
- **INFO**: Major state changes, depth transitions, shoal events
- **DEBUG**: Detailed timing information, UI updates, movement tracking
- **WARN**: Unexpected states, missing data, edge cases

### Testing Support
- **Mock Integration**: Comprehensive test coverage with Mockito
- **Updated Test Suite**: Tests reflect new chat message-based behavior
- **Deprecated Functionality**: Tests updated to handle legacy method compatibility
- **Conservative Testing**: Validates that depth is only set with confirmed information
- **Integration Testing**: End-to-end workflow validation

### Future Enhancements
- **Animation Integration**: Use animation IDs for immediate depth detection without requiring chat messages
- **Proactive Depth Detection**: Detect shoal depth changes without requiring net adjustments
- **Advanced UI**: More sophisticated overlay options with confidence indicators
- **Message Pattern Learning**: Expand recognition of additional chat message variations

## Recent Changes and Migration

### Major Refactoring (Chat Message-Based Implementation)

The trawling system underwent a significant refactoring to improve accuracy and reliability:

#### What Changed:
1. **ShoalDepthTracker Refactoring**:
   - Removed area-based depth initialization
   - Removed timing-based depth predictions
   - Removed movement direction tracking
   - Implemented pure chat message-based depth tracking

2. **NetDepthButtonHighlighter Simplification**:
   - Removed complex three-depth area logic
   - Simplified to basic depth matching
   - Only highlights when correction is needed

3. **NetDepthTimer Role Change**:
   - No longer updates ShoalDepthTracker directly
   - Provides timing information for UI display only
   - Maintains timing predictions for reference

#### Migration Impact:
- **Backward Compatibility**: Deprecated methods maintained for compatibility
- **Test Updates**: All tests updated to reflect new behavior
- **Improved Accuracy**: Depth tracking now based on confirmed game information only
- **Simplified Logic**: Removed complex prediction algorithms in favor of reliable chat message parsing

#### Benefits:
- **Higher Accuracy**: No more incorrect depth predictions
- **Simpler Maintenance**: Reduced complexity in core tracking logic
- **Better User Experience**: UI only shows information when certain
- **Reliable State**: Depth information always matches actual game state

## Conclusion

The trawling system represents a sophisticated integration of game state tracking, predictive timing, and user interface enhancement. The recent refactoring to a chat message-based approach demonstrates a commitment to accuracy over complexity, ensuring that players receive reliable information for optimizing their trawling experience. The modular architecture ensures maintainability while providing comprehensive functionality through advanced RuneLite plugin development techniques including event handling, UI manipulation, state management, and performance optimization.
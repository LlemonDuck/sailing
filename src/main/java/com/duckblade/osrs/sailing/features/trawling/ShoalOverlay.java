package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;

import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Singleton
public class ShoalOverlay extends Overlay
        implements PluginLifecycleComponent {

    private static final int SHOAL_HIGHLIGHT_SIZE = 10;

    // Widget indices for net depth indicators
    private static final int STARBOARD_DEPTH_WIDGET_INDEX = 96;
    private static final int PORT_DEPTH_WIDGET_INDEX = 131;
    
    // Sprite IDs for each depth level
    private static final int SPRITE_SHALLOW = 7081;
    private static final int SPRITE_MODERATE = 7082;
    private static final int SPRITE_DEEP = 7083;

    // Clickbox IDs
    private static final Set<Integer> SHOAL_CLICKBOX_IDS = ImmutableSet.of(
            TrawlingData.ShoalObjectID.BLUEFIN,
            TrawlingData.ShoalObjectID.GIANT_KRILL,
            TrawlingData.ShoalObjectID.GLISTENING,
            TrawlingData.ShoalObjectID.HADDOCK,
            TrawlingData.ShoalObjectID.HALIBUT,
            TrawlingData.ShoalObjectID.MARLIN,
            TrawlingData.ShoalObjectID.SHIMMERING,
            TrawlingData.ShoalObjectID.VIBRANT,
            TrawlingData.ShoalObjectID.YELLOWFIN);

    @Nonnull
    private final Client client;
    private final SailingConfig config;
    private final ShoalDepthTracker shoalDepthTracker;
    private final BoatTracker boatTracker;
    private final Set<GameObject> shoals = new HashSet<>();

    @Inject
    public ShoalOverlay(@Nonnull Client client, SailingConfig config, 
                       ShoalDepthTracker shoalDepthTracker, BoatTracker boatTracker) {
        this.client = client;
        this.config = config;
        this.shoalDepthTracker = shoalDepthTracker;
        this.boatTracker = boatTracker;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        return config.trawlingHighlightShoals();
    }

    @Override
    public void startUp() {
        log.debug("ShoalOverlay starting up");
    }

    @Override
    public void shutDown() {
        log.debug("ShoalOverlay shutting down");
        shoals.clear();
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        if (SHOAL_CLICKBOX_IDS.contains(objectId)) {
            shoals.add(obj);
            log.debug("Shoal spawned with ID {} at {} (total shoals: {})", objectId, obj.getLocalLocation(), shoals.size());
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        if (shoals.remove(obj)) {
            log.debug("Shoal despawned with ID {}", obj.getId());
        }
    }

    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded e) {
        // Only clear shoals when we're not actively sailing
        // During sailing, shoals move and respawn frequently, so we keep them tracked
        if (!e.getWorldView().isTopLevel()) {
            return;
        }
        
        // Check if player and worldview are valid before calling isSailing
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getWorldView() == null) {
            log.debug("Top-level world view unloaded (player/worldview null), clearing {} shoals", shoals.size());
            shoals.clear();
            return;
        }
        
        if (!SailingUtil.isSailing(client)) {
            log.debug("Top-level world view unloaded while not sailing, clearing {} shoals", shoals.size());
            shoals.clear();
        }
    }



    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.trawlingHighlightShoals() || shoals.isEmpty()) {
            return null;
        }

        // Track which object IDs we've already rendered to avoid stacking overlays
        Set<Integer> renderedIds = new HashSet<>();
        
        for (GameObject shoal : shoals) {
            int objectId = shoal.getId();
            // Only render one shoal per object ID to avoid overlay stacking
            if (!renderedIds.contains(objectId)) {
                renderShoalHighlight(graphics, shoal);
                renderedIds.add(objectId);
            }
        }

        return null;
    }

    private void renderShoalHighlight(Graphics2D graphics, GameObject shoal) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(client, shoal.getLocalLocation(), SHOAL_HIGHLIGHT_SIZE);
        if (poly != null) {
            Color color = getShoalColor(shoal.getId());
            Stroke originalStroke = graphics.getStroke();
            graphics.setStroke(new BasicStroke(0.5f));
            OverlayUtil.renderPolygon(graphics, poly, color);
            graphics.setStroke(originalStroke);
        }
    }

    private Color getShoalColor(int objectId) {
        // Priority 1: Check depth matching (highest priority)
        NetDepth shoalDepth = shoalDepthTracker.getCurrentDepth();
        if (shoalDepth != null) {
            NetDepth playerDepth = getPlayerNetDepth();
            if (playerDepth != null && playerDepth != shoalDepth) {
                return Color.RED; // Wrong depth - highest priority
            }
        }
        
        // Priority 2: Special shoals use green (medium priority)
        if (isSpecialShoal(objectId)) {
            return Color.GREEN;
        }
        
        // Priority 3: Default to configured color (lowest priority)
        return config.trawlingShoalHighlightColour();
    }

    /**
     * Helper method to get player's current net depth from BoatTracker
     * Returns null if player has no nets equipped or nets are not available
     */
    private NetDepth getPlayerNetDepth() {
        Boat boat = boatTracker.getBoat();
        if (boat == null || boat.getNetTiers().isEmpty()) {
            return null;
        }

        // Get the facilities widget to read net depth from UI
        Widget widgetSailingRows = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
        if (widgetSailingRows == null) {
            return null;
        }

        // Try to get depth from starboard net first, then port net
        NetDepth starboardDepth = getNetDepthFromWidget(widgetSailingRows, STARBOARD_DEPTH_WIDGET_INDEX);
        if (starboardDepth != null) {
            return starboardDepth;
        }

        NetDepth portDepth = getNetDepthFromWidget(widgetSailingRows, PORT_DEPTH_WIDGET_INDEX);
        return portDepth;
    }

    /**
     * Get the current net depth from widget sprite
     */
    private NetDepth getNetDepthFromWidget(Widget parent, int widgetIndex) {
        Widget depthWidget = parent.getChild(widgetIndex);
        if (depthWidget == null) {
            return null;
        }

        int spriteId = depthWidget.getSpriteId();
        
        if (spriteId == SPRITE_SHALLOW) {
            return NetDepth.SHALLOW;
        } else if (spriteId == SPRITE_MODERATE) {
            return NetDepth.MODERATE;
        } else if (spriteId == SPRITE_DEEP) {
            return NetDepth.DEEP;
        }

        return null;
    }

    /**
     * Check if the shoal is a special type (VIBRANT, GLISTENING, SHIMMERING)
     */
    private boolean isSpecialShoal(int objectId) {
        return objectId == TrawlingData.ShoalObjectID.VIBRANT ||
               objectId == TrawlingData.ShoalObjectID.GLISTENING ||
               objectId == TrawlingData.ShoalObjectID.SHIMMERING;
    }

}

package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Set;

@Slf4j
@Singleton
public class ShoalOverlay extends Overlay
        implements PluginLifecycleComponent {

    private static final int SHOAL_HIGHLIGHT_SIZE = 10;

    @Nonnull
    private final Client client;
    private final SailingConfig config;
    private final ShoalTracker shoalTracker;
    private final NetDepthTimer netDepthTimer;

    @Inject
    public ShoalOverlay(@Nonnull Client client, SailingConfig config, ShoalTracker shoalTracker, NetDepthTimer netDepthTimer) {
        this.client = client;
        this.config = config;
        this.shoalTracker = shoalTracker;
        this.netDepthTimer = netDepthTimer;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGHEST);
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
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.trawlingHighlightShoals()) {
            return null;
        }

        // Use NPC for highlighting instead of GameObjects for more reliable rendering
        NPC shoalNpc = shoalTracker.getCurrentShoalNpc();
        if (shoalNpc != null) {
            renderShoalNpcHighlight(graphics, shoalNpc);
            renderDepthTimer(graphics, shoalNpc);
            return null;
        }

        // Fallback to GameObject highlighting if NPC is not available
        Set<GameObject> shoals = shoalTracker.getShoalObjects();
        if (!shoals.isEmpty()) {
            GameObject shoalToHighlight = selectShoalToHighlight(shoals);
            if (shoalToHighlight != null) {
                renderShoalHighlight(graphics, shoalToHighlight);
            }
        }

        return null;
    }

    /**
     * Select which shoal to highlight when multiple shoals are present.
     * Priority: Special shoals (green) > Regular shoals (config color)
     */
    private GameObject selectShoalToHighlight(Set<GameObject> shoals) {
        GameObject firstSpecialShoal = null;
        GameObject firstRegularShoal = null;
        
        for (GameObject shoal : shoals) {
            if (isSpecialShoal(shoal.getId())) {
                if (firstSpecialShoal == null) {
                    firstSpecialShoal = shoal;
                }
            } else {
                if (firstRegularShoal == null) {
                    firstRegularShoal = shoal;
                }
            }
            
            // If we have both types, we can stop looking
            if (firstSpecialShoal != null && firstRegularShoal != null) {
                break;
            }
        }
        
        // Prioritize special shoals over regular ones
        return firstSpecialShoal != null ? firstSpecialShoal : firstRegularShoal;
    }

    private void renderShoalNpcHighlight(Graphics2D graphics, NPC shoalNpc) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(client, shoalNpc.getLocalLocation(), SHOAL_HIGHLIGHT_SIZE);
        if (poly != null) {
            // Use depth-based coloring for NPC highlighting
            Color color = getShoalColorFromDepth();
            Stroke originalStroke = graphics.getStroke();
            graphics.setStroke(new BasicStroke(0.5f));
            OverlayUtil.renderPolygon(graphics, poly, color);
            graphics.setStroke(originalStroke);
        }
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

    private Color getShoalColorFromDepth() {
        // Check if we have any special shoal GameObjects
        Set<GameObject> shoals = shoalTracker.getShoalObjects();
        boolean hasSpecialShoal = shoals.stream()
            .anyMatch(shoal -> isSpecialShoal(shoal.getId()));
        
        if (hasSpecialShoal) {
            log.debug("Special shoal detected, using green highlight");
            return Color.GREEN;
        }
        
        // Use config color for regular shoals
        log.debug("Regular shoal detected, using config color");
        return config.trawlingShoalHighlightColour();
    }

    private Color getShoalColor(int objectId) {
        if (isSpecialShoal(objectId)) {
            return Color.GREEN;
        }
        return config.trawlingShoalHighlightColour();
    }

    /**
     * Check if the shoal is a special type (VIBRANT, GLISTENING, SHIMMERING)
     */
    private boolean isSpecialShoal(int objectId) {
        return objectId == TrawlingData.ShoalObjectID.VIBRANT ||
               objectId == TrawlingData.ShoalObjectID.GLISTENING ||
               objectId == TrawlingData.ShoalObjectID.SHIMMERING;
    }

    /**
     * Render depth timer text on the shoal NPC
     */
    private void renderDepthTimer(Graphics2D graphics, NPC shoalNpc) {
        if (!config.trawlingShowNetDepthTimer()) {
            return;
        }

        NetDepthTimer.TimerInfo timerInfo = netDepthTimer.getTimerInfo();
        if (timerInfo == null) {
            return;
        }

        Point textLocation = Perspective.getCanvasTextLocation(client, graphics, shoalNpc.getLocalLocation(), getTimerText(timerInfo), 0);
        if (textLocation != null) {
            renderTimerText(graphics, textLocation, timerInfo);
        }
    }



    /**
     * Get the text to display for the timer
     */
    private String getTimerText(NetDepthTimer.TimerInfo timerInfo) {
        if (timerInfo.isActive()) {
            int ticksUntilChange = timerInfo.getTicksUntilDepthChange();
            return String.valueOf(ticksUntilChange);
        }
        return null;
    }

    /**
     * Render the timer text with appropriate styling
     */
    private void renderTimerText(Graphics2D graphics, Point textLocation, NetDepthTimer.TimerInfo timerInfo) {
        Font originalFont = graphics.getFont();
        Color originalColor = graphics.getColor();

        // Set font and color
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        
        Color textColor;
        if (!timerInfo.isActive()) {
            textColor = timerInfo.isWaiting() ? Color.ORANGE : Color.YELLOW;
        } else {
            int ticksUntilChange = timerInfo.getTicksUntilDepthChange();
            textColor = ticksUntilChange <= 5 ? Color.RED : Color.WHITE;
        }

        String text = getTimerText(timerInfo);
        
        // Draw text with black outline for better visibility
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        // Center the text
        int x = textLocation.getX() - textWidth / 2;
        int y = textLocation.getY() + textHeight / 4;
        
        // Draw black outline
        graphics.setColor(Color.BLACK);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    graphics.drawString(text, x + dx, y + dy);
                }
            }
        }
        
        // Draw main text
        graphics.setColor(textColor);
        graphics.drawString(text, x, y);

        // Restore original font and color
        graphics.setFont(originalFont);
        graphics.setColor(originalColor);
    }

}

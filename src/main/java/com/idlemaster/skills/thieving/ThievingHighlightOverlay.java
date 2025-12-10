package com.idlemaster.skills.thieving;

import com.idlemaster.IdleMasterConfig;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.*;

/**
 * Overlay to highlight the distracted wealthy citizen.
 */
public class ThievingHighlightOverlay extends Overlay {
    
    private static final Color OUTLINE_COLOR = new Color(0, 255, 0, 255); // Bright green outline
    private static final int OUTLINE_WIDTH = 2;
    private static final int FEATHER = 4;
    private final IdleMasterConfig config;
    private final ThievingManager thievingManager;
    private final ModelOutlineRenderer modelOutlineRenderer;
    
    @Inject
    public ThievingHighlightOverlay(IdleMasterConfig config, 
                                     ThievingManager thievingManager,
                                     ModelOutlineRenderer modelOutlineRenderer) {
        this.config = config;
        this.thievingManager = thievingManager;
        this.modelOutlineRenderer = modelOutlineRenderer;
        
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }
    
    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enableThievingOverlay() || !config.highlightDistractedCitizen()) {
            return null;
        }
        
        NPC distractedNpc = thievingManager.getDistractedNpc();
        if (distractedNpc == null) {
            return null;
        }
        
        // Only highlight when in thieving area
        if (!thievingManager.getThievingInfo().isInThievingArea()) {
            return null;
        }
        
        // Render the outline with fill
        modelOutlineRenderer.drawOutline(distractedNpc, OUTLINE_WIDTH, OUTLINE_COLOR, FEATHER);
        
        return null;
    }
}

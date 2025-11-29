package com.idlemaster;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;

/**
 * In-game overlay for Idle Master (minimal, as main display is in floating window)
 */
public class IdleMasterOverlay extends Overlay {

    private final IdleMasterConfig config;

    @Inject
    public IdleMasterOverlay(IdleMasterConfig config) {
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // The main overlay is the floating window
        // This overlay can be used for in-game indicators if needed
        return null;
    }
}

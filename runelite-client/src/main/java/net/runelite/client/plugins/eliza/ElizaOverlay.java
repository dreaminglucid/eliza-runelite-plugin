package net.runelite.client.plugins.eliza;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ElizaOverlay extends OverlayPanel {
    private final ElizaPlugin plugin;
    private final ElizaConfig config;
    private final PlayerTracker playerTracker;
    
    private Instant lastUpdate = Instant.now();
    private int cachedPlayerCount = 0;
    private int cachedRecentCount = 0;
    private static final int UPDATE_INTERVAL_SECONDS = 5;

    @Inject
    public ElizaOverlay(ElizaPlugin plugin, ElizaConfig config, PlayerTracker playerTracker) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.plugin = plugin;
        this.config = config;
        this.playerTracker = playerTracker;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        // Only update counts every few seconds
        if (Instant.now().isAfter(lastUpdate.plus(UPDATE_INTERVAL_SECONDS, ChronoUnit.SECONDS))) {
            cachedPlayerCount = playerTracker.getActivePlayerCount();
            cachedRecentCount = playerTracker.getRecentInteractorCount();
            lastUpdate = Instant.now();
            playerTracker.updateActivePlayers(); // Update player list periodically
        }

        panelComponent.getChildren().clear();

        // Keep panel minimal
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("Bridge Troll")
            .color(plugin.isStarted() ? Color.GREEN : Color.RED)
            .build());

        if (plugin.isStarted()) {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Players:")
                .right(String.valueOf(cachedPlayerCount))
                .build());

            if (cachedRecentCount > 0) {
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("Recent:")
                    .right(String.valueOf(cachedRecentCount))
                    .build());
            }
        }

        return super.render(graphics);
    }
}
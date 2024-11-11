package net.runelite.client.plugins.bridgetroll;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class BridgeTrollOverlay extends OverlayPanel {
    private final BridgeTrollPlugin plugin;
    private final BridgeTrollConfig config;
    private final PlayerTracker playerTracker;

    @Inject
    public BridgeTrollOverlay(BridgeTrollPlugin plugin, BridgeTrollConfig config, PlayerTracker playerTracker) {
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

        panelComponent.getChildren().add(TitleComponent.builder()
            .text("Bridge Troll")
            .color(Color.GREEN)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Status:")
            .right(plugin.isStarted() ? "Active" : "Inactive")
            .rightColor(plugin.isStarted() ? Color.GREEN : Color.RED)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Players:")
            .right(String.valueOf(playerTracker.getActivePlayerCount()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Recent Players:")
            .right(String.valueOf(playerTracker.getRecentInteractorCount()))
            .build());

        return super.render(graphics);
    }
}
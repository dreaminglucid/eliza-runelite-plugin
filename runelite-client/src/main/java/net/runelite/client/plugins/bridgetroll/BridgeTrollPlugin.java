package net.runelite.client.plugins.bridgetroll;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "Bridge Troll",
    description = "A bridge troll that responds to player interactions using AI",
    tags = {"npc", "chatbot", "bridge", "troll", "interaction", "ai"}
)
public class BridgeTrollPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private BridgeTrollConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private BridgeTrollOverlay overlay;

    @Inject
    private MessageHandler messageHandler;

    @Inject
    private PlayerTracker playerTracker;

    @Inject
    private APIService apiService;

    private boolean isStarted = false;

    @Provides
    BridgeTrollConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BridgeTrollConfig.class);
    }

    @Override
    protected void startUp() {
        log.info("========== BRIDGE TROLL STARTUP ==========");
        overlayManager.add(overlay);
        playerTracker.clear();
        if (config.enabled()) {
            start();
        }
    }

    @Override
    protected void shutDown() {
        log.info("========== BRIDGE TROLL SHUTDOWN ==========");
        overlayManager.remove(overlay);
        playerTracker.clear();
        stop();
    }

    private void start() {
        isStarted = true;
        messageHandler.reset();
        playerTracker.reset();
        log.info("Bridge Troll started. API Endpoint: {}", config.apiEndpoint());
    }

    private void stop() {
        isStarted = false;
        messageHandler.reset();
        playerTracker.reset();
        log.info("Bridge Troll stopped");
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (!config.enabled() || client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        messageHandler.processQueue(clientThread);
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (!config.enabled() || !isStarted || chatMessage.getType() != ChatMessageType.PUBLICCHAT) {
            return;
        }

        messageHandler.handleChatMessage(chatMessage, client, playerTracker, apiService, config);
    }

    public boolean isStarted() {
        return isStarted;
    }
}
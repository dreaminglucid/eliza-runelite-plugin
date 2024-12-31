package net.runelite.client.plugins.eliza;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.eliza.api.external.APIService;
import net.runelite.client.plugins.eliza.api.local.WorldStateEndpoint;
import net.runelite.client.plugins.eliza.config.ElizaConfig;
import net.runelite.client.plugins.eliza.services.chat.MessageHandler;
import net.runelite.client.plugins.eliza.services.equipment.EquipmentService;
import net.runelite.client.plugins.eliza.services.player.OtherPlayerTracker;
import net.runelite.client.plugins.eliza.services.world.WorldService;
import net.runelite.client.plugins.eliza.state.EquipmentState;
import net.runelite.client.plugins.eliza.state.GameDataSnapshot;
import net.runelite.client.plugins.eliza.state.LocationState;
import net.runelite.client.plugins.eliza.ui.ElizaOverlay;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(name = "Eliza", description = "eliza integrated with runelite", tags = { "npc", "chatbot", "eliza",
        "interaction", "ai" })
public class ElizaPlugin extends Plugin {
    // A static snapshot accessible by the HTTP server
    private static final GameDataSnapshot snapshot = new GameDataSnapshot();

    // Provide a getter so the WorldStateEndpoint can read it
    public static GameDataSnapshot getSnapshot() {
        return snapshot;
    }

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ElizaConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ElizaOverlay overlay;

    @Inject
    private MessageHandler messageHandler;

    @Inject
    private OtherPlayerTracker playerTracker;

    @Inject
    private EquipmentService equipmentService;

    @Inject
    private WorldService worldService;

    @Inject
    private APIService apiService;

    // The server providing /world-state
    @Inject
    private WorldStateEndpoint worldStateEndpoint;

    private boolean isStarted = false;

    @Provides
    ElizaConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ElizaConfig.class);
    }

    // Provide the WorldStateEndpoint to Guice
    @Provides
    @Singleton
    public static WorldStateEndpoint provideWorldStateEndpoint(
            Client client,
            WorldService worldService,
            OtherPlayerTracker playerTracker,
            EquipmentService equipmentService) {
        return new WorldStateEndpoint(client, worldService, playerTracker, equipmentService);
    }

    @Override
    protected void startUp() {
        log.info("========== ELIZA STARTUP ==========");
        overlayManager.add(overlay);
        playerTracker.clear();

        // Start the local server
        worldStateEndpoint.start();

        if (config.enabled()) {
            startPlugin();
        }
    }

    @Override
    protected void shutDown() {
        log.info("========== ELIZA SHUTDOWN ==========");
        overlayManager.remove(overlay);

        // Stop the server
        worldStateEndpoint.stop();

        playerTracker.clear();
        stopPlugin();
    }

    private void startPlugin() {
        isStarted = true;
        messageHandler.reset();
        playerTracker.reset();
        log.info("eliza started. API Endpoint: {}", config.apiEndpoint());
    }

    private void stopPlugin() {
        isStarted = false;
        messageHandler.reset();
        playerTracker.reset();
        log.info("eliza stopped");
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        // First, check if plugin is enabled and the game is fully logged in
        if (!config.enabled() || client.getGameState() != GameState.LOGGED_IN) {
            // Mark snapshot as not logged in, so the server sees minimal data
            snapshot.setLoggedIn(false);
            return;
        }

        // We are logged in, so update the snapshot
        Player local = client.getLocalPlayer();
        if (local != null) {
            snapshot.setLoggedIn(true);
            snapshot.setPlayerName(local.getName());

            // Build and store the location snapshot
            LocationState locState = worldService.buildLocationState(local, playerTracker);
            snapshot.setLocationState(locState);

            // Build and store the equipment snapshot
            EquipmentState eqState = equipmentService.buildEquipmentSnapshot(local);
            snapshot.setEquipmentState(eqState);

            snapshot.setTotalPlayers(playerTracker.getActivePlayerCount());
            snapshot.setTimestamp(System.currentTimeMillis());
        } else {
            snapshot.setLoggedIn(false);
        }

        // IMPORTANT: process queued messages so they actually get sent
        messageHandler.processQueue(clientThread);
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        // Only handle chat if enabled, started, and it's public chat
        if (!config.enabled() || !isStarted || chatMessage.getType() != ChatMessageType.PUBLICCHAT) {
            return;
        }

        // Pass the chat message to the AI for potential response
        messageHandler.handleChatMessage(chatMessage, client, playerTracker, apiService, config);
    }

    public boolean isStarted() {
        return isStarted;
    }
}
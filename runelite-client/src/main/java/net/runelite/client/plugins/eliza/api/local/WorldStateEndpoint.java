package net.runelite.client.plugins.eliza.api.local;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.plugins.eliza.ElizaPlugin;
import net.runelite.client.plugins.eliza.services.equipment.EquipmentService;
import net.runelite.client.plugins.eliza.services.player.OtherPlayerTracker;
import net.runelite.client.plugins.eliza.services.world.WorldService;
import net.runelite.client.plugins.eliza.state.GameDataSnapshot;
import net.runelite.client.plugins.eliza.state.LocationState;
import net.runelite.client.plugins.eliza.state.EquipmentState;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

@Slf4j
@Singleton
public class WorldStateEndpoint {
    private HttpServer server;
    private static final int PORT = 3001;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Client client;
    private final WorldService worldService;
    private final OtherPlayerTracker playerTracker;
    private final EquipmentService equipmentService;

    public WorldStateEndpoint(
            Client client,
            WorldService worldService,
            OtherPlayerTracker playerTracker,
            EquipmentService equipmentService) {
        this.client = client;
        this.worldService = worldService;
        this.playerTracker = playerTracker;
        this.equipmentService = equipmentService;

        log.info("WorldStateEndpoint constructor called");
    }

    public void start() {
        log.info("========== WORLD STATE ENDPOINT START ==========");
        try {
            log.info("Creating server on port {}", PORT);
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            log.info("Creating context /world-state");
            server.createContext("/world-state", new WorldStateHandler());
            server.setExecutor(null); // default single-thread

            log.info("Starting server...");
            server.start();

            log.info("World state endpoint started at http://localhost:{}/world-state", PORT);
            log.info("============================================");
        } catch (IOException e) {
            log.error("Failed to start world state endpoint", e);
            log.error("Cause: {}", e.getMessage());
            log.error("============================================");
        }
    }

    public void stop() {
        log.info("========== WORLD STATE ENDPOINT STOP ==========");
        if (server != null) {
            server.stop(0);
            log.info("World state endpoint stopped");
        } else {
            log.info("No server was running");
        }
        log.info("============================================");
    }

    private class WorldStateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            log.info("Received world state request from: {}", exchange.getRemoteAddress());

            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                JsonObject worldState = buildSnapshotJson();
                String response = gson.toJson(worldState);

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

                byte[] responseBytes = response.getBytes();
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                log.error("Error handling world state request", e);
                exchange.sendResponseHeaders(500, -1);
            }
        }

        private JsonObject buildSnapshotJson() {
            GameDataSnapshot snap = ElizaPlugin.getSnapshot();

            JsonObject state = new JsonObject();
            JsonObject currentPlayer = new JsonObject();

            if (snap.isLoggedIn()) {
                currentPlayer.addProperty("name", snap.getPlayerName());

                // location
                LocationState loc = snap.getLocationState();
                JsonObject locationObj = new JsonObject();
                locationObj.addProperty("x", loc.getX());
                locationObj.addProperty("y", loc.getY());
                locationObj.addProperty("plane", loc.getPlane());
                locationObj.addProperty("regionId", loc.getRegionId());
                locationObj.addProperty("description", loc.getDescription());
                // add nearbyPlayers array
                locationObj.add("nearbyPlayers", gson.toJsonTree(loc.getNearbyPlayers()));

                currentPlayer.add("location", locationObj);

                // equipment
                EquipmentState eq = snap.getEquipmentState();
                JsonObject equipObj = new JsonObject();
                equipObj.add("slots", gson.toJsonTree(eq.getSlots()));
                equipObj.addProperty("description", eq.getDescription());

                currentPlayer.add("equipment", equipObj);
            } else {
                // Not logged in -> minimal structure
                currentPlayer.addProperty("name", "Unknown");
                JsonObject locationObj = new JsonObject();
                locationObj.addProperty("description", "Not logged in or loading");
                currentPlayer.add("location", locationObj);

                JsonObject equipObj = new JsonObject();
                equipObj.addProperty("description", "No equipment");
                currentPlayer.add("equipment", equipObj);
            }

            state.add("currentPlayer", currentPlayer);

            // Add context and timestamp
            JsonObject context = new JsonObject();
            context.addProperty("totalPlayers", snap.getTotalPlayers());
            context.addProperty("timestamp", snap.getTimestamp());
            state.add("context", context);

            return state;
        }
    }
}
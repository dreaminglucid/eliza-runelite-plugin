package net.runelite.client.plugins.bridgetroll;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import okhttp3.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class APIService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    @Inject
    private EquipmentService equipmentService;

    @Inject
    private WorldService worldService;

    public void sendMessage(String sender, String message, Client client,
            PlayerTracker playerTracker, BridgeTrollConfig config,
            Consumer<List<String>> responseHandler) {
        JsonObject requestBody = createRequestBody(sender, message, client, playerTracker);

        Request request = new Request.Builder()
                .url(config.apiEndpoint() + "/troll/message")
                .post(RequestBody.create(JSON, requestBody.toString()))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("API request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleAPIResponse(response, playerTracker, responseHandler);
            }
        });
    }

    private JsonObject createRequestBody(String sender, String message, Client client, PlayerTracker playerTracker) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("userId", sender);
        requestBody.addProperty("userName", sender);
        requestBody.add("context", createContextObject(sender, playerTracker));

        // Get equipment state
        Player player = playerTracker.findPlayerByName(sender);
        JsonObject equipmentState = equipmentService.getEquipmentState(player);
        requestBody.add("equipment", equipmentState);

        // Get world location state
        JsonObject locationState = worldService.getWorldLocation(player);
        requestBody.add("location", locationState);

        // Create full message with equipment description and location
        String equipmentDesc = equipmentService.getEquipmentDescription(player);
        String locationDesc = worldService.getLocationDescription(player);
        String fullMessage = String.format("%s (%s is wearing: %s, located in: %s)",
                message, sender, equipmentDesc, locationDesc);
        requestBody.addProperty("text", fullMessage);

        // Add available actions
        JsonArray actions = new JsonArray();
        actions.add("walk");
        actions.add("examine");
        actions.add("talk_to");
        requestBody.add("actions", actions);

        log.info("API request body: {}", requestBody);
        return requestBody;
    }

    private JsonObject createContextObject(String sender, PlayerTracker playerTracker) {
        JsonObject context = new JsonObject();
        JsonArray recentPlayers = new JsonArray();

        for (String player : playerTracker.getRecentInteractors()) {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("name", player);
            playerObj.addProperty("interactions", playerTracker.getPlayerInteractions().getOrDefault(player, 0));
            recentPlayers.add(playerObj);
        }

        context.add("recentPlayers", recentPlayers);
        context.addProperty("totalPlayers", playerTracker.getActivePlayerCount());
        context.addProperty("currentSpeaker", sender);
        context.addProperty("isBusyChat",
                playerTracker.getActivePlayerCount() > BridgeTrollConstants.QUIET_CHAT_THRESHOLD);

        return context;
    }

    private void handleAPIResponse(Response response, PlayerTracker playerTracker,
            Consumer<List<String>> responseHandler) throws IOException {
        try (response) {
            if (!response.isSuccessful()) {
                log.error("API request unsuccessful: {}", response.code());
                return;
            }

            String jsonResponse = response.body().string();
            log.info("API response: {}", jsonResponse);

            JsonArray responseArray = new JsonParser().parse(jsonResponse).getAsJsonArray();

            if (responseArray.size() == 0) {
                log.error("Empty response from API");
                return;
            }

            JsonObject messageObj = responseArray.get(0).getAsJsonObject();
            String trollResponse = messageObj.get("text").getAsString();
            String action = messageObj.has("action") ? messageObj.get("action").getAsString() : "NONE";

            if ("IGNORE".equals(action)) {
                log.info("Ignoring message due to IGNORE action");
                return;
            }

            List<String> processedMessages = processResponse(trollResponse, playerTracker);
            responseHandler.accept(processedMessages);

            // Execute the chosen action
            executeAction(action, playerTracker.findPlayerByName(messageObj.get("userName").getAsString()));
        } catch (Exception e) {
            log.error("Error processing API response", e);
        }
    }

    private void executeAction(String action, Player player) {
        if (action.equals("walk")) {
            worldService.walkTo(player.getWorldLocation());
        } else if (action.equals("examine")) {
            // Implement examine action
        } else if (action.equals("talk_to")) {
            // Implement talk_to action
        }
    }

    private List<String> processResponse(String response, PlayerTracker playerTracker) {
        response = stripEmotes(response);

        // Handle @username mentions
        for (String playerName : playerTracker.getActivePlayerNames()) {
            String atMention = "@" + playerName;
            if (response.contains(atMention)) {
                response = response.replace(atMention, playerName + ":");
            }
        }

        List<String> messageParts = splitMessage(response);
        return limitMessageParts(messageParts, playerTracker.getActivePlayerCount());
    }

    private String stripEmotes(String message) {
        return message.replaceAll("\\*[^*]*\\*", "").trim();
    }

    private List<String> splitMessage(String message) {
        List<String> parts = new ArrayList<>();
        if (message == null || message.isEmpty()) {
            return parts;
        }

        String[] sentences = message.split("(?<=[.!?])\\s+");
        StringBuilder currentPart = new StringBuilder();

        for (String sentence : sentences) {
            if (currentPart.length() > 0 &&
                    currentPart.length() + sentence.length() + 1 > BridgeTrollConstants.MAX_MESSAGE_LENGTH) {
                parts.add(currentPart.toString().trim());
                currentPart.setLength(0);
            }

            if (sentence.length() > BridgeTrollConstants.MAX_MESSAGE_LENGTH) {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString().trim());
                    currentPart.setLength(0);
                }

                String[] words = sentence.split("\\s+");
                for (String word : words) {
                    if (currentPart.length() + word.length() + 1 > BridgeTrollConstants.MAX_MESSAGE_LENGTH) {
                        if (currentPart.length() > BridgeTrollConstants.MIN_MESSAGE_LENGTH ||
                                currentPart.length() + word.length() > BridgeTrollConstants.MAX_MESSAGE_LENGTH) {
                            parts.add(currentPart.toString().trim());
                            currentPart.setLength(0);
                        }
                    }

                    if (currentPart.length() > 0) {
                        currentPart.append(" ");
                    }
                    currentPart.append(word);
                }
            } else {
                if (currentPart.length() > 0) {
                    currentPart.append(" ");
                }
                currentPart.append(sentence);
            }
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString().trim());
        }

        return parts;
    }

    private List<String> limitMessageParts(List<String> messageParts, int activePlayerCount) {
        if (messageParts.isEmpty()) {
            return messageParts;
        }

        if (activePlayerCount > BridgeTrollConstants.QUIET_CHAT_THRESHOLD) {
            return Collections.singletonList(messageParts.get(0));
        }

        int numMessages = Math.min(1 + (int) (Math.random() * 3), messageParts.size());

        if (messageParts.size() > numMessages) {
            List<String> limitedParts = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();

            for (int i = 0; i < messageParts.size(); i++) {
                indices.add(i);
            }

            for (int i = 0; i < numMessages; i++) {
                int randomIndex = (int) (Math.random() * indices.size());
                limitedParts.add(messageParts.get(indices.get(randomIndex)));
                indices.remove(randomIndex);
            }

            limitedParts.sort((a, b) -> messageParts.indexOf(a) - messageParts.indexOf(b));
            return limitedParts;
        }

        return messageParts;
    }
}
package net.runelite.client.plugins.eliza.api;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.plugins.eliza.config.ElizaConfig;
import net.runelite.client.plugins.eliza.services.OtherPlayerTracker;
import net.runelite.client.plugins.eliza.utils.ElizaConstants;
import okhttp3.*;
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

    public void sendMessage(String sender, String message, Client client,
            OtherPlayerTracker playerTracker, ElizaConfig config,
            Consumer<List<String>> responseHandler) {
        try {
            JsonObject requestBody = createRequestBody(sender, message, client, playerTracker);

            Request request = new Request.Builder()
                    .url(config.apiEndpoint() + "/24b86618-cfdf-02dc-8b23-84627ec0e9ea/message")
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
        } catch (Exception e) {
            log.error("Error sending message", e);
        }
    }

    private JsonObject createRequestBody(String sender, String message, Client client,
            OtherPlayerTracker playerTracker) {
        JsonObject requestBody = new JsonObject();
        try {
            requestBody.addProperty("userId", sender);
            requestBody.addProperty("userName", sender);

            JsonObject context = createContextObject(sender, playerTracker);
            requestBody.add("context", context);

            Player player = playerTracker.findPlayerByName(sender);
            if (player != null) {
                try {
                    String fullMessage = String.format("%s", message, sender);
                    requestBody.addProperty("text", fullMessage);
                } catch (Exception e) {
                    log.error("Error creating player-specific data", e);
                    requestBody.addProperty("text", message);
                }
            } else {
                requestBody.addProperty("text", message);
            }

            log.info("API request body: {}", requestBody);
        } catch (Exception e) {
            log.error("Error creating request body", e);
            requestBody.addProperty("text", message);
        }
        return requestBody;
    }

    private JsonObject createContextObject(String sender, OtherPlayerTracker playerTracker) {
        JsonObject context = new JsonObject();
        try {
            JsonArray recentPlayers = new JsonArray();

            if (playerTracker != null) {
                for (String player : playerTracker.getRecentInteractors()) {
                    JsonObject playerObj = new JsonObject();
                    playerObj.addProperty("name", player);
                    playerObj.addProperty("interactions",
                            playerTracker.getPlayerInteractions().getOrDefault(player, 0));
                    recentPlayers.add(playerObj);
                }

                context.add("recentPlayers", recentPlayers);
                context.addProperty("totalPlayers", playerTracker.getActivePlayerCount());
                context.addProperty("currentSpeaker", sender);
                context.addProperty("isBusyChat",
                        playerTracker.getActivePlayerCount() > ElizaConstants.QUIET_CHAT_THRESHOLD);
            } else {
                context.add("recentPlayers", recentPlayers);
                context.addProperty("totalPlayers", 0);
                context.addProperty("currentSpeaker", sender);
                context.addProperty("isBusyChat", false);
            }
        } catch (Exception e) {
            log.error("Error creating context object", e);
        }
        return context;
    }

    private void handleAPIResponse(Response response, OtherPlayerTracker playerTracker,
            Consumer<List<String>> responseHandler) {
        if (response == null) {
            log.error("Null response received");
            return;
        }

        try {
            if (!response.isSuccessful()) {
                log.error("API request unsuccessful: {}", response.code());
                return;
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                log.error("Null response body");
                return;
            }

            String jsonResponse = responseBody.string();
            log.info("API response: {}", jsonResponse);

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                log.error("Empty response from API");
                return;
            }

            try {
                JsonElement jsonElement = new JsonParser().parse(jsonResponse);
                if (!jsonElement.isJsonArray()) {
                    log.error("Expected JSON array response");
                    return;
                }

                JsonArray responseArray = jsonElement.getAsJsonArray();
                if (responseArray.size() == 0) {
                    log.error("Empty response array from API");
                    return;
                }

                JsonObject messageObj = responseArray.get(0).getAsJsonObject();
                if (!messageObj.has("text")) {
                    log.error("Response missing text field");
                    return;
                }

                String trollResponse = messageObj.get("text").getAsString();
                List<String> processedMessages = processResponse(trollResponse, playerTracker);

                if (processedMessages != null && !processedMessages.isEmpty()) {
                    responseHandler.accept(processedMessages);
                }
            } catch (JsonSyntaxException e) {
                log.error("Error parsing JSON response", e);
            }
        } catch (Exception e) {
            log.error("Error processing API response", e);
        } finally {
            response.close();
        }
    }

    private List<String> processResponse(String response, OtherPlayerTracker playerTracker) {
        if (response == null)
            return Collections.emptyList();

        try {
            if (playerTracker != null) {
                for (String playerName : playerTracker.getActivePlayerNames()) {
                    String atMention = "@" + playerName;
                    if (response.contains(atMention)) {
                        response = response.replace(atMention, playerName + ":");
                    }
                }
            }

            List<String> messageParts = splitMessage(response);
            return limitMessageParts(messageParts,
                    playerTracker != null ? playerTracker.getActivePlayerCount() : 0);
        } catch (Exception e) {
            log.error("Error processing response", e);
            return Collections.singletonList(response);
        }
    }

    private List<String> splitMessage(String message) {
        List<String> parts = new ArrayList<>();
        if (message == null || message.isEmpty()) {
            return parts;
        }

        try {
            String[] sentences = message.split("(?<=[.!?])\\s+");
            StringBuilder currentPart = new StringBuilder();

            for (String sentence : sentences) {
                if (currentPart.length() > 0 &&
                        currentPart.length() + sentence.length() + 1 > ElizaConstants.MAX_MESSAGE_LENGTH) {
                    parts.add(currentPart.toString().trim());
                    currentPart.setLength(0);
                }

                if (sentence.length() > ElizaConstants.MAX_MESSAGE_LENGTH) {
                    if (currentPart.length() > 0) {
                        parts.add(currentPart.toString().trim());
                        currentPart.setLength(0);
                    }

                    String[] words = sentence.split("\\s+");
                    for (String word : words) {
                        if (currentPart.length() + word.length() + 1 > ElizaConstants.MAX_MESSAGE_LENGTH) {
                            if (currentPart.length() > ElizaConstants.MIN_MESSAGE_LENGTH ||
                                    currentPart.length() + word.length() > ElizaConstants.MAX_MESSAGE_LENGTH) {
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
        } catch (Exception e) {
            log.error("Error splitting message", e);
            parts.add(message);
        }

        return parts;
    }

    private List<String> limitMessageParts(List<String> messageParts, int activePlayerCount) {
        if (messageParts == null || messageParts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            if (activePlayerCount > ElizaConstants.QUIET_CHAT_THRESHOLD) {
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
        } catch (Exception e) {
            log.error("Error limiting message parts", e);
            return Collections.singletonList(messageParts.get(0));
        }
    }
}
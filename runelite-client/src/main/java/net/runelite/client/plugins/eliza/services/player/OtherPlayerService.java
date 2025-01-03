package net.runelite.client.plugins.eliza.services.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Singleton;

import static net.runelite.client.plugins.eliza.utils.ElizaConstants.*;

import java.util.*;

@Slf4j
@Singleton
public class OtherPlayerService {
    private final Set<String> activePlayerNames = new HashSet<>();
    private final Queue<Long> recentMessageTimes = new LinkedList<>();
    private final Map<String, Integer> playerInteractions = new HashMap<>();
    private final Queue<String> recentInteractors = new LinkedList<>();

    @Inject
    private Client client;

    @Inject
    private LocalPlayerService localPlayerService;

    public void clear() {
        playerInteractions.clear();
        recentInteractors.clear();
    }

    public void reset() {
        activePlayerNames.clear();
        recentMessageTimes.clear();
        playerInteractions.clear();
        recentInteractors.clear();
    }

    public void updateActivePlayers() {
        activePlayerNames.clear();
        String localPlayerName = localPlayerService.getLocalPlayerName();

        for (Player player : client.getPlayers()) {
            if (player != null && player.getName() != null &&
                    !player.getName().equals(localPlayerName) &&
                    !player.getName().toLowerCase().contains(TROLL_NAME)) {
                activePlayerNames.add(player.getName());
            }
        }
    }

    public void addNearbyPlayersToJson(JsonObject location, WorldPoint point) {
        JsonArray nearbyPlayers = new JsonArray();
        String localPlayerName = localPlayerService.getLocalPlayerName();

        for (Player p : client.getPlayers()) {
            if (p != null && !p.getName().equals(localPlayerName)) {
                WorldPoint otherPoint = p.getWorldLocation();
                if (otherPoint != null) {
                    int distance = point.distanceTo(otherPoint);
                    if (distance <= 15) { // Only include players within 15 tiles
                        JsonObject playerInfo = new JsonObject();
                        playerInfo.addProperty("name", p.getName());
                        playerInfo.addProperty("distance", distance);
                        nearbyPlayers.add(playerInfo);
                    }
                }
            }
        }
        location.add("nearbyPlayers", nearbyPlayers);
    }

    public String getNearbyPlayersDescription(WorldPoint point) {
        StringBuilder nearbyDesc = new StringBuilder();
        int nearbyCount = 0;
        String localPlayerName = localPlayerService.getLocalPlayerName();

        for (Player p : client.getPlayers()) {
            if (p != null && !p.getName().equals(localPlayerName)) {
                WorldPoint otherPoint = p.getWorldLocation();
                if (otherPoint != null) {
                    int distance = point.distanceTo(otherPoint);
                    if (distance <= 3) { // Only mention very close players
                        if (nearbyCount++ > 0) {
                            nearbyDesc.append(", ");
                        }
                        nearbyDesc.append(p.getName());
                    }
                }
            }
        }

        return nearbyDesc.toString();
    }

    public void updatePlayerInteraction(String playerName) {
        if (playerName == null || playerName.equals(localPlayerService.getLocalPlayerName()))
            return;

        playerInteractions.merge(playerName, 1, Integer::sum);

        if (!recentInteractors.contains(playerName)) {
            recentInteractors.offer(playerName);
            while (recentInteractors.size() > MAX_TRACKED_PLAYERS) {
                String removed = recentInteractors.poll();
                playerInteractions.remove(removed);
            }
        }
    }

    public double calculateResponseChance() {
        int playerCount = activePlayerNames.size();
        int recentMessages = recentMessageTimes.size();

        double chance = BASE_RESPONSE_CHANCE;

        if (playerCount > QUIET_CHAT_THRESHOLD) {
            chance *= (double) QUIET_CHAT_THRESHOLD / playerCount;
        }

        if (recentMessages > BUSY_CHAT_THRESHOLD) {
            chance *= (double) BUSY_CHAT_THRESHOLD / recentMessages;
        }

        if (!recentInteractors.isEmpty()) {
            chance *= 1.5; // Boost chance if there have been recent interactions
        }

        if (recentMessageTimes.size() > BUSY_CHAT_THRESHOLD) {
            chance *= 0.5;
        }

        chance *= 0.8 + (Math.random() * 0.4); // Varies from 80% to 120% of base chance

        return Math.max(0.1, Math.min(0.9, chance)); // Keep between 10% and 90%
    }

    public Set<String> getActivePlayerNames() {
        return Collections.unmodifiableSet(activePlayerNames);
    }

    public int getActivePlayerCount() {
        return activePlayerNames.size();
    }

    public int getRecentInteractorCount() {
        return recentInteractors.size();
    }

    public Map<String, Integer> getPlayerInteractions() {
        return Collections.unmodifiableMap(playerInteractions);
    }

    public Queue<String> getRecentInteractors() {
        return new LinkedList<>(recentInteractors);
    }

    public void updateRecentActivity() {
        long currentTime = System.currentTimeMillis();

        while (!recentMessageTimes.isEmpty() &&
                recentMessageTimes.peek() < currentTime - ACTIVITY_WINDOW) {
            recentMessageTimes.poll();
        }

        recentMessageTimes.offer(currentTime);
        while (recentMessageTimes.size() > MAX_CACHED_MESSAGES) {
            recentMessageTimes.poll();
        }
    }

    public Player findPlayerByName(String name) {
        if (name == null || name.equals(localPlayerService.getLocalPlayerName()))
            return null;

        for (Player player : client.getPlayers()) {
            if (player != null && player.getName() != null &&
                    player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
}
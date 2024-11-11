package net.runelite.client.plugins.bridgetroll;

import net.runelite.api.ChatPlayer;
import net.runelite.api.Client;
import net.runelite.api.Player;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

import static net.runelite.client.plugins.bridgetroll.BridgeTrollConstants.*;

@Singleton
public class PlayerTracker {
    private final Set<String> activePlayerNames = new HashSet<>();
    private final Queue<Long> recentMessageTimes = new LinkedList<>();
    private final Map<String, Integer> playerInteractions = new HashMap<>();
    private final Queue<String> recentInteractors = new LinkedList<>();

    @Inject
    private Client client;

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
        
        if (client.getFriendsChatManager() != null) {
            for (ChatPlayer player : client.getFriendsChatManager().getMembers()) {
                if (player != null && player.getName() != null && 
                    !player.getName().toLowerCase().contains(TROLL_NAME)) {
                    activePlayerNames.add(player.getName());
                }
            }
        }
        
        if (client.getLocalPlayer() != null) {
            client.getPlayers().forEach(player -> {
                if (player != null && player.getName() != null && 
                    !player.getName().toLowerCase().contains(TROLL_NAME)) {
                    activePlayerNames.add(player.getName());
                }
            });
        }
    }

    public void updatePlayerInteraction(String playerName) {
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
        
        return Math.max(0.1, chance);
    }

    public Set<String> getActivePlayerNames() {
        return activePlayerNames;
    }

    public int getActivePlayerCount() {
        return activePlayerNames.size();
    }

    public int getRecentInteractorCount() {
        return recentInteractors.size();
    }

    public Map<String, Integer> getPlayerInteractions() {
        return playerInteractions;
    }

    public Queue<String> getRecentInteractors() {
        return recentInteractors;
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
        if (client.getLocalPlayer() != null && 
            client.getLocalPlayer().getName() != null && 
            client.getLocalPlayer().getName().equalsIgnoreCase(name)) {
            return client.getLocalPlayer();
        }
        
        return client.getWorldView(-1).players().stream()
            .filter(p -> p != null && p.getName() != null && p.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }
}
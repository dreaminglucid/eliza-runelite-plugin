package net.runelite.client.plugins.eliza.services;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class LocalPlayerService {
    @Inject
    private Client client;

    public Player getLocalPlayer() {
        return client.getLocalPlayer();
    }

    public String getLocalPlayerName() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer != null && localPlayer.getName() != null) {
            return localPlayer.getName();
        }
        return null;
    }

    public WorldPoint getLocalPlayerLocation() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer != null) {
            return localPlayer.getWorldLocation();
        }
        return null;
    }

    public boolean isLocalPlayer(String playerName) {
        if (playerName == null)
            return false;
        String localName = getLocalPlayerName();
        return localName != null && localName.equalsIgnoreCase(playerName);
    }
}
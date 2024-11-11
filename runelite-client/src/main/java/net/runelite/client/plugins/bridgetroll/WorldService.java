package net.runelite.client.plugins.bridgetroll;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Player;
import net.runelite.api.World;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuAction;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class WorldService {
    @Inject
    private Client client;

    public void walkTo(WorldPoint destination) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        LocalPoint localLocation = localPlayer.getLocalLocation();
        client.menuAction(
            localLocation.getX(),
            localLocation.getY(),
            MenuAction.WALK,
            0,
            0,
            "Walk here",
            ""
        );
    }

    public JsonObject getWorldLocation(Player player) {
        JsonObject location = new JsonObject();

        if (player != null) {
            WorldPoint point = player.getWorldLocation();
            location.addProperty("x", point.getX());
            location.addProperty("y", point.getY());
            location.addProperty("plane", point.getPlane());

            LocalPoint destination = client.getLocalDestinationLocation();
            if (destination != null) {
                location.addProperty("moving", true);
                location.addProperty("destinationX", destination.getX());
                location.addProperty("destinationY", destination.getY());
            }

            JsonArray nearbyPlayers = new JsonArray();
            for (Player p : client.getPlayers()) {
                if (p == null || p == client.getLocalPlayer()) {
                    continue;
                }
                JsonObject playerInfo = new JsonObject();
                playerInfo.addProperty("name", p.getName());
                playerInfo.addProperty("distance", point.distanceTo(p.getWorldLocation()));
                nearbyPlayers.add(playerInfo);
            }
            location.add("nearbyPlayers", nearbyPlayers);
        }

        return location;
    }

    public String getLocationDescription(Player player) {
        if (player == null) {
            return "unknown location";
        }

        WorldPoint point = player.getWorldLocation();
        LocalPoint destination = client.getLocalDestinationLocation();

        if (destination != null) {
            return String.format("at (%d, %d) moving", point.getX(), point.getY());
        } else {
            return String.format("at (%d, %d)", point.getX(), point.getY());
        }
    }

    public boolean isMoving() {
        return client.getLocalDestinationLocation() != null;
    }

    public WorldPoint getCurrentLocation() {
        Player player = client.getLocalPlayer();
        return player != null ? player.getWorldLocation() : null;
    }
}